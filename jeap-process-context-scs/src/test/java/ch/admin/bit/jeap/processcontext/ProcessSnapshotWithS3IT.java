package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.adapter.objectstorage.S3ObjectStorageConnectionProperties;
import ch.admin.bit.jeap.processcontext.adapter.objectstorage.S3ProcessSnapshotRepository;
import ch.admin.bit.jeap.processcontext.adapter.objectstorage.TimedS3Client;
import ch.admin.bit.jeap.processcontext.adapter.restapi.model.MessageDTO;
import ch.admin.bit.jeap.processcontext.adapter.restapi.model.ProcessInstanceDTO;
import ch.admin.bit.jeap.processcontext.domain.processinstance.SerializedProcessSnapshotArchiveData;
import ch.admin.bit.jeap.processcontext.event.test1.Test1Event;
import ch.admin.bit.jeap.processcontext.event.test2.Test2Event;
import ch.admin.bit.jeap.processcontext.testevent.Test1EventBuilder;
import ch.admin.bit.jeap.processcontext.testevent.Test2EventBuilder;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@SuppressWarnings("SameParameterValue")
@Slf4j
class ProcessSnapshotWithS3IT extends ProcessInstanceITBase {

    private static final String MINIO_IMAGE = "minio/minio:RELEASE.2025-09-07T16-13-09Z";
    private static final String TEST_BUCKET_NAME = "test-bucket";

    private static final MinIOContainer MINIO_CONTAINER = new MinIOContainer(MINIO_IMAGE);

    @DynamicPropertySource
    static void registerObjectStorageProperties(DynamicPropertyRegistry registry) {
        registry.add("jeap.processcontext.objectstorage.snapshot-bucket", () -> TEST_BUCKET_NAME);
        registry.add("jeap.processcontext.objectstorage.connection.accessUrl", MINIO_CONTAINER::getS3URL);
        registry.add("jeap.processcontext.objectstorage.connection.accessKey", MINIO_CONTAINER::getUserName);
        registry.add("jeap.processcontext.objectstorage.connection.secretKey", MINIO_CONTAINER::getPassword);
    }

    @BeforeAll
    static void setup() {
        MINIO_CONTAINER.start();
        TimedS3Client s3Client = new TimedS3Client(createS3ConnectionProperties(), null);
        s3Client.createBucket(CreateBucketRequest.builder().bucket(TEST_BUCKET_NAME).build());
    }

    @AfterAll
    static void tearDown() {
        MINIO_CONTAINER.stop();
    }

    @Autowired
    private S3ProcessSnapshotRepository s3ProcessSnapshotRepository;

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void testCreateSnapshots() {
        // start a new process
        String processTemplateName = "snapshots";
        createProcessInstanceFromTemplate(processTemplateName);

        // send Test1Event that fulfills the programmatic snapshot condition
        sendTest1Event("trigger");
        assertMessageCount(originProcessId, "Test1Event", 1);

        // send Test2Event that complete the only task in the process and thus the process and triggers a snapshot on completion
        sendTest2Event("complete");
        assertMessageCount(originProcessId, "Test2Event", 1);

        // wait for the process to complete
        assertProcessInstanceCompleted(originProcessId);

        // assert that two process snapshot versions have been created
        Optional<SerializedProcessSnapshotArchiveData> snapshotV1Optional = s3ProcessSnapshotRepository.loadSnapshot(originProcessId, 1);
        assertThat(snapshotV1Optional).isPresent();
        SerializedProcessSnapshotArchiveData snapshotV1 = snapshotV1Optional.get();
        assertThat(snapshotV1.getSerializedProcessSnapshot()).isNotEmpty();
        assertThat(snapshotV1.getMetadata().getSnapshotVersion()).isEqualTo(1);
        Optional<SerializedProcessSnapshotArchiveData> snapshotV2Optional = s3ProcessSnapshotRepository.loadSnapshot(originProcessId, 2);
        assertThat(snapshotV2Optional).isPresent();
        SerializedProcessSnapshotArchiveData snapshotV2 = snapshotV2Optional.get();
        assertThat(snapshotV2.getSerializedProcessSnapshot()).isNotEmpty();
        assertThat(snapshotV2.getMetadata().getSnapshotVersion()).isEqualTo(2);

        // Snapshot created events should have been published for the two snapshot versions
        assertSnapshotCreatedEvents(1, 2);

        // Clear database
        String processId = this.originProcessId;
        clearDatabase();

        // Retrieve process instance DTO using the REST API, assert that it is read
        // from the snapshot on S3 and not from the DB
        ProcessInstanceDTO dto = Awaitility.await()
                .pollInSameThread()
                .atMost(TIMEOUT)
                .until(() -> processInstanceController.getProcessInstanceByOriginProcessId(processId),
                        Matchers.hasProperty("snapshot", is(equalTo(true))));

        assertThat(dto.getName()).containsEntry("de", "snapshots");
        assertThat(dto.getProcessCompletion().getConclusion())
                .isEqualTo("SUCCEEDED");
        assertThat(dto.getProcessCompletion().getCompletedAt())
                .isNotNull();
        assertThat(dto.getProcessCompletion().getReason()).containsEntry("de", "allTasksInFinalStateProcessCompletionCondition");
    }

    private void assertMessageCount(String originProcessId, String messageType, long count) {
        Awaitility.await()
                .pollInSameThread()
                .atMost(TIMEOUT)
                .until(() -> countProcessEventsOfType(originProcessId, messageType), is(equalTo(count)));
    }

    private long countProcessEventsOfType(String originProcessId, String type) {
        ProcessInstanceDTO processInstanceDTO = processInstanceController.getProcessInstanceByOriginProcessId(originProcessId);
        return processInstanceDTO.getMessages().stream().
                map(MessageDTO::getName).
                filter(type::equals).
                count();
    }

    private void sendTest1Event(String s) {
        Test1Event event1 = Test1EventBuilder.createForProcessId(originProcessId)
                .taskIds(s)
                .build();
        sendSync("topic.test1", event1);
    }

    private void sendTest2Event(String s) {
        Test2Event event2 = Test2EventBuilder.createForProcessId(originProcessId)
                .objectId(s)
                .build();
        sendSync("topic.test2", event2);
    }

    @Override
    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }

    private static S3ObjectStorageConnectionProperties createS3ConnectionProperties() {
        S3ObjectStorageConnectionProperties connectionProperties = new S3ObjectStorageConnectionProperties();
        connectionProperties.setAccessKey(MINIO_CONTAINER.getUserName());
        connectionProperties.setSecretKey(MINIO_CONTAINER.getPassword());
        connectionProperties.setAccessUrl(MINIO_CONTAINER.getS3URL());
        return connectionProperties;
    }
}
