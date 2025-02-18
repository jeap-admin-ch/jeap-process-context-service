package ch.admin.bit.jeap.processcontext.migration;

import ch.admin.bit.jeap.processcontext.ProcessInstanceMockS3ITBase;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceService;
import ch.admin.bit.jeap.processcontext.domain.tx.Transactions;
import ch.admin.bit.jeap.processcontext.event.test1.SubjectReference;
import ch.admin.bit.jeap.processcontext.event.test1.Test1Event;
import ch.admin.bit.jeap.processcontext.event.test1.Test1EventReferences;
import ch.admin.bit.jeap.processcontext.event.test2.Test2Event;
import ch.admin.bit.jeap.processcontext.testevent.Test1EventBuilder;
import ch.admin.bit.jeap.processcontext.testevent.Test2EventBuilder;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@Slf4j
class ProcessInstanceRelationMigrationIT extends ProcessInstanceMockS3ITBase {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private Transactions transactions;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ProcessInstanceService processInstanceService;

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void updateProcessState_whenRelations_thenRelationsAvailableAfterMigration() {
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
                .until(() -> transactions.withinNewTransactionWithResult(() -> processInstanceRepository.findByOriginProcessIdLoadingMessages(originProcessId).orElseThrow().getRelations().size() == 4));

        transactions.withinNewTransaction(() -> {
            final Optional<ProcessInstance> byOriginProcessId = processInstanceRepository.findByOriginProcessIdLoadingMessages(originProcessId);
            assertThat(byOriginProcessId.orElseThrow().getRelations()).hasSize(4);
        });

        // Update template name for the process instance
        transactions.withinNewTransaction(() -> entityManager
                .createQuery("UPDATE ProcessInstance p SET p.processTemplateName='migrationFullTestNew' WHERE p.originProcessId='" + originProcessId + "'")
                .executeUpdate());

        // Trigger migration for process instances with changed template hash
        processInstanceService.updateProcessState(originProcessId);

        transactions.withinNewTransaction(() -> {
            final Optional<ProcessInstance> byOriginProcessId = processInstanceRepository.findByOriginProcessIdLoadingMessages(originProcessId);
            assertThat(byOriginProcessId.orElseThrow().getRelations()).hasSize(4);
        });

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
        Test2Event event2_1 = Test2EventBuilder.createForProcessId(originProcessId)
                .objectId(s)
                .build();
        sendSync("topic.test2", event2_1);
    }

    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }
}
