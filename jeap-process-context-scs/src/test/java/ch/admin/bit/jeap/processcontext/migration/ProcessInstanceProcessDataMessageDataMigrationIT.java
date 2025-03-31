package ch.admin.bit.jeap.processcontext.migration;

import ch.admin.bit.jeap.processcontext.ProcessInstanceMockS3ITBase;
import ch.admin.bit.jeap.processcontext.adapter.restapi.ProcessInstanceController;
import ch.admin.bit.jeap.processcontext.adapter.restapi.model.ProcessInstanceDTO;
import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.message.MessageData;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceService;
import ch.admin.bit.jeap.processcontext.domain.tx.Transactions;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZonedDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;


@Slf4j
class ProcessInstanceProcessDataMessageDataMigrationIT extends ProcessInstanceMockS3ITBase {

    @Autowired
    private ProcessInstanceService processInstanceService;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private Transactions transactions;
    @Autowired
    private ProcessInstanceRepository processInstanceRepository;
    @Autowired
    private ProcessInstanceController processInstanceController;

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void updateProcessState_whenProcessDataAndEventCorrelated_thenProcessDataAndEventAvailableAfterMigration() {
        // Start a new process
        String processTemplateName = "migrationFullTest";

        createProcessInstanceFromTemplate(processTemplateName);
        assertProcessInstanceCreatedEvent(originProcessId, processTemplateName);
        transactions.withinNewTransaction(() -> {
            final Message message = Message.messageBuilder()
                    .messageId("eventId")
                    .idempotenceId("idempotenceId")
                    .messageName("Test1Event")
                    .messageCreatedAt(ZonedDateTime.now())
                    .messageData(Set.of(MessageData.builder().key("correlationEventDataKey")
                            .value("value")
                            .templateName("migrationFullTest")
                            .build()))
                    .build();
            entityManager.persist(message);
            ProcessInstance processInstance1 = processInstanceRepository.findByOriginProcessIdLoadingMessages(originProcessId).orElseThrow();
            log.debug("processInstance {} State {}", processInstance1.getId(), processInstance1.getState());
            processInstance1.addMessage(message);
        });
        // Update template name for the process instance
        transactions.withinNewTransaction(() -> entityManager
                .createQuery("UPDATE ProcessInstance p SET p.processTemplateName='migrationFullTestNew' WHERE p.originProcessId='" + originProcessId + "'")
                .executeUpdate());

        // Trigger migration for process instances with changed template hash
        processInstanceService.updateProcessState(originProcessId);

        transactions.withinNewTransaction(() -> {
            final ProcessInstance processInstance = processInstanceRepository.findByOriginProcessIdLoadingMessages(originProcessId).orElseThrow();
            assertThat(processInstance.getProcessData()).hasSize(1);
            assertThat(processInstance.getMessageReferences()).hasSize(1);
            assertThat(processInstance.getProcessTemplate().getProcessDataTemplates()).isEmpty();
        });

        transactions.withinNewTransaction(() -> {
            final ProcessInstanceDTO processInstanceByOriginProcessId = processInstanceController.getProcessInstanceByOriginProcessId(originProcessId);
            assertThat(processInstanceByOriginProcessId.getProcessData()).hasSize(1);
            assertThat(processInstanceByOriginProcessId.getMessages()).hasSize(1);
            assertThat(processInstanceByOriginProcessId.getMilestones().get(0).getState()).isEqualTo("DELETED");
        });
    }

    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }
}
