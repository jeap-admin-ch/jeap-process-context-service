package ch.admin.bit.jeap.test.processcontext;

import ch.admin.bit.jeap.messaging.avro.errorevent.MessageProcessingFailedEvent;
import ch.admin.bit.jeap.processcontext.EventListenerStub;
import ch.admin.bit.jeap.processcontext.RelationListenerStub;
import ch.admin.bit.jeap.processcontext.adapter.kafka.message.consumer.KafkaMessageConsumerFactory;
import ch.admin.bit.jeap.processcontext.event.process.snapshot.created.ProcessSnapshotCreatedEvent;
import ch.admin.bit.jeap.processcontext.plugin.api.relation.RelationListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.test.utils.ContainerTestUtils;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ComponentScan({"ch.admin.bit.jeap.processcontext"})
public class KafkaIntegrationTestConfig {

    public static class TestMessageProcessingFailedEventListener {
        public final List<MessageProcessingFailedEvent> messageProcessingFailedEvents = new ArrayList<>();

        @KafkaListener(topics = "errorTopic")
        public void onMessageProcessingFailedEvent(MessageProcessingFailedEvent messageProcessingFailedEvent) {
            messageProcessingFailedEvents.add(messageProcessingFailedEvent);
        }
    }

    @Bean
    TestMessageProcessingFailedEventListener testMessageProcessingFailedEventListener() {
        return new TestMessageProcessingFailedEventListener();
    }

    @Bean
    RelationListener relationListener() {
        return new RelationListenerStub();
    }

    @Bean
    EventListenerStub<ProcessSnapshotCreatedEvent> processSnapshotCreatedEventListener() {
        return new EventListenerStub<>();
    }

    /**
     * Waits for the programmatically created Kafka listener containers to be assigned partitions. This ensures that
     * tests do not start sending messages before the listeners are ready to receive them.
     */
    @Bean
    ApplicationListener<ApplicationReadyEvent> readyListener(KafkaMessageConsumerFactory kafkaMessageConsumerFactory) {
        return ignored -> kafkaMessageConsumerFactory.getContainers().forEach(c ->
                ContainerTestUtils.waitForAssignment(c, 1));
    }
}
