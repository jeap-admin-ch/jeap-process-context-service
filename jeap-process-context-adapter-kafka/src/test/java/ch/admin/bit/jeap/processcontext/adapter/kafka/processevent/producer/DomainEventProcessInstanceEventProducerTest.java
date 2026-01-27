package ch.admin.bit.jeap.processcontext.adapter.kafka.processevent.producer;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.processcontext.adapter.kafka.KafkaProducerException;
import ch.admin.bit.jeap.processcontext.adapter.kafka.TopicConfiguration;
import ch.admin.bit.jeap.processcontext.event.process.snapshot.created.ProcessSnapshotCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DomainEventProcessInstanceEventProducerTest {

    private static final String TOPIC_NAME = "process-snapshot-created-topic";
    private static final String SYSTEM_NAME = "test-system";
    private static final String SERVICE_NAME = "test-service";
    private static final String PROCESS_ID = "test-process-id";
    private static final int SNAPSHOT_VERSION = 5;

    @Mock
    private TopicConfiguration topicConfiguration;

    @Mock
    private KafkaProperties kafkaProperties;

    @Mock
    private KafkaTemplate<AvroMessageKey, AvroMessage> kafkaTemplate;

    private DomainEventProcessInstanceEventProducer producer;

    @BeforeEach
    void setUp() {
        producer = new DomainEventProcessInstanceEventProducer(topicConfiguration, kafkaProperties, kafkaTemplate);
    }

    @Test
    void produceProcessSnapshotCreatedEventSynchronously_shouldSendEventToKafka() {
        when(topicConfiguration.getProcessSnapshotCreated()).thenReturn(TOPIC_NAME);
        when(kafkaProperties.getSystemName()).thenReturn(SYSTEM_NAME);
        when(kafkaProperties.getServiceName()).thenReturn(SERVICE_NAME);
        when(kafkaTemplate.send(eq(TOPIC_NAME), any(ProcessSnapshotCreatedEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        producer.produceProcessSnapshotCreatedEventSynchronously(PROCESS_ID, SNAPSHOT_VERSION);

        ArgumentCaptor<ProcessSnapshotCreatedEvent> eventCaptor = ArgumentCaptor.forClass(ProcessSnapshotCreatedEvent.class);
        verify(kafkaTemplate).send(eq(TOPIC_NAME), eventCaptor.capture());

        ProcessSnapshotCreatedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getProcessId()).isEqualTo(PROCESS_ID);
        assertThat(capturedEvent.getReferences().getReference().getSnapshotVersion()).isEqualTo(SNAPSHOT_VERSION);
        assertThat(capturedEvent.getIdentity().getIdempotenceId()).isEqualTo(PROCESS_ID + "-snapshot-created-" + SNAPSHOT_VERSION);
    }

    @Test
    void produceProcessSnapshotCreatedEventSynchronously_whenInterrupted_shouldThrowKafkaProducerException() {
        when(topicConfiguration.getProcessSnapshotCreated()).thenReturn(TOPIC_NAME);
        when(kafkaProperties.getSystemName()).thenReturn(SYSTEM_NAME);
        when(kafkaProperties.getServiceName()).thenReturn(SERVICE_NAME);

        CompletableFuture<Object> neverCompletingFuture = new CompletableFuture<>();
        when(kafkaTemplate.send(eq(TOPIC_NAME), any(ProcessSnapshotCreatedEvent.class)))
                .thenAnswer(invocation -> {
                    Thread.currentThread().interrupt();
                    return neverCompletingFuture;
                });

        assertThatThrownBy(() -> producer.produceProcessSnapshotCreatedEventSynchronously(PROCESS_ID, SNAPSHOT_VERSION))
                .isInstanceOf(KafkaProducerException.class)
                .hasMessageContaining("has been interrupted")
                .hasCauseInstanceOf(InterruptedException.class);
    }

    @Test
    void produceProcessSnapshotCreatedEventSynchronously_whenExecutionFails_shouldThrowKafkaProducerException() {
        when(topicConfiguration.getProcessSnapshotCreated()).thenReturn(TOPIC_NAME);
        when(kafkaProperties.getSystemName()).thenReturn(SYSTEM_NAME);
        when(kafkaProperties.getServiceName()).thenReturn(SERVICE_NAME);

        RuntimeException cause = new RuntimeException("Kafka failure");
        when(kafkaTemplate.send(eq(TOPIC_NAME), any(ProcessSnapshotCreatedEvent.class)))
                .thenReturn(CompletableFuture.failedFuture(cause));

        assertThatThrownBy(() -> producer.produceProcessSnapshotCreatedEventSynchronously(PROCESS_ID, SNAPSHOT_VERSION))
                .isInstanceOf(KafkaProducerException.class)
                .hasMessageContaining("failed with an exception")
                .hasCauseInstanceOf(ExecutionException.class);
    }
}
