package ch.admin.bit.jeap.processcontext.migration;

import ch.admin.bit.jeap.processcontext.ProcessInstanceMockS3ITBase;
import ch.admin.bit.jeap.processcontext.adapter.restapi.ProcessInstanceController;
import ch.admin.bit.jeap.processcontext.adapter.restapi.model.ProcessInstanceDTO;
import ch.admin.bit.jeap.processcontext.domain.processinstance.MilestoneState;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceService;
import ch.admin.bit.jeap.processcontext.domain.tx.Transactions;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessInstanceMilestoneMigrationIT extends ProcessInstanceMockS3ITBase {

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
    void updateProcessState_whenMilestonesAreNewOrDeleted_thenNewMilestonesCreatedOldMilestonesDeleted() {
        // Start a new process
        String processTemplateName = "migrationMilestoneTest";
        createProcessInstanceFromTemplate(processTemplateName);
        assertProcessInstanceCreatedEvent(originProcessId, processTemplateName);

        // Update template name for the process instance
        transactions.withinNewTransaction(() -> entityManager
                .createQuery("UPDATE ProcessInstance p SET p.processTemplateName='migrationMilestoneTestNew' WHERE p.originProcessId='" + originProcessId + "'")
                .executeUpdate());

        // Set the task state to completed
        transactions.withinNewTransaction(() -> entityManager
                .createQuery("UPDATE TaskInstance t SET t.state='COMPLETED' WHERE t.taskTypeName='task1'")
                .executeUpdate());

        // Trigger migration for process instances with changed template hash
        processInstanceService.updateProcessState(originProcessId);

        final ProcessInstance processInstance = processInstanceRepository.findByOriginProcessIdLoadingMessages(originProcessId).orElseThrow();

        assertThat(processInstance.getMilestones()).hasSize(4);

        assertThat((processInstance.getMilestones().stream().filter(t -> t.getName().equals("NewMilestone") && t.getState() == MilestoneState.UNKNOWN).count())).isEqualTo(1L);
        assertThat((processInstance.getMilestones().stream().filter(t -> t.getName().equals("NewCompletedMilestone") && t.getState() == MilestoneState.REACHED).count())).isEqualTo(1L);
        assertThat((processInstance.getMilestones().stream().filter(t -> t.getName().equals("DeletedMilestone") && t.getState() == MilestoneState.DELETED).count())).isEqualTo(1L);
        assertThat((processInstance.getMilestones().stream().filter(t -> t.getName().equals("MilestoneDontTouch") && t.getState() == MilestoneState.REACHED).count())).isEqualTo(1L);

        final ProcessInstanceDTO processInstanceByOriginProcessId = processInstanceController.getProcessInstanceByOriginProcessId(originProcessId);

        assertThat(processInstanceByOriginProcessId.getMilestones()).hasSize(4);
        assertThat((processInstanceByOriginProcessId.getMilestones().stream().filter(t -> t.getName().equals("NewMilestone") && t.getState().equals(MilestoneState.UNKNOWN.name())).count())).isEqualTo(1L);
        assertThat((processInstanceByOriginProcessId.getMilestones().stream().filter(t -> t.getName().equals("NewCompletedMilestone") && t.getState().equals(MilestoneState.REACHED.name())).count())).isEqualTo(1L);
        assertThat((processInstanceByOriginProcessId.getMilestones().stream().filter(t -> t.getName().equals("DeletedMilestone") && t.getState().equals(MilestoneState.DELETED.name())).count())).isEqualTo(1L);
        assertThat((processInstanceByOriginProcessId.getMilestones().stream().filter(t -> t.getName().equals("MilestoneDontTouch") && t.getState().equals(MilestoneState.REACHED.name())).count())).isEqualTo(1L);
    }

    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }
}
