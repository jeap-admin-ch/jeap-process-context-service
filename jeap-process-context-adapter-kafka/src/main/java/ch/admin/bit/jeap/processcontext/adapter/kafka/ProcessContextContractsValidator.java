package ch.admin.bit.jeap.processcontext.adapter.kafka;

import ch.admin.bit.jeap.messaging.kafka.contract.ContractsProvider;
import ch.admin.bit.jeap.messaging.kafka.contract.DefaultContractsValidator;
import ch.admin.bit.jeap.messaging.model.MessageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * The shared events are not yet in the event registry. Therefore, we need a specialized contracts provider
 * to check which events can be sent and received
 */
@Component
@Slf4j
public class ProcessContextContractsValidator extends DefaultContractsValidator {
    private static final Collection<String> SHARED_EVENTS_ALLOWED_TO_PUBLISH = List.of(
            "ProcessContextOutdatedEvent",
            "ProcessContextStateChangedEvent",
            "MessageProcessingFailedEvent",
            "ProcessInstanceCreatedEvent",
            "ProcessMilestoneReachedEvent",
            "ProcessInstanceCompletedEvent",
            "ProcessSnapshotCreatedEvent");

    public ProcessContextContractsValidator(@Value("${spring.application.name}") String appName, ContractsProvider contractsProvider) {
        super(appName, contractsProvider);
    }

    @Override
    public void ensurePublisherContract(MessageType type, String topic) {
        if (SHARED_EVENTS_ALLOWED_TO_PUBLISH.contains(type.getName())) {
            return;
        }
        super.ensurePublisherContract(type, topic);
    }

    @Override
    public void ensureConsumerContract(MessageType domainEventType, String topic) {
        if (SHARED_EVENTS_ALLOWED_TO_PUBLISH.contains(domainEventType.getName())) {
            return;
        }
        super.ensureConsumerContract(domainEventType, topic);
    }
}
