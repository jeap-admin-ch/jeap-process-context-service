package ch.admin.bit.jeap.processcontext.adapter.kafka.internalevent.consumer;

import ch.admin.bit.jeap.processcontext.adapter.kafka.TopicConfiguration;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceService;
import ch.admin.bit.jeap.processcontext.internal.event.outdated.ProcessContextOutdatedEvent;
import ch.admin.bit.jeap.processcontext.internal.event.outdated.ProcessContextOutdatedPayload;
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
        String originProcessId = event.getProcessId();
        if (isMigrationTriggerEvent(event)) {
            triggerMigration(originProcessId);
        } else {
            updateProcessState(originProcessId);
        }
    }

    private void updateProcessState(String originProcessId) {
        log.debug("Received process update message for process ID {}", originProcessId);
        processInstanceService.updateProcessState(originProcessId);
    }

    private void triggerMigration(String originProcessId) {
        log.debug("Triggering migration for process ID {}", originProcessId);
        processInstanceService.migrateProcessInstanceTemplate(originProcessId);
    }

    private boolean isMigrationTriggerEvent(ProcessContextOutdatedEvent event) {
        return event.getOptionalPayload()
                .map(ProcessContextOutdatedPayload::getTriggerMigration)
                .orElse(false);
    }
}
