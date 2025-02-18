package ch.admin.bit.jeap.processcontext.adapter.objectstorage;

import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessSnapshot;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessSnapshotArchiveData;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessSnapshotMetadata;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessSnapshotRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.SerializedProcessSnapshotArchiveData;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.io.*;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;

import static java.lang.Integer.parseInt;
import static java.util.Comparator.naturalOrder;

@Slf4j
@RequiredArgsConstructor
public class S3ProcessSnapshotRepository implements ProcessSnapshotRepository {

    private static final String METADATA_SYSTEM_NAME = "pcs-snapshot-system-name";
    private static final String METADATA_SCHEMA_NAME = "pcs-snapshot-schema-name";
    private static final String METADATA_SCHEMA_VERSION = "pcs-snapshot-schema-version";
    private static final String METADATA_SNAPSHOT_VERSION = "pcs-snapshot-version";
    private static final String METADATA_SNAPSHOT_RETENTION_MONTHS = "pcs-snapshot-retention-months";
    private static final String CONTENT_TYPE = "avro/binary";
    private static final String OBJECT_TYPE_TAG_NAME = "pcs-object-type";
    private static final String SNAPSHOT_OBJECT_TYPE = "snapshot";
    private static final Tag SNAPSHOT_TAG = Tag.builder().key(OBJECT_TYPE_TAG_NAME).value(SNAPSHOT_OBJECT_TYPE).build();

    private final S3ObjectStorageRepository s3ObjectStorageRepository;
    private final S3ObjectStorageProperties s3ObjectStorageProperties;

    @PostConstruct
    public void init() {
        log.info("Created S3 snapshot repository with object storage configuration properties: {}", s3ObjectStorageProperties);

        if (s3ObjectStorageProperties.snapshotRetentionDays < 1) {
            log.error("Snapshot retention days must be greater than 0");
            throw new IllegalArgumentException("Snapshot retention days must be greater than 0. Please set the value of 'jeap.processcontext.objectstorage.snapshot-retention-days'");
        }

        s3ObjectStorageRepository.checkAccessToBucket(s3ObjectStorageProperties.getSnapshotBucket());
        applySnapshotRetentionConfiguration();
    }

    @Override
    public void storeSnapshot(ProcessSnapshotArchiveData processSnapshotArchiveData) {
        byte[] serializedProcessSnapshot = serialize(processSnapshotArchiveData);
        Map<String, String> s3Metadata = createS3MetaData(processSnapshotArchiveData);
        String key = createKey(processSnapshotArchiveData);
        String bucket = s3ObjectStorageProperties.getSnapshotBucket();
        s3ObjectStorageRepository.putObject(bucket, key, serializedProcessSnapshot, CONTENT_TYPE, s3Metadata, Set.of(SNAPSHOT_TAG));
    }

    @Override
    public Optional<SerializedProcessSnapshotArchiveData> loadSnapshot(String originProcessId, Integer version) {
        try {
            if (version == null) {
                Optional<Integer> latestProcessSnapshotVersion = findNewestSnapshotVersion(originProcessId);
                if (latestProcessSnapshotVersion.isPresent()) {
                    version = latestProcessSnapshotVersion.get();
                } else {
                    return Optional.empty();
                }
            }

            String key = createKey(originProcessId, version);
            ResponseBytes<GetObjectResponse> responseBytes = s3ObjectStorageRepository.getObjectAsBytes(s3ObjectStorageProperties.getSnapshotBucket(), key);
            ProcessSnapshotMetadata processSnapshotMetadata = createProcessSnapshotMetadata(responseBytes.response(), originProcessId, version);
            byte[] serializedSnapshot = responseBytes.asByteArray();
            return Optional.of(new SerializedProcessSnapshotArchiveData(serializedSnapshot, processSnapshotMetadata));
        } catch (S3ObjectStorageRepositoryObjectNotFoundException nfe) {
            log.warn("Snapshot for origin process id '{}' and version {} not found.", originProcessId, version);
            return Optional.empty();
        } catch (Exception e) {
            log.error("An unexpected exception happened loading the snapshot for origin process id '{}' and version {}.", originProcessId, version, e);
            throw e;
        }
    }

    private Optional<Integer> findNewestSnapshotVersion(String processOriginId) {
        List<String> versions = s3ObjectStorageRepository.listObjects(s3ObjectStorageProperties.getSnapshotBucket(), createPrefix(processOriginId));
        return versions.stream()
                .map(Integer::parseInt)
                .max(naturalOrder());
    }

    @Override
    public Optional<ProcessSnapshot> loadAndDeserializeNewestSnapshot(String processOriginId) {
        return findNewestSnapshotVersion(processOriginId)
                .flatMap(snapshotVersion -> loadSnapshot(processOriginId, snapshotVersion))
                .map(S3ProcessSnapshotRepository::deserializeSnapshot);
    }

    static ProcessSnapshot deserializeSnapshot(SerializedProcessSnapshotArchiveData serializedProcessSnapshotArchiveData) {
        byte[] serializedSnapshot = serializedProcessSnapshotArchiveData.getSerializedProcessSnapshot();
        int snapshotSchemaVersion = serializedProcessSnapshotArchiveData.getMetadata().getSchemaVersion();
        return deserializeSnapshot(serializedSnapshot, snapshotSchemaVersion);
    }

    @SneakyThrows
    static ProcessSnapshot deserializeSnapshot(byte[] serializedSnapshot, int snapshotSchemaVersion) {
        // There are currently only two versions of the ProcessSnapshot schema with version2 being fully compatible
        // to version 1. Therefore, version 1 data can be deserialized into a version 2 snapshot instance, too.
        Schema writerSchema = getProcessSnapshotSchema(snapshotSchemaVersion);
        Schema readerSchema = ProcessSnapshot.getClassSchema(); // current schema used by the PCS
        DatumReader<ProcessSnapshot> datumReader = new SpecificDatumReader<>(writerSchema, readerSchema);
        Decoder decoder = DecoderFactory.get().binaryDecoder(new ByteArrayInputStream(serializedSnapshot), null);
        return datumReader.read(null, decoder);
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    private static Schema getProcessSnapshotSchema(int version) {
        return switch (version) {
            case 1 -> ch.admin.bit.jeap.processcontext.archive.processsnapshot.v1.ProcessSnapshot.getClassSchema();
            default -> ProcessSnapshot.getClassSchema();
        };
    }

    void applySnapshotRetentionConfiguration() {
        final LifecycleRule snapshotLifecycleRule = createSnapshotLifecycleRule();
        final String bucket = s3ObjectStorageProperties.getSnapshotBucket();
        Optional<LifecycleRule> existingSnapshotLifecycleRule = fetchBucketLifecycleRule(snapshotLifecycleRule.id(), bucket);
        if (existingSnapshotLifecycleRule.isPresent()) {
            log.info("Leaving snapshot retention configuration unchanged for bucket '{}' and life cycle rule {}.",
                    bucket, existingSnapshotLifecycleRule.get());
        } else {
            // There is only one lifecycle rule on the bucket: the snapshot lifecycle rule -> no need to merge lifecycle
            // rules here, i.e. we simply replace the bucket lifecycle configuration with a new one.
            var bucketLifecycleConfiguration = BucketLifecycleConfiguration.builder()
                    .rules(snapshotLifecycleRule)
                    .build();
            log.info("Applying snapshot retention configuration to bucket '{}' by life cycle rule {}.",
                    bucket, snapshotLifecycleRule);
            s3ObjectStorageRepository.setBucketLifecycleConfiguration(bucket, bucketLifecycleConfiguration);
        }
    }

    private Optional<LifecycleRule> fetchBucketLifecycleRule(String ruleId, String bucket) {
        return s3ObjectStorageRepository.getBucketLifecycleRules(bucket).stream()
                .filter(rule -> ruleId.equals(rule.id()))
                .findAny();
    }

    private LifecycleRule createSnapshotLifecycleRule() {
        return LifecycleRule.builder()
                .id(getSnapshotLifecycleRuleId())
                // only apply to objects tagged as snapshots
                .filter(filterBuilder -> filterBuilder.tag(SNAPSHOT_TAG))
                // expire the object after the snapshot retention duration expired
                .expiration(expirationBuilder ->
                        expirationBuilder.days(s3ObjectStorageProperties.getSnapshotRetentionDays()))
                .status(ExpirationStatus.ENABLED)
                .build();
    }

    private String getSnapshotLifecycleRuleId() {
        // A different snapshot retention configuration results in a different lifecycle rule id
        return "snapshot_retention_days_" + s3ObjectStorageProperties.getSnapshotRetentionDays();
    }

    private ProcessSnapshotMetadata createProcessSnapshotMetadata(GetObjectResponse getObjectResponse, String originProcessId, int snapshotVersion) {
        try {
            Map<String, String> s3ObjectMetadata = getObjectResponse.metadata();
            return ProcessSnapshotMetadata.builder().
                    schemaName(requireNotNull(s3ObjectMetadata.get(METADATA_SCHEMA_NAME), METADATA_SCHEMA_NAME)).
                    schemaVersion(getSchemaVersion(s3ObjectMetadata)).
                    systemName(requireNotNull(s3ObjectMetadata.get(METADATA_SYSTEM_NAME), METADATA_SYSTEM_NAME)).
                    snapshotVersion(parseInt(requireNotNull(s3ObjectMetadata.get(METADATA_SNAPSHOT_VERSION), METADATA_SNAPSHOT_VERSION))).
                    retentionPeriodMonths(parseInt(requireNotNull(s3ObjectMetadata.get(METADATA_SNAPSHOT_RETENTION_MONTHS), METADATA_SNAPSHOT_RETENTION_MONTHS))).
                    build();
        } catch (Exception e) {
            throw S3ProcessSnapshotRepositoryException.s3MetadataReadException(originProcessId, snapshotVersion, e);
        }
    }

    private int getSchemaVersion(Map<String, String> s3ObjectMetadata) {
        String versionStr = requireNotNull(s3ObjectMetadata.get(METADATA_SCHEMA_VERSION), METADATA_SCHEMA_VERSION);
        if (versionStr.equals("1.0.0")) {
            // for backward compatibility to previous PCS version that used 1.0.0 as schema version instead of 1
            return 1;
        }
        return Integer.parseInt(versionStr);
    }

    private static <T> T requireNotNull(T object, String attribute) {
        return Objects.requireNonNull(object, attribute + " must not be null");
    }

    private static byte[] serialize(ProcessSnapshotArchiveData processSnapshotArchiveData) {
        ProcessSnapshot processSnapshot = processSnapshotArchiveData.getProcessSnapshot();
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Encoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
            DatumWriter<SpecificRecord> datumWriter = new SpecificDatumWriter<>(processSnapshot.getSchema());
            datumWriter.write(processSnapshot, encoder);
            encoder.flush();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw S3ProcessSnapshotRepositoryException.serializationException(processSnapshotArchiveData, e);
        }
    }

    private Map<String, String> createS3MetaData(ProcessSnapshotArchiveData processSnapshotArchiveData) {
        ProcessSnapshotMetadata processSnapshotMetadata = processSnapshotArchiveData.getMetadata();
        return Map.of(
                METADATA_SYSTEM_NAME, processSnapshotMetadata.getSystemName(),
                METADATA_SCHEMA_NAME, processSnapshotMetadata.getSchemaName(),
                METADATA_SCHEMA_VERSION, Integer.toString(processSnapshotMetadata.getSchemaVersion()),
                METADATA_SNAPSHOT_VERSION, Integer.toString(processSnapshotMetadata.getSnapshotVersion()),
                METADATA_SNAPSHOT_RETENTION_MONTHS, Integer.toString(processSnapshotMetadata.getRetentionPeriodMonths())
        );
    }

    private static String createKey(ProcessSnapshotArchiveData processSnapshotArchiveData) {
        return createKey(processSnapshotArchiveData.getProcessSnapshot().getOriginProcessId(),
                processSnapshotArchiveData.getMetadata().getSnapshotVersion());
    }

    private static String createKey(String originProcessId, int snapshotVersion) {
        return createPrefix(originProcessId) + snapshotVersion;
    }

    private static String createPrefix(String originProcessId) {
        return originProcessId + "/";
    }

}
