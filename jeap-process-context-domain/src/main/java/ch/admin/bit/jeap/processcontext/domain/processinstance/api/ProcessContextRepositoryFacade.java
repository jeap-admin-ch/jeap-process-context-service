package ch.admin.bit.jeap.processcontext.domain.processinstance.api;

import java.util.UUID;

/**
 * Facade for querying process instances from conditions in the PCS API using the
 * {@link ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext} API model.
 */
public interface ProcessContextRepositoryFacade {

    boolean isAllTasksInFinalState(UUID processInstanceId);
}
