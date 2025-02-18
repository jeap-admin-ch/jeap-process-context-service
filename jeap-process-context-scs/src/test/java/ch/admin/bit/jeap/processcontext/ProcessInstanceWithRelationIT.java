package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.event.test1.SubjectReference;
import ch.admin.bit.jeap.processcontext.event.test1.Test1Event;
import ch.admin.bit.jeap.processcontext.event.test1.Test1EventReferences;
import ch.admin.bit.jeap.processcontext.event.test2.Test2Event;
import ch.admin.bit.jeap.processcontext.plugin.api.relation.Relation;
import ch.admin.bit.jeap.processcontext.testevent.Test1EventBuilder;
import ch.admin.bit.jeap.processcontext.testevent.Test2EventBuilder;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import com.fasterxml.uuid.Generators;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class ProcessInstanceWithRelationIT extends ProcessInstanceMockS3ITBase {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private RelationListenerStub relationListenerStub;

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void processWithRelations_whenRelationsAreAdded_thenShouldNotifyListener() {
        // Start a new process
        String processTemplateName = "relations";
        createProcessInstanceFromTemplate(processTemplateName);
        assertProcessInstanceCreatedEvent(originProcessId, processTemplateName);

        // Add events, producing process data
        // Produce two events with reference to be extracted
        sendTest1Event("subjectId-1");
        sendTest1Event("subjectId-2");
        // Produce two events with payload to be extracted
        sendTest2Event("objectId-1");
        sendTest2Event("objectId-2");

        // Check that process completes after all tasks have been completed
        assertProcessInstanceCompleted(originProcessId);
        assertProcessInstanceCompletedEvent(originProcessId);
        Awaitility.await()
                .atMost(TIMEOUT)
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> {
                    List<Relation> relations = relationListenerStub.getRelations(originProcessId);
                    log.info("Relations: {}", relations);
                    return relations.size() == 4;
                });

        Relation expectedRelation1 = createRelation("subjectId-1", "objectId-1");
        Relation expectedRelation2 = createRelation("subjectId-1", "objectId-2");
        Relation expectedRelation3 = createRelation("subjectId-2", "objectId-1");
        Relation expectedRelation4 = createRelation("subjectId-2", "objectId-2");
        List<Relation> actualRelations = relationListenerStub.getRelations(originProcessId);
        assertEquals(Set.of(expectedRelation1, expectedRelation2, expectedRelation3, expectedRelation4), Set.copyOf(actualRelations));
    }

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void processWithRelations_whenJoinByRole_thenRoleValueShouldBeApplied() {
        // Start a new process
        String processTemplateName = "relations_join_byRole";
        createProcessInstanceFromTemplate(processTemplateName);
        assertProcessInstanceCreatedEvent(originProcessId, processTemplateName);

        // Add events, producing process data
        // Produce two events with reference to be extracted
        sendTest1Event("subjectId-1", "v1");
        sendTest1Event("subjectId-2", "v2");
        // Produce two events with payload to be extracted
        sendTest2Event("objectId-1", "v1");
        sendTest2Event("objectId-2", "v2");

        // Check that process completes after all tasks have been completed
        assertProcessInstanceCompleted(originProcessId);
        assertProcessInstanceCompletedEvent(originProcessId);
        Awaitility.await()
                .atMost(TIMEOUT)
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> {
                    List<Relation> relations = relationListenerStub.getRelations(originProcessId);
                    log.info("Relations: {}", relations);
                    return relations.size() == 2;
                });

        Relation expectedRelation1 = createRelation("subjectId-1", "objectId-1");
        Relation expectedRelation2 = createRelation("subjectId-2", "objectId-2");
        List<Relation> actualRelations = relationListenerStub.getRelations(originProcessId);
        assertEquals(Set.of(expectedRelation1, expectedRelation2), Set.copyOf(actualRelations));
    }

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void processWithRelations_whenJoinByValue_thenValueShouldBeApplied() {
        // Start a new process
        String processTemplateName = "relations_join_byValue";
        createProcessInstanceFromTemplate(processTemplateName);
        assertProcessInstanceCreatedEvent(originProcessId, processTemplateName);

        // Add events, producing process data
        // Produce two events with reference to be extracted
        sendTest1Event("red");
        sendTest1Event("blue");
        // Produce two events with payload to be extracted
        sendTest2Event("red");
        sendTest2Event("green");

        // Check that process completes after all tasks have been completed
        assertProcessInstanceCompleted(originProcessId);
        assertProcessInstanceCompletedEvent(originProcessId);
        Awaitility.await()
                .atMost(TIMEOUT)
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> {
                    List<Relation> relations = relationListenerStub.getRelations(originProcessId);
                    log.info("Relations: {}", relations);
                    return relations.size() == 1;
                });

        Relation expectedRelation1 = createRelation("red", "red");
        List<Relation> actualRelations = relationListenerStub.getRelations(originProcessId);
        assertEquals(Set.of(expectedRelation1), Set.copyOf(actualRelations));
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

    private void sendTest1Event(String s, String version) {
        Test1Event event1 = Test1EventBuilder.createForProcessId(originProcessId)
                .taskIds()
                .build();
        event1.setReferences(Test1EventReferences.newBuilder()
                .setSubjectReference(SubjectReference.newBuilder()
                        .setSubjectId(s)
                        .setVersion(version)
                        .build())
                .build());
        sendSync("topic.test1", event1);
    }

    private void sendTest2Event(String s) {
        Test2Event event2_1 = Test2EventBuilder.createForProcessId(originProcessId)
                .objectId(s)
                .build();
        sendSync("topic.test2", event2_1);
    }

    private void sendTest2Event(String s, String version) {
        Test2Event event2_1 = Test2EventBuilder.createForProcessId(originProcessId)
                .objectId(s)
                .version(version)
                .build();
        sendSync("topic.test2", event2_1);
    }

    private Relation createRelation(String s, String s2) {
        return Relation.builder()
                .systemId("ch.test.System")
                .originProcessId(originProcessId)
                .predicateType("ch.test.predicate.Knows")
                .subjectType("ch.test.Subject")
                .subjectId(s)
                .objectType("ch.test.Object")
                .objectId(s2)
                .createdAt(ZonedDateTime.now())
                .idempotenceId(Generators.timeBasedEpochGenerator().generate())
                .build();
    }

    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }
}
