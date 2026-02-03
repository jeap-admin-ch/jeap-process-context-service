package ch.admin.bit.jeap.processcontext.plugin.api.message;

import ch.admin.bit.jeap.messaging.model.MessagePayload;

import java.util.Collections;
import java.util.Set;

public interface PayloadExtractor<E extends MessagePayload> {

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
