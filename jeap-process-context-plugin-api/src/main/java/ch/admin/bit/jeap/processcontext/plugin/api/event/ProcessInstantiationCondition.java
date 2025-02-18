package ch.admin.bit.jeap.processcontext.plugin.api.event;

import ch.admin.bit.jeap.messaging.model.Message;

/**
 * Interface for implementing conditions ruling over the creation of new process instances based on messages.
 * @param <M> The type of the messages considered.
 */
public interface ProcessInstantiationCondition<M extends Message> {

    /**
     * Based on the given message, decides if a new process instance should be created.
     * @param message The message on which to base the decision.
     * @return <code>true</code> if a new process instance should be created, <code>false</code> otherwise.
     */
    boolean triggersProcessInstantiation(M message);

}
