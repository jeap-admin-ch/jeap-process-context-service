package ch.admin.bit.jeap.processcontext.domain.processinstance.api;

import ch.admin.bit.jeap.processcontext.domain.message.MessageSearchQueryRepository;

import java.util.UUID;

/**
 * Facade for querying process instances from conditions in the PCS API using the
 * {@link ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext} API model.
 */
public interface ProcessContextRepositoryFacade extends MessageSearchQueryRepository {

    boolean areAllTasksInFinalState(UUID processInstanceId);
}
