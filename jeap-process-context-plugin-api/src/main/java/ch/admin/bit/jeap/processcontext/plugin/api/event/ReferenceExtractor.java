package ch.admin.bit.jeap.processcontext.plugin.api.event;

import ch.admin.bit.jeap.messaging.model.MessageReferences;

import java.util.Collections;
import java.util.Set;

public interface ReferenceExtractor<E extends MessageReferences> {

    /**
     * Returns event related data from the references of an event.
     * <p>
     * This information will be persisted in the process instance and will be available in the API Plugin.
     * <p>
     *
     * @see ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext
     * @see ch.admin.bit.jeap.processcontext.plugin.api.context.Message
     *
     * @deprecated Replaced by {@link #getMessageData(MessageReferences)}
     * @param references MessageReferences
     * @return Set of EventData
     */
    @SuppressWarnings("removal")
    @Deprecated(since = "7.0.0", forRemoval = true)
    default Set<EventData> getEventData(E references) {
        return Collections.emptySet();
    }

    /**
     * Returns message related data from the references of a message.
     * <p>
     * This information will be persisted in the process instance and will be available in the API Plugin.
     * <p>
     *
     * @param references MessageReferences
     * @return Set of {@link MessageData}
     * @see ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext
     * @see ch.admin.bit.jeap.processcontext.plugin.api.context.Message
     */
    default Set<MessageData> getMessageData(E references) {
        return Collections.emptySet();
    }

}
