package ch.admin.bit.jeap.processcontext.adapter.kafka.processevent.producer;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEvent;
import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.processcontext.adapter.kafka.TopicConfiguration;
import ch.admin.bit.jeap.processcontext.domain.port.ProcessInstanceEventProducer;
import ch.admin.bit.jeap.processcontext.event.ProcessInstanceCompletedEventBuilder;
import ch.admin.bit.jeap.processcontext.event.ProcessInstanceCreatedEventBuilder;
import ch.admin.bit.jeap.processcontext.event.ProcessMilestoneReachedEventBuilder;
import ch.admin.bit.jeap.processcontext.event.ProcessSnapshotCreatedEventBuilder;
import ch.admin.bit.jeap.processcontext.event.process.instance.completed.ProcessInstanceCompletedEvent;
import ch.admin.bit.jeap.processcontext.event.process.instance.created.ProcessInstanceCreatedEvent;
import ch.admin.bit.jeap.processcontext.event.process.milestone.reached.ProcessMilestoneReachedEvent;
import ch.admin.bit.jeap.processcontext.event.process.snapshot.created.ProcessSnapshotCreatedEvent;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
@RequiredArgsConstructor
public class DomainEventProcessInstanceEventProducer implements ProcessInstanceEventProducer {

    private static final String CREATED_EVENT_IDEMPOTENCE_SUFFIX = "-created";
    private static final String COMPLETED_EVENT_IDEMPOTENCE_SUFFIX = "-completed";
    private static final String MILESTONE_REACHED_EVENT_IDEMPOTENCE_INFIX = "-milestone-reached-";
    private static final String SNAPSHOT_CREATED_EVENT_IDEMPOTENCE_INFIX = "-snapshot-created-";

    private final TopicConfiguration topicConfiguration;
    private final KafkaProperties kafkaProperties;
    private final KafkaTemplate<AvroMessageKey, AvroMessage> kafkaTemplate;

    @Override
    @Timed(value = "jeap_pcs_produce_process_instance_created_event", percentiles = {0.5, 0.8, 0.95, 0.99})
    public void produceProcessInstanceCreatedEventSynchronously(String originProcessId, String processName) {
        ProcessInstanceCreatedEvent createdEvent = ProcessInstanceCreatedEventBuilder.create()
                .idempotenceId(originProcessId + CREATED_EVENT_IDEMPOTENCE_SUFFIX)
                .processId(originProcessId)
                .processName(processName)
                .systemName(kafkaProperties.getSystemName())
                .serviceName(kafkaProperties.getServiceName())
                .build();

        sendEventSynchronously(createdEvent, topicConfiguration.getProcessInstanceCreated());
    }

    @Override
    @Timed(value = "jeap_pcs_produce_process_instance_completed_event", percentiles = {0.5, 0.8, 0.95, 0.99})
    public void produceProcessInstanceCompletedEventSynchronously(String originProcessId) {
        ProcessInstanceCompletedEvent completedEvent = ProcessInstanceCompletedEventBuilder.create()
                .idempotenceId(originProcessId + COMPLETED_EVENT_IDEMPOTENCE_SUFFIX)
                .processId(originProcessId)
                .systemName(kafkaProperties.getSystemName())
                .serviceName(kafkaProperties.getServiceName())
                .build();

        sendEventSynchronously(completedEvent, topicConfiguration.getProcessInstanceCompleted());
    }

    @Override
    @Timed(value = "jeap_pcs_produce_process_milestone_reached_event", percentiles = {0.5, 0.8, 0.95, 0.99})
    public void produceProcessMilestoneReachedEventSynchronously(String originProcessId, String milestoneName) {
        ProcessMilestoneReachedEvent milestoneReachedEvent = ProcessMilestoneReachedEventBuilder.create()
                .idempotenceId(originProcessId + MILESTONE_REACHED_EVENT_IDEMPOTENCE_INFIX + milestoneName)
                .processId(originProcessId)
                .systemName(kafkaProperties.getSystemName())
                .serviceName(kafkaProperties.getServiceName())
                .milestoneName(milestoneName)
                .build();

        sendEventSynchronously(milestoneReachedEvent, topicConfiguration.getProcessMilestoneReached());
    }

    @Override
    @Timed(value = "jeap_pcs_produce_process_snapshot_created_event", percentiles = {0.5, 0.8, 0.95, 0.99})
    public void produceProcessSnapshotCreatedEventSynchronously(String originProcessId, int snapshotVersion) {
        ProcessSnapshotCreatedEvent snapshotCreatedEvent = ProcessSnapshotCreatedEventBuilder.create()
                .idempotenceId(originProcessId + SNAPSHOT_CREATED_EVENT_IDEMPOTENCE_INFIX + snapshotVersion)
                .processId(originProcessId)
                .systemName(kafkaProperties.getSystemName())
                .serviceName(kafkaProperties.getServiceName())
                .snapshotVersion(snapshotVersion)
                .build();

        sendEventSynchronously(snapshotCreatedEvent, topicConfiguration.getProcessSnapshotCreated());
    }

    private void sendEventSynchronously(AvroDomainEvent event, String topic) {
        try {
            // We add the 'get()' here to make sure an exception is thrown synchronously if the sending fails.
            kafkaTemplate.send(topic, event).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Sending of " + getEventName(event) + " has been interrupted.", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Sending of " + getEventName(event) + " failed with an exception.", e);
        }
    }

    private static String getEventName(AvroDomainEvent event) {
        return event.getType().getName();
    }
}
