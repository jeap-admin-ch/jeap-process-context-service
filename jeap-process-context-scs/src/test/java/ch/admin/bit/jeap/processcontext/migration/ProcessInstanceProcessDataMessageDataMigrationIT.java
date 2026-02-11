package ch.admin.bit.jeap.processcontext.migration;

import ch.admin.bit.jeap.processcontext.ProcessInstanceMockS3ITBase;
import ch.admin.bit.jeap.processcontext.adapter.restapi.ProcessInstanceController;
import ch.admin.bit.jeap.processcontext.adapter.restapi.model.ProcessInstanceDTO;
import ch.admin.bit.jeap.processcontext.domain.message.MessageReferenceRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessDataRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceService;
import ch.admin.bit.jeap.processcontext.domain.tx.Transactions;
import ch.admin.bit.jeap.processcontext.testevent.Test1EventBuilder;
import ch.admin.bit.jeap.processcontext.testevent.Test3EventBuilder;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;


@Slf4j
@TestPropertySource(properties =
        "jeap.processcontext.template.classpath-location-pattern=classpath:/process/templates/migration_full_test*.json")
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
    private ProcessDataRepository processDataRepository;
    @Autowired
    private ProcessInstanceController processInstanceController;
    @Autowired
    private MessageReferenceRepository messageReferenceRepository;

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void updateProcessState_whenProcessDataAndEventCorrelated_thenProcessDataAndEventAvailableAfterMigration() {
        // Start a new process
        createProcessInstance();

        sendTest1Event();
        await().pollInSameThread().untilAsserted(() -> {
            ProcessInstanceDTO processInstanceByOriginProcessId = processInstanceController.getProcessInstanceByOriginProcessId(originProcessId);
            assertThat(processInstanceByOriginProcessId.getMessages()).hasSize(2);
        });

        // Update template name for the process instance
        transactions.withinNewTransaction(() -> entityManager
                .createQuery("UPDATE ProcessInstance p SET p.processTemplateName='migrationFullTestNew' WHERE p.originProcessId='" + originProcessId + "'")
                .executeUpdate());

        // Trigger migration for process instances with changed template hash
        processInstanceService.migrateProcessInstanceTemplate(originProcessId);

        transactions.withinNewTransaction(() -> {
            ProcessInstance processInstance = processInstanceRepository.findByOriginProcessId(originProcessId).orElseThrow();
            assertThat(processDataRepository.findByProcessInstanceId(processInstance.getId())).hasSize(1);
            assertThat(messageReferenceRepository.findByProcessInstanceId(processInstance.getId())).hasSize(2);
            assertThat(processInstance.getProcessTemplate().getProcessDataTemplates()).isEmpty();
        });

        transactions.withinNewTransaction(() -> {
            ProcessInstanceDTO processInstanceByOriginProcessId = processInstanceController.getProcessInstanceByOriginProcessId(originProcessId);
            assertThat(processInstanceByOriginProcessId.getProcessData()).hasSize(1);
            assertThat(processInstanceByOriginProcessId.getMessages()).hasSize(2);
        });
    }

    private void createProcessInstance() {
        sendSync("topic.test3", Test3EventBuilder.createForProcessId(originProcessId).build());
        assertProcessInstanceCreated(originProcessId, "migrationFullTest");
    }

    private void sendTest1Event() {
        sendSync("topic.test1", Test1EventBuilder.createForProcessId(originProcessId).build());
    }

    @Override
    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }
}
