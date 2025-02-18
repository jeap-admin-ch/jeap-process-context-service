package ch.admin.bit.jeap.processcontext.domain.processupdate;

import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.port.InternalMessageProducer;
import com.fasterxml.uuid.Generators;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessUpdateServiceTest {
    private static final String ORIGIN_PROCESS_ID = "originProcessId";
    private static final String EVENT_NAME = "eventName";
    private static final String IDEMPOTENCE_ID = "idempotenceId";
    private static final UUID EVENT_REFERENCE = Generators.timeBasedEpochGenerator().generate();

    @Mock
    ProcessUpdateRepository repository;

    @Mock
    InternalMessageProducer eventProducer;

    @Mock
    ProcessUpdate processUpdate;

    @Mock
    Message message;

    @Captor
    ArgumentCaptor<ProcessUpdate> processUpdateArgumentCaptor;

    private ProcessUpdateService service;


    @BeforeEach
    void setUp() {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        service = new ProcessUpdateService(eventProducer, repository, transactionManager);
    }

    @Test
    void domainEventReceived() {
        when(repository.findByOriginProcessIdAndMessageNameAndIdempotenceId(ORIGIN_PROCESS_ID, EVENT_NAME, IDEMPOTENCE_ID)).thenReturn(Optional.empty());
        doReturn(processUpdate).when(repository).save(processUpdateArgumentCaptor.capture());
        mockEventIdempotenceData();
        when(message.getId()).thenReturn(EVENT_REFERENCE);

        service.messageReceived(ORIGIN_PROCESS_ID, message);

        verify(eventProducer).produceProcessContextOutdatedEventSynchronously(ORIGIN_PROCESS_ID);
        assertProcessUpdate(ProcessUpdateType.DOMAIN_EVENT, null);
    }

    @Test
    void domainEventReceived_whenUpdateWithSameIdempotenceIdReceived_expectNotSaved_outdatedMessageProduced() {
        when(repository.findByOriginProcessIdAndMessageNameAndIdempotenceId(ORIGIN_PROCESS_ID, EVENT_NAME, IDEMPOTENCE_ID)).thenReturn(Optional.of(mock(ProcessUpdate.class)));
        mockEventIdempotenceData();

        service.messageReceived(ORIGIN_PROCESS_ID, message);

        verifyNoMoreInteractions(repository);
        verify(eventProducer).produceProcessContextOutdatedEventSynchronously(ORIGIN_PROCESS_ID);
    }

    @Test
    void createProcessReceived() {
        when(repository.findByOriginProcessIdAndMessageNameAndIdempotenceId(ORIGIN_PROCESS_ID, EVENT_NAME, IDEMPOTENCE_ID)).thenReturn(Optional.empty());
        doReturn(processUpdate).when(repository).save(processUpdateArgumentCaptor.capture());
        mockEventIdempotenceData();
        when(message.getId()).thenReturn(EVENT_REFERENCE);
        final String templateName = "templateName";

        service.createProcessReceived(ORIGIN_PROCESS_ID, templateName, message);

        verify(eventProducer).produceProcessContextOutdatedEventSynchronously(ORIGIN_PROCESS_ID);
        assertProcessUpdate(ProcessUpdateType.CREATE_PROCESS, null);
        assertEquals(templateName, processUpdateArgumentCaptor.getValue().getParams());
    }

    @Test
    void createProcessReceived_whenUpdateWithSameIdempotenceIdReceived_expectNotSaved_outdatedMessageProduced() {
        when(repository.findByOriginProcessIdAndMessageNameAndIdempotenceId(ORIGIN_PROCESS_ID, EVENT_NAME, IDEMPOTENCE_ID)).thenReturn(Optional.of(mock(ProcessUpdate.class)));
        mockEventIdempotenceData();
        final String templateName = "templateName";

        service.createProcessReceived(ORIGIN_PROCESS_ID, templateName, message);

        verifyNoMoreInteractions(repository);
        verify(eventProducer).produceProcessContextOutdatedEventSynchronously(ORIGIN_PROCESS_ID);
    }

    private void mockEventIdempotenceData() {
        when(message.getMessageName()).thenReturn(EVENT_NAME);
        when(message.getIdempotenceId()).thenReturn(IDEMPOTENCE_ID);
    }

    private void assertProcessUpdate(ProcessUpdateType processUpdateType, String name) {
        assertEquals(ORIGIN_PROCESS_ID, processUpdateArgumentCaptor.getValue().getOriginProcessId());
        assertEquals(EVENT_NAME, processUpdateArgumentCaptor.getValue().getMessageName());
        assertEquals(IDEMPOTENCE_ID, processUpdateArgumentCaptor.getValue().getIdempotenceId());
        assertEquals(processUpdateType, processUpdateArgumentCaptor.getValue().getProcessUpdateType());
        assertEquals(Optional.of(EVENT_REFERENCE), processUpdateArgumentCaptor.getValue().getMessageReference());
        assertEquals(name, processUpdateArgumentCaptor.getValue().getName());
    }
}
