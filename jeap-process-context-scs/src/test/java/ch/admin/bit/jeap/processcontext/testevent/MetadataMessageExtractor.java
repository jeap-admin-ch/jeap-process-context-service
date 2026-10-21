package ch.admin.bit.jeap.processcontext.testevent;

import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.processcontext.plugin.api.message.MessageData;
import ch.admin.bit.jeap.processcontext.plugin.api.message.MessageExtractor;

import java.util.Set;

public class MetadataMessageExtractor implements MessageExtractor<Message> {

    @Override
    public Set<MessageData> getMessageData(Message message) {
        return Set.of(
                new MessageData("messageId", message.getIdentity().getId()),
                new MessageData("messageType", message.getType().getName()));
    }
}
