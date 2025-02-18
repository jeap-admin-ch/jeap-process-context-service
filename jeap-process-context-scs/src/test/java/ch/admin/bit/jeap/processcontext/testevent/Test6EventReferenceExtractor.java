package ch.admin.bit.jeap.processcontext.testevent;

import ch.admin.bit.jeap.processcontext.event.test6.Test6EventReferences;
import ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData;
import ch.admin.bit.jeap.processcontext.plugin.api.event.ReferenceExtractor;

import java.util.Set;

public class Test6EventReferenceExtractor implements ReferenceExtractor<Test6EventReferences> {

    @Override
    public Set<MessageData> getMessageData(Test6EventReferences references) {
        String processId = references.getProcessReference().getProcessId();
        return Set.of(MessageData.builder()
                .key("relatedProcessId")
                .value(processId)
                .build());
    }

}
