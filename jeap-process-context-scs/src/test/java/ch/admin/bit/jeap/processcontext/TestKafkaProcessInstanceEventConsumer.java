package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.messaging.api.MessageListener;
import ch.admin.bit.jeap.processcontext.adapter.kafka.TopicConfiguration;
import ch.admin.bit.jeap.processcontext.event.process.snapshot.created.ProcessSnapshotCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TestKafkaProcessInstanceEventConsumer {

    private final List<MessageListener<ProcessSnapshotCreatedEvent>> processSnapshotCreatedEventListeners;

    @KafkaListener(topics = {TopicConfiguration.PROCESS_SNAPSHOT_CREATED_EVENT_TOPIC_NAME})
    public void consume(ProcessSnapshotCreatedEvent event, Acknowledgment ack) {
        processSnapshotCreatedEventListeners.forEach(listener -> listener.receive(event));
        ack.acknowledge();
    }
}
