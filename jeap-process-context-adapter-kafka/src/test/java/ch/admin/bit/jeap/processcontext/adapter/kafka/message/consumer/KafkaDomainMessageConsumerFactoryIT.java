package ch.admin.bit.jeap.processcontext.adapter.kafka.message.consumer;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.processcontext.adapter.kafka.KafkaAdapterIntegrationTestBase;
import ch.admin.bit.jeap.processcontext.event.ProcessInstanceCreatedEventBuilder;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class KafkaDomainMessageConsumerFactoryIT extends KafkaAdapterIntegrationTestBase {

    private static final String TOPIC_NAME = "my-custom-topic";

    @Autowired
    private KafkaMessageConsumerFactory kafkaDomainEventConsumerFactory;

    @Test
    void startConsumer() {
        AtomicReference<AvroMessage> receivedEvent = new AtomicReference<>();
        AcknowledgingMessageListener<AvroMessageKey, AvroMessage> listener = (data, ack) -> {
            assertNull(receivedEvent.get());
            assertNotNull(ack);
            receivedEvent.set(data.value());
            ack.acknowledge();
        };
        AvroMessage event = createDomainEvent();

        // Start consumer
        ConcurrentMessageListenerContainer<AvroMessageKey, AvroMessage> container =
                ReflectionTestUtils.invokeMethod(kafkaDomainEventConsumerFactory, "startConsumer", TOPIC_NAME, event.getType().getName(), null, listener);

        // wait for listener to be active
        assertThat(container).isNotNull();
        ContainerTestUtils.waitForAssignment(container, 1);
        assertEquals("jeap-process-context-scs_my-custom-topic_ProcessInstanceCreatedEvent", container.getGroupId());

        // publish event
        kafkaTemplate.send(TOPIC_NAME, event);

        // expect consumer to receive event
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .until(() -> receivedEvent.get() != null);
        assertEquals(event.getIdentity().getId(), receivedEvent.get().getIdentity().getId());
    }

    private static AvroMessage createDomainEvent() {
        return ProcessInstanceCreatedEventBuilder.create()
                .processName("process")
                .systemName("TEST")
                .serviceName("service")
                .processId("123")
                .idempotenceId("idempotenceId1")
                .build();
    }
}
