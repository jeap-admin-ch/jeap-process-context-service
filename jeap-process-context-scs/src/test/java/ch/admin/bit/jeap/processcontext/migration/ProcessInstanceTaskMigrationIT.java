package ch.admin.bit.jeap.processcontext.migration;

import ch.admin.bit.jeap.processcontext.ProcessInstanceMockS3ITBase;
import ch.admin.bit.jeap.processcontext.adapter.restapi.ProcessInstanceController;
import ch.admin.bit.jeap.processcontext.adapter.restapi.model.ProcessInstanceDTO;
import ch.admin.bit.jeap.processcontext.domain.processinstance.*;
import ch.admin.bit.jeap.processcontext.domain.tx.Transactions;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import jakarta.persistence.EntityManager;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;


class ProcessInstanceTaskMigrationIT extends ProcessInstanceMockS3ITBase {

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
    void updateProcessState_whenTasksAreNewOrDeleted_thenNewTaskCreatedOldTaskDeleted() {
        // Start a new process
        String processTemplateName = "migrationTaskTest";
        createProcessInstanceFromTemplate(processTemplateName);
        assertProcessInstanceCreatedEvent(originProcessId, processTemplateName);

        // Update template name for the process instance
        transactions.withinNewTransaction(() -> entityManager
                .createQuery("UPDATE ProcessInstance p SET p.processTemplateName='migrationTaskTestNew' WHERE p.originProcessId='" + originProcessId + "'")
                .executeUpdate());

        // Trigger migration for process instances with changed template hash
        processInstanceService.updateProcessState(originProcessId);

        Awaitility.await().until(() -> {
            final ProcessInstance processInstance = processInstanceRepository.findByOriginProcessIdLoadingMessages(originProcessId).orElseThrow();
            return processInstance.getTasks().size() == 3;
        });

        final ProcessInstance processInstance = processInstanceRepository.findByOriginProcessIdLoadingMessages(originProcessId).orElseThrow();
        assertThat((processInstance.getTasks().stream().filter(t -> t.getTaskTypeName().equals("task3") && t.getState() == TaskState.UNKNOWN).count())).isEqualTo(1L);
        assertThat((processInstance.getTasks().stream().filter(t -> t.getTaskTypeName().equals("task2") && t.getState() == TaskState.DELETED).count())).isEqualTo(1L);
        assertThat((processInstance.getTasks().stream().filter(t -> t.getTaskTypeName().equals("task1") && t.getState() == TaskState.PLANNED).count())).isEqualTo(1L);
        assertThat(processInstance.getState()).isEqualTo(ProcessState.STARTED);
        final ProcessInstanceDTO processInstanceByOriginProcessId = processInstanceController.getProcessInstanceByOriginProcessId(originProcessId);

        assertThat(processInstanceByOriginProcessId.getTasks()).hasSize(3);
        assertThat((processInstanceByOriginProcessId.getTasks().stream().filter(t -> t.getName().get("de").equals("migrationTaskTestNew.task.task3") && t.getState().equals("UNKNOWN")).count())).isEqualTo(1L);
        assertThat((processInstanceByOriginProcessId.getTasks().stream().filter(t -> t.getName().get("de").equals("migrationTaskTestNew.task.task2") && t.getState().equals("DELETED")).count())).isEqualTo(1L);
        assertThat((processInstanceByOriginProcessId.getTasks().stream().filter(t -> t.getName().get("de").equals("migrationTaskTestNew.task.task1") && t.getState().equals("PLANNED")).count())).isEqualTo(1L);
        assertThat(processInstanceByOriginProcessId.getState()).isEqualTo("STARTED");
    }

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void updateProcessState_whenTasksAreNew_thenOnlyAddTasksRequiringUnplannedInstance() {
        // Start a new process
        String processTemplateName = "migrationTaskTest";
        createProcessInstanceFromTemplate(processTemplateName);
        assertProcessInstanceCreatedEvent(originProcessId, processTemplateName);

        // Update template name for the process instance
        transactions.withinNewTransaction(() -> entityManager
                .createQuery("UPDATE ProcessInstance p SET p.processTemplateName='migrationTaskTestOptional' WHERE p.originProcessId='" + originProcessId + "'")
                .executeUpdate());

        // Trigger migration for process instances with changed template hash
        processInstanceService.updateProcessState(originProcessId);

        Awaitility.await().until(() -> {
            final ProcessInstance processInstance = processInstanceRepository.findByOriginProcessIdLoadingMessages(originProcessId).orElseThrow();
            return processInstance.getTasks().size() == 3;
        });

        final ProcessInstance processInstance = processInstanceRepository.findByOriginProcessIdLoadingMessages(originProcessId).orElseThrow();

        assertThat((processInstance.getTasks().stream().filter(t -> t.getTaskTypeName().equals("task2") && t.getState() == TaskState.PLANNED).count())).isEqualTo(1L);
        assertThat((processInstance.getTasks().stream().filter(t -> t.getTaskTypeName().equals("task1") && t.getState() == TaskState.PLANNED).count())).isEqualTo(1L);
        assertThat((processInstance.getTasks().stream().filter(t -> t.getTaskTypeName().equals("staticSingleInstanceTask") && t.getState() == TaskState.UNKNOWN).count())).isEqualTo(1L);
        assertThat(processInstance.getState()).isEqualTo(ProcessState.STARTED);
        final ProcessInstanceDTO processInstanceByOriginProcessId = processInstanceController.getProcessInstanceByOriginProcessId(originProcessId);

        assertThat(processInstanceByOriginProcessId.getTasks()).hasSize(3);
        assertThat((processInstanceByOriginProcessId.getTasks().stream().filter(t -> t.getName().get("de").equals("migrationTaskTestOptional.task.task2") && t.getState().equals("PLANNED")).count())).isEqualTo(1L);
        assertThat((processInstanceByOriginProcessId.getTasks().stream().filter(t -> t.getName().get("de").equals("migrationTaskTestOptional.task.task1") && t.getState().equals("PLANNED")).count())).isEqualTo(1L);
        assertThat((processInstanceByOriginProcessId.getTasks().stream().filter(t -> t.getName().get("de").equals("migrationTaskTestOptional.task.staticSingleInstanceTask") && t.getState().equals("UNKNOWN")).count())).isEqualTo(1L);
        assertThat(processInstanceByOriginProcessId.getState()).isEqualTo("STARTED");
    }

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void updateProcessState_whenTasksAreDeleted_thenProcessIsCompleted() {
        // Start a new process
        String processTemplateName = "migrationTaskTest";
        createProcessInstanceFromTemplate(processTemplateName);
        assertProcessInstanceCreatedEvent(originProcessId, processTemplateName);

        // Update template name for the process instance
        transactions.withinNewTransaction(() -> entityManager
                .createQuery("UPDATE ProcessInstance p SET p.processTemplateName='migrationTaskTestAllDeleted' WHERE p.originProcessId='" + originProcessId + "'")
                .executeUpdate());

        // Set the task state to completed
        transactions.withinNewTransaction(() -> entityManager
                .createQuery("UPDATE TaskInstance t SET t.state='COMPLETED' WHERE t.taskTypeName='task1'")
                .executeUpdate());

        // Trigger migration for process instances with changed template hash
        processInstanceService.updateProcessState(originProcessId);

        Awaitility.await().until(() -> {
            final ProcessInstance processInstance = processInstanceRepository.findByOriginProcessIdLoadingMessages(originProcessId).orElseThrow();
            return processInstance.getTasks().size() == 2 && processInstance.getState() == ProcessState.COMPLETED;
        });

        final ProcessInstance processInstance = processInstanceRepository.findByOriginProcessIdLoadingMessages(originProcessId).orElseThrow();

        assertThat((processInstance.getTasks().stream().filter(t -> t.getTaskTypeName().equals("task2") && t.getState() == TaskState.DELETED).count())).isEqualTo(1L);
        assertThat((processInstance.getTasks().stream().filter(t -> t.getTaskTypeName().equals("task1") && t.getState() == TaskState.COMPLETED).count())).isEqualTo(1L);
        final ProcessInstanceDTO processInstanceByOriginProcessId = processInstanceController.getProcessInstanceByOriginProcessId(originProcessId);

        assertThat(processInstanceByOriginProcessId.getTasks()).hasSize(2);
        assertThat((processInstanceByOriginProcessId.getTasks().stream().filter(t -> t.getName().get("de").equals("migrationTaskTestAllDeleted.task.task2") && t.getState().equals("DELETED")).count())).isEqualTo(1L);
        assertThat((processInstanceByOriginProcessId.getTasks().stream().filter(t -> t.getName().get("de").equals("migrationTaskTestAllDeleted.task.task1") && t.getState().equals("COMPLETED")).count())).isEqualTo(1L);
        assertThat(processInstanceByOriginProcessId.getState()).isEqualTo("COMPLETED");
    }

    @Override
    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }
}
