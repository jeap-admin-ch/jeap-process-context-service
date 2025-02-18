package ch.admin.bit.jeap.processcontext.adapter.kafka.internalevent.consumer;

import ch.admin.bit.jeap.processcontext.adapter.kafka.TopicConfiguration;
import ch.admin.bit.jeap.processcontext.domain.processevent.ProcessEventService;
import ch.admin.bit.jeap.processcontext.internal.event.statechanged.ProcessContextStateChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import static ch.admin.bit.jeap.processcontext.adapter.kafka.internalevent.consumer.InternalMessageUtil.handleAndAcknowledge;

@Component
@RequiredArgsConstructor
@Slf4j
@KafkaListener(groupId = "${spring.application.name}-state-changed",
        topics = TopicConfiguration.PROCESS_STATE_CHANGED_TOPIC_NAME)
public class ProcessContextStateChangedEventConsumer {
    private final ProcessEventService processEventService;

    @KafkaHandler
    public void consumeProcessStateChangedMessage(ProcessContextStateChangedEvent event, Acknowledgment ack) {
        String originProcessId = event.getProcessId();
        handleAndAcknowledge(originProcessId, ack, this::reactToStateChange);
    }

    private void reactToStateChange(String originProcessId) {
        log.debug("Received process state changed message for process ID {}", originProcessId);
        processEventService.reactToProcessStateChange(originProcessId);
    }
}
