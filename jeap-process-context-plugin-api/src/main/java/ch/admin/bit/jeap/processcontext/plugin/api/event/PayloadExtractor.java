package ch.admin.bit.jeap.processcontext.plugin.api.event;

import ch.admin.bit.jeap.messaging.model.MessagePayload;

import java.util.Collections;
import java.util.Set;

public interface PayloadExtractor<E extends MessagePayload> {

    /**
     * Returns event related data from payload of an event.
     * <p>
     * This information will be persisted in the process instance and will be available in the API Plugin.
     * It can then be used in the implementation of a custom completion condition.
     * <p>
     * @see ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext
     * @see ch.admin.bit.jeap.processcontext.plugin.api.context.Message
     *
     * @deprecated Replaced by {@link #getMessageData(MessagePayload)}
     * @param payload MessagePayload
     * @return Set of {@link EventData}
     */
    @SuppressWarnings("removal")
    @Deprecated(since = "7.0.0", forRemoval = true)
    default Set<EventData> getEventData(E payload) {
        return Collections.emptySet();
    }

    /**
     * Returns message related data from payload of a message.
     * <p>
     * This information will be persisted in the process instance and will be available in the API Plugin.
     * It can then be used in the implementation of a custom completion condition.
     * <p>
     *
     * @param payload MessagePayload
     * @return Set of {@link MessageData}
     * @see ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext
     * @see ch.admin.bit.jeap.processcontext.plugin.api.context.Message
     */
    default Set<MessageData> getMessageData(E payload) {
        return Collections.emptySet();
    }
}
