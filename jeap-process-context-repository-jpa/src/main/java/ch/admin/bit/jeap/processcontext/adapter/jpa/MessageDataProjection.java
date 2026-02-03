package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.plugin.api.message.MessageData;

/**
 * Projection interface for MessageData.
 */
interface MessageDataProjection {
    String getKey();

    String getValue();

    String getRole();

    default MessageData toMessageData() {
        return new MessageData(getKey(), getValue(), getRole());
    }
}
