package ch.admin.bit.jeap.processcontext.adapter.kafka.internalevent;

import ch.admin.bit.jeap.processcontext.adapter.test.kafka.config.TestApp;
import ch.admin.bit.jeap.processcontext.domain.port.InternalMessageProducer;
import ch.admin.bit.jeap.processcontext.domain.processevent.ProcessEventService;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceService;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import ch.admin.bit.jeap.processcontext.internal.event.outdated.ProcessContextOutdatedEvent;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@TestPropertySource(properties = "jeap.processcontext.process-updates.auto-start=false")
@EmbeddedKafka(controlledShutdown = true, partitions = 1, kraft = true)
@ActiveProfiles("local")
@SpringBootTest(
        properties = {
                "jeap.processcontext.kafka.topic.process-changed-internal=changed",
                "jeap.processcontext.kafka.topic.process-outdated-internal=outdated",
                "jeap.processcontext.kafka.topic.process-instance-created=process-instance-created",
                "jeap.processcontext.kafka.topic.process-instance-completed=process-instance-completed",
                "jeap.processcontext.kafka.topic.process-snapshot-created=process-snapshot-created",
                "jeap.processcontext.kafka.topic.create-process-instance=create-process-instance",
                "jeap.messaging.kafka.consume-without-contract-allowed=true",
                "jeap.messaging.kafka.silent-ignore-without-contract=true",
                "jeap.messaging.kafka.error-topic-name=error",
                "jeap.messaging.kafka.system-name=test",
                "jeap.messaging.kafka.service-name=test"
        },
        classes = TestApp.class)
@ExtendWith(MockitoExtension.class)
class ProcessContextOutdatedEventConsumerPausedIT {

    @Autowired
    private InternalMessageProducer eventProducer;
    @MockitoBean
    private CommonErrorHandler errorHandler;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private KafkaListenerEndpointRegistry registry;
    @MockitoBean
    protected ProcessInstanceService processInstanceService;
    @MockitoBean
    protected ProcessEventService processEventService;
    @MockitoBean
    protected ProcessTemplateRepository processTemplateRepository;

    @BeforeEach
    void waitForKafkaListener() {
        registry.getListenerContainers()
                .forEach(c -> {
                    // The ProcessContextOutdatedEventConsumer is paused, its container will not be assigned a partition
                    if (!c.getGroupId().endsWith("-event-received")) {
                        ContainerTestUtils.waitForAssignment(c, 1);
                    }
                });
    }

    private static ProcessContextOutdatedEvent receivedEvent;

    @TestConfiguration
    static class TestConfig {
        @KafkaListener(topics = "outdated", groupId = "pause-test-event-received")
        public void listen(ProcessContextOutdatedEvent event, Acknowledgment ack) {
            receivedEvent = event;
            ack.acknowledge();
        }
    }

    @Test
    void whenEventReceivedConsumerIsNotAutoStarted_makeSureItIsPaused() {
        eventProducer.produceProcessContextOutdatedEventSynchronously("1234");

        // Wait for event to be consumed by the test consumer
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .until(() -> receivedEvent != null);

        // Make sure that the event-received consumer container for ProcessContextOutdatedEventConsumer is not active
        MessageListenerContainer messageListenerContainer = registry.getListenerContainers().stream()
                .filter(c -> Objects.requireNonNull(c.getGroupId()).endsWith("-event-received"))
                .findFirst().orElseThrow();
        assertThat(messageListenerContainer.isRunning())
                .describedAs("ProcessContextOutdatedEventConsumer listener container should be paused")
                .isFalse();

        verifyNoErrorHandlingInteractions(errorHandler);
    }

    private void verifyNoErrorHandlingInteractions(CommonErrorHandler errorHandlerMock) {
        verify(errorHandlerMock, never()).handleOne(any(), any(), any(), any());
        verify(errorHandlerMock, never()).handleBatch(any(), any(), any(), any(), any());
        verify(errorHandlerMock, never()).handleRemaining(any(), any(), any(), any());
        verify(errorHandlerMock, never()).handleOtherException(any(), any(), any(), anyBoolean());
    }
}
