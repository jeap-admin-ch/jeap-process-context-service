package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.adapter.objectstorage.S3ProcessSnapshotRepository;
import ch.admin.bit.jeap.processcontext.event.test1.SubjectReference;
import ch.admin.bit.jeap.processcontext.event.test1.Test1Event;
import ch.admin.bit.jeap.processcontext.event.test1.Test1EventReferences;
import ch.admin.bit.jeap.processcontext.testevent.Test1EventBuilder;
import ch.admin.bit.jeap.processcontext.testevent.Test2EventBuilder;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;

import static org.hamcrest.Matchers.containsString;

@TestPropertySource(properties =
        "jeap.processcontext.template.classpath-location-pattern=classpath:/process/templates/metrics.json")
class MetricsConfigIT extends ProcessInstanceMockS3ITBase {

    @LocalServerPort
    int localServerPort;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private RelationListenerStub relationListenerStub;

    @MockitoBean
    S3ProcessSnapshotRepository s3ProcessSnapshotRepository;

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void testExpectedMetrics_areProduced() {
        // given
        // Start a new process
        String processTemplateName = "metrics";

        // Add events, producing process data
        // Produce two events with reference to be extracted
        sendTest1Event("subjectId-1");
        assertProcessInstanceCreated(originProcessId, processTemplateName);
        sendTest1Event("subjectId-2");
        // Produce two events with payload to be extracted
        sendTest2Event("objectId-1");
        sendTest2Event("objectId-2");

        // Check that process completes after all tasks have been completed
        assertProcessInstanceCompleted(originProcessId);
        assertProcessInstanceCompleted(originProcessId);
        assertSnapshotCreatedEvent();
        Awaitility.await()
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> relationListenerStub.getRelations(originProcessId).size() == 4);

        // then
        requestWithPrometheusRole()
                .get("/process-context/actuator/prometheus")
                .then().assertThat()
                .statusCode(200)
                .body(containsString("pcs_process_instances"))
                .body(containsString("pcs_processes_completed_total"))
                .body(containsString("pcs_snapshot_total"))
                .body(containsString("pcs_messages_received_total"))
                .body(containsString("pcs_process_updates_processed_total"))
                .body(containsString("pcs_failed_process_updates_total"))
                .body(containsString("jeap_pcs_process_message"))
                .body(containsString("jeap_pcs_early_correlate_message"))
                .body(containsString("jeap_pcs_update_process_state"))
                .body(containsString("pcs_process_update"))
                .body(containsString("jeap_pcs_late_correlate_message"))
                .body(containsString("jeap_pcs_produce_snapshot_events"))
                .body(containsString("jeap_pcs_produce_process_snapshot_created_event"))
                .body(containsString("pcs_process_instances_total"));
    }

    private RequestSpecification requestWithPrometheusRole() {
        return RestAssured.given()
                .port(localServerPort)
                .auth().basic("prometheus", "test");
    }

    private void sendTest1Event(String s) {
        Test1Event event1 = Test1EventBuilder.createForProcessId(originProcessId)
                .taskIds()
                .build();
        event1.setReferences(Test1EventReferences.newBuilder()
                .setSubjectReference(SubjectReference.newBuilder()
                        .setSubjectId(s)
                        .build())
                .build());
        sendSync("topic.test1", event1);
    }

    private void sendTest2Event(String s) {
        sendSync("topic.test2", Test2EventBuilder.createForProcessId(originProcessId)
                .objectId(s)
                .build());
    }

    @Override
    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }
}
