package ch.admin.bit.jeap.processcontext.adapter.kafka.internalevent;

import ch.admin.bit.jeap.processcontext.adapter.kafka.KafkaAdapterIntegrationTestBase;
import ch.admin.bit.jeap.processcontext.domain.port.InternalMessageProducer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

class InternalEventsIT extends KafkaAdapterIntegrationTestBase {

    @Autowired
    private InternalMessageProducer eventProducer;
    @MockitoBean
    private CommonErrorHandler errorHandler;

    @Test
    void produceAndConsumeProcessContextOutdatedEvent() {
        UUID messageId = UUID.randomUUID();
        eventProducer.produceProcessContextOutdatedEventSynchronously("1234", messageId, "message", "456");
        verify(processInstanceService, timeout(TEST_TIMEOUT)).handleMessage("1234", messageId);
        verifyNoErrorHandlingInteractions(errorHandler);
    }

    @Test
    void produceAndConsumeProcessContextOutdatedEvent_createProcess() {
        UUID messageId = UUID.randomUUID();
        eventProducer.produceProcessContextOutdatedCreateProcessEventSynchronously("1234", messageId, "message", "456", "template");
        verify(processInstanceService, timeout(TEST_TIMEOUT)).handleMessage("1234", messageId, "template");
        verifyNoErrorHandlingInteractions(errorHandler);
    }

    @Test
    void produceAndConsumeProcessContextOutdatedEvent_triggerMigration() {
        eventProducer.produceProcessContextOutdatedMigrationTriggerEvents(List.of("1234"), "4567");
        verify(processInstanceService, timeout(TEST_TIMEOUT)).migrateProcessInstanceTemplate("1234");
        verifyNoErrorHandlingInteractions(errorHandler);
    }

    private void verifyNoErrorHandlingInteractions(CommonErrorHandler errorHandlerMock) {
        verify(errorHandlerMock, never()).handleOne(any(), any(), any(), any());
        verify(errorHandlerMock, never()).handleBatch(any(), any(), any(), any(), any());
        verify(errorHandlerMock, never()).handleRemaining(any(), any(), any(), any());
        verify(errorHandlerMock, never()).handleOtherException(any(), any(), any(), anyBoolean());
    }
}


