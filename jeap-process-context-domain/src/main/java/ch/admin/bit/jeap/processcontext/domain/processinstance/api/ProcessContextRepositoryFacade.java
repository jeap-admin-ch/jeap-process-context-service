package ch.admin.bit.jeap.processcontext.domain.processinstance.api;

import java.util.UUID;

public interface ProcessContextRepositoryFacade {

    boolean isAllTasksCompleted(UUID processInstanceId);
}
