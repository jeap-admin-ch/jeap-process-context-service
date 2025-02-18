package ch.admin.bit.jeap.test.processcontext;

import ch.admin.bit.jeap.messaging.avro.errorevent.MessageProcessingFailedEvent;
import ch.admin.bit.jeap.processcontext.EventListenerStub;
import ch.admin.bit.jeap.processcontext.RelationListenerStub;
import ch.admin.bit.jeap.processcontext.event.process.instance.completed.ProcessInstanceCompletedEvent;
import ch.admin.bit.jeap.processcontext.event.process.instance.created.ProcessInstanceCreatedEvent;
import ch.admin.bit.jeap.processcontext.event.process.milestone.reached.ProcessMilestoneReachedEvent;
import ch.admin.bit.jeap.processcontext.event.process.snapshot.created.ProcessSnapshotCreatedEvent;
import ch.admin.bit.jeap.processcontext.plugin.api.relation.RelationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;

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
    EventListenerStub<ProcessMilestoneReachedEvent> processMilestoneReachedEventListener() {
        return new EventListenerStub<>();
    }

    @Bean
    EventListenerStub<ProcessInstanceCompletedEvent> processInstanceCompletedEventListener() {
        return new EventListenerStub<>();
    }

    @Bean
    EventListenerStub<ProcessInstanceCreatedEvent> processInstanceCreatedEventListener() {
        return new EventListenerStub<>();
    }

    @Bean
    EventListenerStub<ProcessSnapshotCreatedEvent> processSnapshotCreatedEventListener() {
        return new EventListenerStub<>();
    }
}
