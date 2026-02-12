package ch.admin.bit.jeap.processcontext.adapter.kafka.internalevent.consumer;

import ch.admin.bit.jeap.processcontext.adapter.kafka.TopicConfiguration;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceService;
import ch.admin.bit.jeap.processcontext.internal.event.outdated.ProcessContextOutdatedEvent;
import ch.admin.bit.jeap.processcontext.internal.event.outdated.ReceivedMessage;
import ch.admin.bit.jeap.processcontext.internal.event.outdated.ReceivedProcessCreationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessContextOutdatedEventConsumer {
    private final ProcessInstanceService processInstanceService;

    @KafkaListener(groupId = "${spring.application.name}-event-received",
            topics = TopicConfiguration.PROCESS_OUTDATED_TOPIC_NAME)
    public void consumeProcessContextUpdatedEvent(ProcessContextOutdatedEvent event, Acknowledgment ack) {
        try {
            handleEvent(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.warn("Exception while handling internal message", e);
            throw InternalMessageConsumerException.from(e);
        }
    }

    private void handleEvent(ProcessContextOutdatedEvent event) {
        switch (event.getPayload().getProcessUpdateType()) {
            case MIGRATION_TRIGGERED -> triggerMigration(event.getProcessId());
            case MESSAGE_RECEIVED -> updateProcessState(event.getProcessId(), event.getPayload().getReceivedMessage());
            case PROCESS_CREATION_MESSAGE_RECEIVED ->
                    updateProcessStateCreatingProcess(event.getProcessId(), event.getPayload().getReceivedProcessCreationMessage());
            default ->
                    log.warn("Received process context outdated event with unknown process update type: {}", event.getPayload().getProcessUpdateType());
        }
    }

    private void updateProcessState(String originProcessId, ReceivedMessage receivedMessage) {
        log.debug("Received process update message for process ID {}", originProcessId);
        processInstanceService.handleMessage(originProcessId, receivedMessage.getMessageId());
    }

    private void updateProcessStateCreatingProcess(String originProcessId, ReceivedProcessCreationMessage receivedMessage) {
        log.debug("Received process creating message for process ID {}", originProcessId);
        processInstanceService.handleMessage(originProcessId, receivedMessage.getMessageId(), receivedMessage.getTemplateName());
    }

    private void triggerMigration(String originProcessId) {
        log.debug("Triggering migration for process ID {}", originProcessId);
        processInstanceService.migrateProcessInstanceTemplate(originProcessId);
    }
}
