package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceQueryRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.migration.ProcessInstanceMigrationService;
import ch.admin.bit.jeap.processcontext.domain.tx.Transactions;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZonedDateTime;

import static org.awaitility.Awaitility.await;

class ProcessInstanceTemplateMigrationIT extends ProcessInstanceMockS3ITBase {

    @Autowired
    private ProcessInstanceMigrationService processInstanceMigrationService;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private Transactions transactions;
    @Autowired
    private ProcessInstanceQueryRepository processInstanceQueryRepository;

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void processWithOptionalAndMandatoryTasks_whenTasksArePlanned_thenExpectCanComplete() {
        // Start a new process
        String processTemplateName = "migrationTest";
        createProcessInstanceFromTemplate(processTemplateName);
        assertProcessInstanceCreatedEvent(originProcessId, processTemplateName);

        // Force persisted process template hash to be different from current template hash
        transactions.withinNewTransaction(() -> entityManager
                .createQuery("UPDATE ProcessInstance p SET p.processTemplateHash='updated-hash' WHERE p.originProcessId='" + originProcessId + "'")
                .executeUpdate());

        // Trigger migration for process instances with changed template hash
        processInstanceMigrationService.triggerMigrationForModifiedTemplates(ZonedDateTime.now().minusYears(1));

        // Wait until migration has been applied (detected by hash updated after migrations have been applied)
        String expectedHash = processTemplateRepository.findByName(processTemplateName).orElseThrow().getTemplateHash();
        awaitProcessTemplateHashUpdatedTo(expectedHash);
    }

    private void awaitProcessTemplateHashUpdatedTo(String expectedHash) {
        await().atMost(TIMEOUT).pollInSameThread()
                .until(() -> expectedHash.equals(getProcessTemplateHash()));
    }

    private String getProcessTemplateHash() {
        return processInstanceQueryRepository.findByOriginProcessIdLoadingMessages(originProcessId).orElseThrow().getProcessTemplateHash();
    }

    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }
}
