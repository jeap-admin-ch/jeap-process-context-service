package ch.admin.bit.jeap.processcontext.testevent;

import ch.admin.bit.jeap.processcontext.event.test1.Test1EventPayload;
import ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData;
import ch.admin.bit.jeap.processcontext.plugin.api.event.PayloadExtractor;

import java.util.HashSet;
import java.util.Set;

import static org.springframework.util.StringUtils.hasText;

public class Test1EventPayloadExtractor implements PayloadExtractor<Test1EventPayload> {

    @Override
    public Set<MessageData> getMessageData(Test1EventPayload payload) {
        HashSet<MessageData> set = new HashSet<>();
        if (!payload.getTaskIds().isEmpty()) {
            set.add(new MessageData("key1", payload.getTaskIds().get(0)));
        }
        if (hasText(payload.getSomeField()) ) {
            set.add(new MessageData("someField", payload.getSomeField()));
        }
        return set;
    }
}
