package ch.admin.bit.jeap.processcontext.plugin.api.message;

import ch.admin.bit.jeap.messaging.model.MessageReferences;

import java.util.Collections;
import java.util.Set;

public interface ReferenceExtractor<E extends MessageReferences> {

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
