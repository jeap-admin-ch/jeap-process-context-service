package ch.admin.bit.jeap.processcontext.domain.processtemplate;

import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.messaging.model.MessagePayload;
import ch.admin.bit.jeap.messaging.model.MessageReferences;
import ch.admin.bit.jeap.processcontext.plugin.api.event.MessageCorrelationProvider;
import ch.admin.bit.jeap.processcontext.plugin.api.event.PayloadExtractor;
import ch.admin.bit.jeap.processcontext.plugin.api.event.ProcessInstantiationCondition;
import ch.admin.bit.jeap.processcontext.plugin.api.event.ReferenceExtractor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
public class MessageReference {
    String messageName;
    String topicName;
    String clusterName;
    MessageCorrelationProvider<Message> correlationProvider;
    PayloadExtractor<MessagePayload> payloadExtractor;
    ReferenceExtractor<MessageReferences> referenceExtractor;
    ProcessInstantiationCondition<Message> processInstantiationCondition;
    CorrelatedByProcessData correlatedByProcessData;

    @Builder
    @SuppressWarnings("java:S107")
    public MessageReference(@NonNull String messageName,
                            @NonNull String topicName,
                            String clusterName,
                            @NonNull MessageCorrelationProvider<Message> correlationProvider,
                            @NonNull PayloadExtractor<MessagePayload> payloadExtractor,
                            @NonNull ReferenceExtractor<MessageReferences> referenceExtractor,
                            @NonNull ProcessInstantiationCondition<Message> processInstantiationCondition,
                            CorrelatedByProcessData correlatedByProcessData) {
        this.messageName = messageName;
        this.topicName = topicName;
        this.clusterName = clusterName;
        this.correlationProvider = correlationProvider;
        this.payloadExtractor = payloadExtractor;
        this.referenceExtractor = referenceExtractor;
        this.correlatedByProcessData = correlatedByProcessData;
        this.processInstantiationCondition = processInstantiationCondition;
    }

}
