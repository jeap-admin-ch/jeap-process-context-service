package ch.admin.bit.jeap.processcontext.testevent;

import ch.admin.bit.jeap.processcontext.event.test1.Test1EventReferences;
import ch.admin.bit.jeap.processcontext.event.test2.Test2EventPayload;
import ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData;
import ch.admin.bit.jeap.processcontext.plugin.api.event.PayloadExtractor;
import ch.admin.bit.jeap.processcontext.plugin.api.event.ReferenceExtractor;

import java.util.Set;

import static java.util.Collections.emptySet;

public class RelationTestEventExtractor implements PayloadExtractor<Test2EventPayload>, ReferenceExtractor<Test1EventReferences> {
    @Override
    public Set<MessageData> getMessageData(Test1EventReferences references) {
        if (references.getSubjectReference() != null) {
            return Set.of(MessageData.builder()
                    .key("theSubject")
                    .value(references.getSubjectReference().getSubjectId())
                    .role(references.getSubjectReference().getVersion())
                    .build());
        } else {
            return emptySet();
        }
    }

    @Override
    public Set<MessageData> getMessageData(Test2EventPayload payload) {
        if (payload.getObjectId() != null) {
            return Set.of(MessageData.builder()
                    .key("theObject")
                    .value(payload.getObjectId())
                    .role(payload.getVersion())
                    .build());
        } else {
            return emptySet();
        }
    }
}
