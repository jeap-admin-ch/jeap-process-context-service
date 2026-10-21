package ch.admin.bit.jeap.processcontext.plugin.api.message;

import ch.admin.bit.jeap.messaging.model.Message;

import java.util.Collections;
import java.util.Set;

public interface MessageExtractor<E extends Message> {

    /**
     * Returns message related data from a message.
     * <p>
     * This information will be persisted in the process instance and will be available in the API Plugin.
     * It can then be used in the implementation of a custom completion condition.
     * <p>
     *
     * @param message Message
     * @return Set of {@link MessageData}
     * @see ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext
     * @see ch.admin.bit.jeap.processcontext.plugin.api.context.Message
     */
    default Set<MessageData> getMessageData(E message) {
        return Collections.emptySet();
    }
}
