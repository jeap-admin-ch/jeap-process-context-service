package ch.admin.bit.jeap.processcontext.repository.template.json.stubs;

import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.processcontext.plugin.api.message.MessageData;
import ch.admin.bit.jeap.processcontext.plugin.api.message.MessageExtractor;

import java.util.Set;

public class TestMessageExtractor implements MessageExtractor<Message> {

    @Override
    public Set<MessageData> getMessageData(Message message) {
        return Set.of(new MessageData("messageId", message.getIdentity().getId()));
    }
}
