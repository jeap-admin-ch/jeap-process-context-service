package ch.admin.bit.jeap.processcontext.testevent;

import ch.admin.bit.jeap.processcontext.event.test1.Test1EventPayload;
import ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData;
import ch.admin.bit.jeap.processcontext.plugin.api.event.PayloadExtractor;

import java.util.Set;

import static java.util.Collections.emptySet;

public class Test1EventDifferentPayloadExtractor implements PayloadExtractor<Test1EventPayload> {

    @Override
    public Set<MessageData> getMessageData(Test1EventPayload payload) {
        if (!payload.getTaskIds().isEmpty()) {
            return Set.of(new MessageData("differentKey1", payload.getTaskIds().get(0)));
        } else {
            return emptySet();
        }
    }
}
