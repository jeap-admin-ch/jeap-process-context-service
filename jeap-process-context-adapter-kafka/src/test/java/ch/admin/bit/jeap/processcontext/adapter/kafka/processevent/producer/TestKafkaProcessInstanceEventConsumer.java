package ch.admin.bit.jeap.processcontext.adapter.kafka.processevent.producer;

import ch.admin.bit.jeap.messaging.api.MessageListener;
import ch.admin.bit.jeap.processcontext.adapter.kafka.TopicConfiguration;
import ch.admin.bit.jeap.processcontext.event.process.instance.completed.ProcessInstanceCompletedEvent;
import ch.admin.bit.jeap.processcontext.event.process.instance.created.ProcessInstanceCreatedEvent;
import ch.admin.bit.jeap.processcontext.event.process.milestone.reached.ProcessMilestoneReachedEvent;
import ch.admin.bit.jeap.processcontext.event.process.snapshot.created.ProcessSnapshotCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TestKafkaProcessInstanceEventConsumer {

    private final List<MessageListener<ProcessInstanceCreatedEvent>> processInstanceCreatedEventListeners;
    private final List<MessageListener<ProcessInstanceCompletedEvent>> processInstanceCompletedEventListeners;
    private final List<MessageListener<ProcessMilestoneReachedEvent>> processMilestoneReachedEventListeners;
    private final List<MessageListener<ProcessSnapshotCreatedEvent>> processSnapshotCreatedEventListeners;

    @KafkaListener(topics = {TopicConfiguration.PROCESS_INSTANCE_CREATED_EVENT_TOPIC_NAME})
    public void consume(final ProcessInstanceCreatedEvent event, Acknowledgment ack) {
        processInstanceCreatedEventListeners.forEach(listener -> listener.receive(event));
        ack.acknowledge();
    }

    @KafkaListener(topics = {TopicConfiguration.PROCESS_INSTANCE_COMPLETED_EVENT_TOPIC_NAME})
    public void consume(final ProcessInstanceCompletedEvent event, Acknowledgment ack) {
        processInstanceCompletedEventListeners.forEach(listener -> listener.receive(event));
        ack.acknowledge();
    }

    @KafkaListener(topics = {TopicConfiguration.PROCESS_MILESTONE_REACHED_EVENT_TOPIC_NAME})
    public void consume(final ProcessMilestoneReachedEvent event, Acknowledgment ack) {
        processMilestoneReachedEventListeners.forEach(listener -> listener.receive(event));
        ack.acknowledge();
    }

    @KafkaListener(topics = {TopicConfiguration.PROCESS_SNAPSHOT_CREATED_EVENT_TOPIC_NAME})
    public void consume(final ProcessSnapshotCreatedEvent event, Acknowledgment ack) {
        processSnapshotCreatedEventListeners.forEach(listener -> listener.receive(event));
        ack.acknowledge();
    }
}
