package ch.admin.bit.jeap.processcontext.adapter.kafka.message.consumer;

import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.processcontext.adapter.test.kafka.config.TestApp;
import ch.admin.bit.jeap.processcontext.domain.message.MessageReceiver;
import ch.admin.bit.jeap.processcontext.domain.processevent.ProcessEventService;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceService;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ActiveProfiles("local")
@SpringBootTest(
        properties = {
                "jeap.processcontext.kafka.message-consumer-paused=true",
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
class KafkaMessageConsumerFactoryPausedIT extends KafkaIntegrationTestBase {

    static final String TEST_DOMAIN_EVENT_TOPIC = "test-domain-event-topic";
    @Autowired
    private KafkaMessageConsumerFactory kafkaMessageConsumerFactory;
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
                .forEach(c -> ContainerTestUtils.waitForAssignment(c, 1));
    }

    @Test
    void whenEventReceivedConsumerIsNotAutoStarted_makeSureItIsPaused() {
        // Given: Start domain event consumers with paused flag
        AtomicBoolean messageReceived = new AtomicBoolean(false);
        MessageReceiver recv = message -> messageReceived.set(true);
        kafkaMessageConsumerFactory.startConsumer(TEST_DOMAIN_EVENT_TOPIC, "message1", null, recv);

        // When: Produce a message on the topic where no consumer should be active
        sendSync(TEST_DOMAIN_EVENT_TOPIC, KafkaMessageConsumerFactoryIT.createDomainEvent());

        // Then: Make sure the message is not received, and the consumer is not active
        assertThat(messageReceived)
                .isFalse();
        boolean containerForTestTopicPresent = registry.getListenerContainers().stream().anyMatch(c -> Objects.requireNonNull(c.getGroupId()).contains(TEST_DOMAIN_EVENT_TOPIC));
        assertThat(containerForTestTopicPresent)
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
