package ch.admin.bit.jeap.processcontext.adapter.kafka.internalevent.producer;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.processcontext.adapter.kafka.KafkaProducerException;
import ch.admin.bit.jeap.processcontext.adapter.kafka.TopicConfiguration;
import ch.admin.bit.jeap.processcontext.internal.event.key.ProcessContextProcessIdKey;
import ch.admin.bit.jeap.processcontext.internal.event.outdated.ProcessContextOutdatedEvent;
import org.apache.avro.specific.SpecificRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalKafkaMessageProducerTest {

    private static final String TOPIC_NAME = "process-outdated-internal-topic";
    private static final String PROCESS_ID = "test-process-id";
    private static final String EVENT_NAME = "event";
    private static final UUID MESSSAGE_ID = UUID.randomUUID();
    private static final String IDEMPOTENCE_ID = "1234";

    @Mock
    private KafkaTemplate<SpecificRecord, AvroMessage> internalKafkaTemplate;

    @Mock
    private TopicConfiguration topicConfiguration;

    @Mock
    private InternalMessageFactory internalMessageFactory;

    @Mock
    private ProcessContextProcessIdKey processIdKey;

    @Mock
    private ProcessContextOutdatedEvent outdatedEvent;

    private InternalKafkaMessageProducer producer;

    @BeforeEach
    void setUp() {
        producer = new InternalKafkaMessageProducer(internalKafkaTemplate, topicConfiguration, internalMessageFactory);
    }

    @Test
    void produceProcessContextOutdatedEventSynchronously_shouldSendEventToKafka() {
        when(topicConfiguration.getProcessOutdatedInternal()).thenReturn(TOPIC_NAME);
        when(internalMessageFactory.key(PROCESS_ID)).thenReturn(processIdKey);
        when(internalMessageFactory.processContextOutdatedEvent(PROCESS_ID, MESSSAGE_ID, EVENT_NAME, IDEMPOTENCE_ID)).thenReturn(outdatedEvent);
        when(internalKafkaTemplate.send(TOPIC_NAME, processIdKey, outdatedEvent))
                .thenReturn(CompletableFuture.completedFuture(null));

        producer.produceProcessContextOutdatedEventSynchronously(PROCESS_ID, MESSSAGE_ID, EVENT_NAME, IDEMPOTENCE_ID);

        verify(internalKafkaTemplate).send(TOPIC_NAME, processIdKey, outdatedEvent);
    }

    @Test
    void produceProcessContextOutdatedEventSynchronously_whenInterrupted_shouldThrowKafkaProducerException() {
        when(topicConfiguration.getProcessOutdatedInternal()).thenReturn(TOPIC_NAME);
        when(internalMessageFactory.key(PROCESS_ID)).thenReturn(processIdKey);
        when(internalMessageFactory.processContextOutdatedEvent(PROCESS_ID, MESSSAGE_ID, EVENT_NAME, IDEMPOTENCE_ID)).thenReturn(outdatedEvent);

        CompletableFuture<Object> neverCompletingFuture = new CompletableFuture<>();
        when(internalKafkaTemplate.send(eq(TOPIC_NAME), any(), any()))
                .thenAnswer(invocation -> {
                    Thread.currentThread().interrupt();
                    return neverCompletingFuture;
                });

        assertThatThrownBy(() -> producer.produceProcessContextOutdatedEventSynchronously(PROCESS_ID, MESSSAGE_ID, EVENT_NAME, IDEMPOTENCE_ID))
                .isInstanceOf(KafkaProducerException.class)
                .hasMessageContaining("Cannot send event")
                .hasCauseInstanceOf(InterruptedException.class);
    }

    @Test
    void produceProcessContextOutdatedEventSynchronously_whenExecutionFails_shouldThrowKafkaProducerException() {
        when(topicConfiguration.getProcessOutdatedInternal()).thenReturn(TOPIC_NAME);
        when(internalMessageFactory.key(PROCESS_ID)).thenReturn(processIdKey);
        when(internalMessageFactory.processContextOutdatedEvent(PROCESS_ID, MESSSAGE_ID, EVENT_NAME, IDEMPOTENCE_ID)).thenReturn(outdatedEvent);

        RuntimeException cause = new RuntimeException("Kafka failure");
        when(internalKafkaTemplate.send(eq(TOPIC_NAME), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(cause));

        assertThatThrownBy(() -> producer.produceProcessContextOutdatedEventSynchronously(PROCESS_ID, MESSSAGE_ID, EVENT_NAME, IDEMPOTENCE_ID))
                .isInstanceOf(KafkaProducerException.class)
                .hasMessageContaining("Cannot send event")
                .hasCauseInstanceOf(ExecutionException.class);
    }
}
