package ch.admin.bit.jeap.processcontext.adapter.kafka.internalevent.consumer;

import ch.admin.bit.jeap.processcontext.adapter.kafka.TopicConfiguration;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceService;
import ch.admin.bit.jeap.processcontext.internal.event.outdated.ProcessContextOutdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import static ch.admin.bit.jeap.processcontext.adapter.kafka.internalevent.consumer.InternalMessageUtil.handleAndAcknowledge;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessContextOutdatedEventConsumer {
    private final ProcessInstanceService processInstanceService;

    @KafkaListener(groupId = "${spring.application.name}-event-received",
            topics = TopicConfiguration.PROCESS_OUTDATED_TOPIC_NAME)
    public void consumeProcessContextUpdatedEvent(final ProcessContextOutdatedEvent event, Acknowledgment ack) {
        String originProcessId = event.getProcessId();
        handleAndAcknowledge(originProcessId, ack, this::updateProcessState);
    }

    private void updateProcessState(String originProcessId) {
        log.debug("Received process update message for process ID {}", originProcessId);
        processInstanceService.updateProcessState(originProcessId);
    }
}
