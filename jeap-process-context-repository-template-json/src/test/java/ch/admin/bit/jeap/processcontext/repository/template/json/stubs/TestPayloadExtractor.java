package ch.admin.bit.jeap.processcontext.repository.template.json.stubs;

import ch.admin.bit.jeap.messaging.model.MessagePayload;
import ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData;
import ch.admin.bit.jeap.processcontext.plugin.api.event.PayloadExtractor;

import java.util.Set;

public class TestPayloadExtractor implements PayloadExtractor<MessagePayload> {

    @Override
    public Set<MessageData> getMessageData(MessagePayload payload) {
        return Set.of(new MessageData("myKey", "myValue"));
    }
}
