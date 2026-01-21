package ch.admin.bit.jeap.processcontext.adapter.objectstorage;

import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.*;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessSnapshotArchiveData;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessSnapshotMetadata;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.utils.Md5Utils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static ch.admin.bit.jeap.processcontext.adapter.objectstorage.S3ProcessSnapshotRepository.deserializeSnapshot;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@Testcontainers
class S3ProcessSnapshotRepositoryIT {

    private static final String MINIO_IMAGE = "minio/minio:RELEASE.2025-09-07T16-13-09Z";
    private static final String TEST_BUCKET_NAME = "test-bucket";
    private static final int SNAPSHOT_RETENTION_DAYS = 2;

    private TimedS3Client s3Client;
    private S3ProcessSnapshotRepository s3ProcessSnapshotRepository;
    private S3ObjectStorageRepository s3ObjectStorageRepository;
    private S3ObjectStorageProperties s3ObjectStorageProperties;

    @Container
    private final MinIOContainer minioContainer = new MinIOContainer(MINIO_IMAGE);


    @BeforeEach
    void setUp() {
        s3ObjectStorageProperties = createS3ObjectStorageProperties();
        s3Client = new TimedS3Client(s3ObjectStorageProperties.connection, null);
        setupStorage();
        checkStorage();
        s3ObjectStorageRepository = new S3ObjectStorageRepository(s3Client);
        s3ProcessSnapshotRepository = new S3ProcessSnapshotRepository(s3ObjectStorageRepository, s3ObjectStorageProperties);
    }

    @Test
    void testDeserializeSnapshot() {
        final var processSnapshotV1 = createProcessSnapshotV1("v1", "IN_PROGRESS");
        byte[] serializedProcessSnapshotV1 = serializeProcessSnapshot(processSnapshotV1);
        final var processSnapshotV2 = createProcessSnapshot("v2", "IN_PROGRESS");
        byte[] serializedProcessSnapshotV2 = serializeProcessSnapshot(processSnapshotV2);

        // assert that a v2 snapshot can be serialized and deserialized back to an equal v2 snapshot instance
        ProcessSnapshot deserializedProcessSnapshotV2FromV2 = S3ProcessSnapshotRepository.deserializeSnapshot(serializedProcessSnapshotV2, 2);
        assertThat(deserializedProcessSnapshotV2FromV2).isEqualTo(processSnapshotV2);

        // assert tha a v1 snapshot can be serialized and deserialized to a v2 snapshot instance
        ProcessSnapshot deserializedProcessSnapshotV2FromV1 = S3ProcessSnapshotRepository.deserializeSnapshot(serializedProcessSnapshotV1, 1);
        assertThat(deserializedProcessSnapshotV2FromV1).isNotNull();
    }

    @Test
    void testStoreAndLoad() {
        final String originProcessId = "test-origin-process-id";
        final int snapshotVersion = 1;
        final int retentionPeriodMonths = 60;
        final var processSnapshotArchiveData = createProcessSnapshotArchiveData(originProcessId, snapshotVersion, retentionPeriodMonths);
        final var processSnapshot = processSnapshotArchiveData.getProcessSnapshot();

        // store and load
        s3ProcessSnapshotRepository.storeSnapshot(processSnapshotArchiveData);
        var serializedProcessSnapshotArchiveDataOptional = s3ProcessSnapshotRepository.loadSnapshot(originProcessId, snapshotVersion);

        assertThat(serializedProcessSnapshotArchiveDataOptional).isPresent();
        var serializedProcessSnapshotArchiveData = serializedProcessSnapshotArchiveDataOptional.get();
        ProcessSnapshotMetadata processSnapshotMetadata = serializedProcessSnapshotArchiveData.getMetadata();
        assertThat(processSnapshotMetadata.getSnapshotVersion()).isEqualTo(1);
        assertThat(processSnapshotMetadata.getRetentionPeriodMonths()).isEqualTo(retentionPeriodMonths);
        assertThat(processSnapshotMetadata.getSystemName()).isEqualTo("JEAP");
        assertThat(processSnapshotMetadata.getSchemaName()).isEqualTo("ProcessSnapshot");
        assertThat(processSnapshotMetadata.getSchemaVersion()).isEqualTo(2);
        assertSnapshotTag(originProcessId, snapshotVersion);

        assertThat(serializedProcessSnapshotArchiveData.getSerializedProcessSnapshot()).isNotEmpty();
        ProcessSnapshot deserializedProcessSnapshot = deserializeSnapshot(serializedProcessSnapshotArchiveData);
        assertThat(deserializedProcessSnapshot).isEqualTo(processSnapshot);

        // test load the newest version when version=null
        var newestSnapshot = s3ProcessSnapshotRepository.loadSnapshot(originProcessId, null);
        assertThat(newestSnapshot)
                .isPresent();
        assertThat(newestSnapshot.get().getMetadata().getSnapshotVersion())
                .isEqualTo(1);
    }

    @Test
    void testLoad_WhenOriginProcessIdNotExists_ThenNotFound() {
        assertThat(s3ProcessSnapshotRepository.loadSnapshot("origin-process-id-does-not-exist", 1))
                .isEmpty();
    }

    @Test
    void testLoad_WhenSnapshotVersionNotExists_ThenNotFound() {
        final String originProcessId = "test-origin-process-id";
        s3ProcessSnapshotRepository.storeSnapshot(createProcessSnapshotArchiveData(originProcessId, 1, 60));
        // originProcessId exists and version exists -> found
        assertThat(s3ProcessSnapshotRepository.loadSnapshot(originProcessId, 1)).isPresent();

        // originProcessId exists but version does not -> not found
        assertThat(s3ProcessSnapshotRepository.loadSnapshot(originProcessId, 42)).
                isEmpty();
    }

    @Test
    void testApplySnapshotRetentionConfiguration_WhenNoExistingConfiguration_ThenSnapshotRetentionConfigurationCreated() {
        assertThat(s3ObjectStorageRepository.getBucketLifecycleRules(TEST_BUCKET_NAME)).isEmpty();

        s3ProcessSnapshotRepository.applySnapshotRetentionConfiguration();

        List<LifecycleRule> lifeCycleRules = s3ObjectStorageRepository.getBucketLifecycleRules(TEST_BUCKET_NAME);
        assertThat(lifeCycleRules).hasSize(1);
        assertThat(lifeCycleRules.get(0).id()).isEqualTo("snapshot_retention_days_" + SNAPSHOT_RETENTION_DAYS);
    }

    @Test
    void testApplySnapshotRetentionConfiguration_WhenSameConfigurationExisting_ThenUnchanged() {
        // initialize the bucket with a snapshot retention of SNAPSHOT_RETENTION_DAYS
        s3ProcessSnapshotRepository.applySnapshotRetentionConfiguration();
        List<LifecycleRule> initialLifeCycleRules = s3ObjectStorageRepository.getBucketLifecycleRules(TEST_BUCKET_NAME);
        assertThat(initialLifeCycleRules).hasSize(1);
        assertThat(initialLifeCycleRules.get(0).id()).isEqualTo("snapshot_retention_days_" + SNAPSHOT_RETENTION_DAYS);

        // apply the same snapshot retention configuration again
        s3ProcessSnapshotRepository.applySnapshotRetentionConfiguration();

        List<LifecycleRule> lifeCycleRules = s3ObjectStorageRepository.getBucketLifecycleRules(TEST_BUCKET_NAME);
        assertThat(lifeCycleRules).hasSize(1);
        assertThat(lifeCycleRules.get(0).id()).isEqualTo("snapshot_retention_days_" + SNAPSHOT_RETENTION_DAYS);
    }

    @Test
    void testApplySnapshotRetentionConfiguration_WhenSnapshotRetentionDaysChanged_ThenSnapshotRetentionConfigurationChanged() {
        // initialize the bucket with a snapshot retention of SNAPSHOT_RETENTION_DAYS
        s3ProcessSnapshotRepository.applySnapshotRetentionConfiguration();
        List<LifecycleRule> initialLifeCycleRules = s3ObjectStorageRepository.getBucketLifecycleRules(TEST_BUCKET_NAME);
        assertThat(initialLifeCycleRules).hasSize(1);
        assertThat(initialLifeCycleRules.get(0).id()).isEqualTo("snapshot_retention_days_" + SNAPSHOT_RETENTION_DAYS);

        // reconfigure the snapshot retention to newSnapshotRetentionDays and apply the new configuration
        final int newSnapshotRetentionDays = 5;
        assertThat(SNAPSHOT_RETENTION_DAYS).isNotEqualTo(newSnapshotRetentionDays);
        s3ObjectStorageProperties.setSnapshotRetentionDays(newSnapshotRetentionDays);
        s3ProcessSnapshotRepository.applySnapshotRetentionConfiguration();

        List<LifecycleRule> lifeCycleRules = s3ObjectStorageRepository.getBucketLifecycleRules(TEST_BUCKET_NAME);
        assertThat(lifeCycleRules).hasSize(1);
        assertThat(lifeCycleRules.get(0).id()).isEqualTo("snapshot_retention_days_" + newSnapshotRetentionDays);
    }

    @Test
    void testLoadAndDeserializeNewestSnapshot() {
        final String originProcessId = "test-origin-process-id";
        final int retentionPeriodMonths = 60;
        final var processSnapshotArchiveData1 = createProcessSnapshotArchiveData(originProcessId, 1, retentionPeriodMonths, "IN_PROGRESS");
        final var processSnapshotArchiveData2 = createProcessSnapshotArchiveData(originProcessId, 2, retentionPeriodMonths, "IN_PROGRESS");
        final var processSnapshotArchiveData3 = createProcessSnapshotArchiveData(originProcessId, 3, retentionPeriodMonths, "COMPLETED");
        s3ProcessSnapshotRepository.storeSnapshot(processSnapshotArchiveData1);
        s3ProcessSnapshotRepository.storeSnapshot(processSnapshotArchiveData3);
        s3ProcessSnapshotRepository.storeSnapshot(processSnapshotArchiveData2);

        // store and load
        var snapshot = s3ProcessSnapshotRepository.loadAndDeserializeNewestSnapshot(originProcessId);

        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().getState())
                .describedAs("Version 3 is the newest version, and the only version in state COMPLETED")
                .isEqualTo("COMPLETED");
    }

    @SuppressWarnings("SameParameterValue")
    private static ProcessSnapshotArchiveData createProcessSnapshotArchiveData(String originProcessId, int snapshotVersion, int retentionPeriodMonths) {
        return createProcessSnapshotArchiveData(originProcessId, snapshotVersion, retentionPeriodMonths, "COMPLETED");
    }

    @SuppressWarnings("SameParameterValue")
    private static ProcessSnapshotArchiveData createProcessSnapshotArchiveData(String originProcessId, int snapshotVersion, int retentionPeriodMonths, String state) {
        ProcessSnapshot processSnapshot = createProcessSnapshot(originProcessId, state);
        return ProcessSnapshotArchiveData.from(processSnapshot, snapshotVersion, retentionPeriodMonths);
    }

    @SuppressWarnings("SameParameterValue")
    private static ch.admin.bit.jeap.processcontext.archive.processsnapshot.v1.ProcessSnapshot createProcessSnapshotV1(
            String originProcessId, String state) {
        Instant created = ZonedDateTime.now().truncatedTo(ChronoUnit.MILLIS).toInstant();
        Instant planned = created.plusSeconds(5);
        Instant modified = planned.plusSeconds(5);
        Instant completed = modified.plusSeconds(5);
        Instant snapshotCreated = completed.plusSeconds(5);
        return ch.admin.bit.jeap.processcontext.archive.processsnapshot.v1.ProcessSnapshot.newBuilder().
                setOriginProcessId(originProcessId).
                setTemplateName("test-template-name").
                setTemplateLabel("test-template-description").
                setState(state).
                setDateTimeCreated(created).
                setDateTimeModified(modified).
                setDateTimeCompleted(completed).
                setSnapshotDateTimeCreated(snapshotCreated).
                setProcessData(List.of(ch.admin.bit.jeap.processcontext.archive.processsnapshot.v1.ProcessData.newBuilder().
                        setKey("key-1").
                        setValue("value-1").
                        setRole(("role-1")).
                        build())).
                setTasks(List.of(ch.admin.bit.jeap.processcontext.archive.processsnapshot.v1.Task.newBuilder().
                        setTaskType("test-task-type").
                        setTaskTypeLabel("test-task-type-description").
                        setOriginTaskId("test-task-origin-id").
                        setState("COMPLETED").
                        setDateTimeCreated(created).
                        setDateTimePlanned(planned).
                        setDateTimeCompleted(completed)
                        .build()))
                .build();
    }

    @SuppressWarnings("SameParameterValue")
    private static ProcessSnapshot createProcessSnapshot(String originProcessId, String state) {
        Instant created = ZonedDateTime.now().truncatedTo(ChronoUnit.MILLIS).toInstant();
        Instant planned = created.plusSeconds(5);
        Instant modified = planned.plusSeconds(5);
        Instant completed = modified.plusSeconds(5);
        Instant snapshotCreated = completed.plusSeconds(5);
        return ProcessSnapshot.newBuilder().
                setOriginProcessId(originProcessId).
                setTemplateName("test-template-name").
                setTemplateLabel("test-template-description").
                setState(state).
                setDateTimeCreated(created).
                setDateTimeModified(modified).
                setDateTimeCompleted(completed).
                setSnapshotDateTimeCreated(snapshotCreated).
                setProcessData(List.of(ProcessData.newBuilder().
                        setKey("key-1").
                        setValue("value-1").
                        setRole(("role-1")).
                        build())).
                setTasks(List.of(Task.newBuilder().
                        setTaskType("test-task-type").
                        setTaskTypeLabel("test-task-type-description").
                        setOriginTaskId("test-task-origin-id").
                        setState("COMPLETED").
                        setDateTimeCreated(created).
                        setDateTimePlanned(planned).
                        setDateTimeCompleted(completed).
                        setTaskData(List.of(TaskData.newBuilder().
                                setKey("task-data-key-1").
                                setLabel("task-data-label-1").
                                setValue("task-data-value-1").
                                build())).
                        setPlannedBy(User.newBuilder().
                                setUserData(List.of(UserData.newBuilder().
                                        setKey("user-data-key-1").
                                        setLabel("user-data-value-1").
                                        setValue("user-data-value-1").
                                        build()))
                                .build())
                        .build()))
                .build();
    }

    @SuppressWarnings("SameParameterValue")
    private void assertSnapshotTag(String originProcessId, int version) {
        final Tag snapshotTag = Tag.builder().key("pcs-object-type").value("snapshot").build();
        var getObjectTaggingResponse = s3Client.getObjectTagging(GetObjectTaggingRequest.builder()
                .bucket(TEST_BUCKET_NAME)
                .key(originProcessId + "/" + version)
                .build());
        assertThat(getObjectTaggingResponse.tagSet()).contains(snapshotTag);
    }

    @SneakyThrows
    private S3ObjectStorageProperties createS3ObjectStorageProperties() {
        S3ObjectStorageConnectionProperties connectionProperties = new S3ObjectStorageConnectionProperties();
        connectionProperties.setAccessKey(minioContainer.getUserName());
        connectionProperties.setSecretKey(minioContainer.getPassword());
        connectionProperties.setAccessUrl(minioContainer.getS3URL());
        S3ObjectStorageProperties objectStorageProperties = new S3ObjectStorageProperties();
        objectStorageProperties.setSnapshotBucket(TEST_BUCKET_NAME);
        objectStorageProperties.setSnapshotRetentionDays(SNAPSHOT_RETENTION_DAYS);
        objectStorageProperties.setConnection(connectionProperties);
        return objectStorageProperties;
    }

    private void setupStorage() {
        CreateBucketRequest cbr = CreateBucketRequest.builder()
                .bucket(TEST_BUCKET_NAME)
                .objectLockEnabledForBucket(true)
                .build();
        s3Client.createBucket(cbr);
    }

    @SneakyThrows
    private void checkStorage() {
        final String objectKey = "check-object";
        final byte[] content = "check-content".getBytes(StandardCharsets.UTF_8);
        final String tagName = "check-tag";
        final String tagValue = "check-tag-value";

        // Check creating an object
        Tag tag = Tag.builder().key(tagName).value(tagValue).build();
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(TEST_BUCKET_NAME)
                .key(objectKey)
                .contentLength((long) content.length)
                .contentMD5(computeMD5HashBase64(content))
                .objectLockMode(ObjectLockMode.COMPLIANCE)
                .objectLockRetainUntilDate(ZonedDateTime.now().plusDays(30).toInstant())
                .tagging(Tagging.builder().tagSet(tag).build())
                .build();
        try {
            s3Client.putObject(request, RequestBody.fromBytes(content));
        } catch (Exception pe) {
            log.error("Putting check object failed.", pe);
            throw pe;
        }

        // Check reading created object
        try {
            s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(objectKey).build());
        } catch (Exception ge) {
            log.error("Reading check object failed.", ge);
            throw ge;
        }
    }

    private String computeMD5HashBase64(byte[] object) {
        return Md5Utils.md5AsBase64(object);
    }

    @SneakyThrows
    private static <T extends org.apache.avro.specific.SpecificRecord> byte[] serializeProcessSnapshot(T processSnapshot) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
        DatumWriter<SpecificRecord> datumWriter = new SpecificDatumWriter<>(processSnapshot.getSchema());
        datumWriter.write(processSnapshot, encoder);
        encoder.flush();
        return outputStream.toByteArray();
    }

}
