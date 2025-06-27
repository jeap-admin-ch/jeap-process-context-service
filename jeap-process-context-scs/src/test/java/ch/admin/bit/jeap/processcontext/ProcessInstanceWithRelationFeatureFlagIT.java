package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.event.test1.SubjectReference;
import ch.admin.bit.jeap.processcontext.event.test1.Test1Event;
import ch.admin.bit.jeap.processcontext.event.test1.Test1EventReferences;
import ch.admin.bit.jeap.processcontext.plugin.api.relation.Relation;
import ch.admin.bit.jeap.processcontext.testevent.Test1EventBuilder;
import ch.admin.bit.jeap.processcontext.testevent.Test2EventBuilder;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class ProcessInstanceWithRelationFeatureFlagIT extends ProcessInstanceMockS3ITBase {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private RelationListenerStub relationListenerStub;

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void processWithRelations_whenFeatureFlagNotActive_thenShouldNotNotifyListener() {
        List<Relation> relations = processWithRelations_whenRelationsAreAdded_thenShouldNotifyListener("relations_feature_flags", 1);
        assertThat(relations).hasSize(1);
    }

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void processWithRelations_whenFeatureFlagActive_thenShouldNotifyListener() {
        List<Relation> relations = processWithRelations_whenRelationsAreAdded_thenShouldNotifyListener("relations_feature_flags_both_active", 2);
        assertThat(relations).hasSize(2);
    }

    protected List<Relation> processWithRelations_whenRelationsAreAdded_thenShouldNotifyListener(String processTemplateName, int size) {
        // Start a new process
        createProcessInstanceFromTemplate(processTemplateName);
        assertProcessInstanceCreatedEvent(originProcessId, processTemplateName);

        sendTest1Event("subjectId-1");
        sendTest2Event("objectId-1");

        // Check that process completes after all tasks have been completed
        assertProcessInstanceCompleted(originProcessId);
        assertProcessInstanceCompletedEvent(originProcessId);
        Awaitility.await()
                .atMost(TIMEOUT)
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> {
                    List<Relation> relations = relationListenerStub.getRelations(originProcessId);
                    log.info("Relations: {}", relations);
                    return relations.size() == size;
                });

        return relationListenerStub.getRelations(originProcessId);
    }

    private void sendTest1Event(String s) {
        Test1Event event1 = Test1EventBuilder.createForProcessId(originProcessId).taskIds().build();
        event1.setReferences(Test1EventReferences.newBuilder()
                .setSubjectReference(SubjectReference.newBuilder()
                        .setSubjectId(s)
                        .build())
                .build());
        sendSync("topic.test1", event1);
    }

    private void sendTest2Event(String s) {
        sendSync("topic.test2", Test2EventBuilder.createForProcessId(originProcessId).objectId(s).build());
    }

    @Override
    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }
}
