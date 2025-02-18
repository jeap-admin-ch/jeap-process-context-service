package ch.admin.bit.jeap.processcontext.repository.template.json.stubs;

import ch.admin.bit.jeap.messaging.model.MessageReferences;
import ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData;
import ch.admin.bit.jeap.processcontext.plugin.api.event.ReferenceExtractor;

import java.util.Set;

public class TestReferenceExtractor implements ReferenceExtractor<MessageReferences> {

    @Override
    public Set<MessageData> getMessageData(MessageReferences references) {
        return Set.of(new MessageData("myKey", "myValue"));
    }
}
