package ch.admin.bit.jeap.processcontext.domain.processupdate;

import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.port.InternalMessageProducer;
import com.fasterxml.uuid.Generators;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessUpdateServiceTest {
    private static final String ORIGIN_PROCESS_ID = "originProcessId";
    private static final String EVENT_NAME = "eventName";
    private static final String IDEMPOTENCE_ID = "idempotenceId";
    private static final String TEMPLATE_NAME = "templateName";
    private static final UUID EVENT_REFERENCE = Generators.timeBasedEpochGenerator().generate();

    @Mock
    InternalMessageProducer eventProducer;

    @Mock
    Message message;

    private ProcessUpdateService service;

    @BeforeEach
    void setUp() {
        service = new ProcessUpdateService(eventProducer);
    }

    @Test
    void domainEventReceived() {
        mockEventIdempotenceData();
        when(message.getId()).thenReturn(EVENT_REFERENCE);
        mockEventIdempotenceData();

        service.messageReceived(ORIGIN_PROCESS_ID, message);

        verify(eventProducer).produceProcessContextOutdatedEventSynchronously(ORIGIN_PROCESS_ID, EVENT_REFERENCE, EVENT_NAME, IDEMPOTENCE_ID);
    }

    @Test
    void domainEventTriggeringProcessCreationReceived() {
        mockEventIdempotenceData();
        when(message.getId()).thenReturn(EVENT_REFERENCE);
        mockEventIdempotenceData();

        service.processCreatingMessageReceived(ORIGIN_PROCESS_ID, message, TEMPLATE_NAME);

        verify(eventProducer).produceProcessContextOutdatedCreateProcessEventSynchronously(ORIGIN_PROCESS_ID, EVENT_REFERENCE, EVENT_NAME, IDEMPOTENCE_ID, TEMPLATE_NAME);
    }

    private void mockEventIdempotenceData() {
        when(message.getMessageName()).thenReturn(EVENT_NAME);
        when(message.getIdempotenceId()).thenReturn(IDEMPOTENCE_ID);
    }
}
