package ch.admin.bit.jeap.processcontext.domain.processinstance.api;

import java.util.UUID;

public interface ProcessContextRepositoryFacade {

    boolean isAllTasksInFinalState(UUID processInstanceId);
}
