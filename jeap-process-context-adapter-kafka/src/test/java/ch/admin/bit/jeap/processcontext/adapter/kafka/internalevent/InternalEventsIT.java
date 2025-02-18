package ch.admin.bit.jeap.processcontext.adapter.kafka.internalevent;

import ch.admin.bit.jeap.processcontext.adapter.kafka.KafkaAdapterIntegrationTestBase;
import ch.admin.bit.jeap.processcontext.domain.port.InternalMessageProducer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.*;

class InternalEventsIT extends KafkaAdapterIntegrationTestBase {

    @Autowired
    private InternalMessageProducer eventProducer;
    @MockitoBean
    private CommonErrorHandler errorHandler;

    @Test
    void produceAndConsumeProcessContextOutdatedEvent_internalMessageAsDomainEvent() {
        eventProducer.produceProcessContextOutdatedEventSynchronously("1234");
        verify(processInstanceService, timeout(TEST_TIMEOUT)).updateProcessState("1234");
        verifyNoErrorHandlingInteractions(errorHandler);
    }

    @Test
    void produceAndConsumeProcessContextStateChangedEvent_internalMessageAsDomainEvent() {
        eventProducer.produceProcessContextStateChangedEventSynchronously("1234");
        verify(processEventService, timeout(TEST_TIMEOUT)).reactToProcessStateChange("1234");
        verifyNoErrorHandlingInteractions(errorHandler);
    }

    private void verifyNoErrorHandlingInteractions(CommonErrorHandler errorHandlerMock) {
        verify(errorHandlerMock, never()).handleOne(any(), any(), any(), any());
        verify(errorHandlerMock, never()).handleBatch(any(), any(), any(), any(), any());
        verify(errorHandlerMock, never()).handleRemaining(any(), any(), any(), any());
        verify(errorHandlerMock, never()).handleOtherException(any(), any(), any(), anyBoolean());
    }
}


