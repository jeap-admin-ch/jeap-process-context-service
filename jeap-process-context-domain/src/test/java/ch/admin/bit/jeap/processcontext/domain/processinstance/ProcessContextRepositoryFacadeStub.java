package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextRepositoryFacade;

import java.util.UUID;

public class ProcessContextRepositoryFacadeStub implements ProcessContextRepositoryFacade {

    private ProcessInstance processInstance;

    @Override
    public boolean areAllTasksInFinalState(UUID processInstanceId) {
        if (processInstance == null) {
            return false;
        }
        return processInstance.getTasks().stream()
                .allMatch(task -> task.getState().isFinalState());
    }

    public void setProcessInstance(ProcessInstance processInstance) {
        this.processInstance = processInstance;
    }
}
