package ch.admin.bit.jeap.processcontext.testevent;

import ch.admin.bit.jeap.messaging.model.MessageReferences;
import ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData;
import ch.admin.bit.jeap.processcontext.plugin.api.event.ReferenceExtractor;

import java.util.Set;

public class ProcessDataCorrelationEventDataReferenceExtractor implements ReferenceExtractor<MessageReferences> {
    @Override
    public Set<MessageData> getMessageData(MessageReferences references) {
        return Set.of(MessageData.builder()
                .key("correlationEventDataKey")
                .value("correlationEventDataValue")
                .role("correlationEventDataRole")
                .build());
    }
}
