package ch.admin.bit.jeap.processcontext.plugin.api.message;

import ch.admin.bit.jeap.messaging.model.Message;

/**
 * Represents a filter for messages of a specific type.
 *
 * @param <M> the type of message to filter
 */
public interface MessageFilter<M extends Message> {

    /**
     * Determines whether the given message should be filtered.
     *
     * @param message the message to evaluate
     * @return {@code true} if the message should be processed; {@code false} to ignore it
     */
    boolean filter(M message);

}
