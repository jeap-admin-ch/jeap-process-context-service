package ch.admin.bit.jeap.processcontext.adapter.kafka.internalevent.consumer;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceService;
import ch.admin.bit.jeap.processcontext.internal.event.outdated.ProcessContextOutdatedEvent;
import ch.admin.bit.jeap.processcontext.internal.event.outdated.ProcessContextOutdatedPayload;
import ch.admin.bit.jeap.processcontext.internal.event.outdated.ProcessUpdateType;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ProcessContextOutdatedEventConsumerTest {
    private final ProcessInstanceService processInstanceService = mock(ProcessInstanceService.class);
    private final MaintenanceProcessContextOutdatedEventHandler maintenanceHandler =
            mock(MaintenanceProcessContextOutdatedEventHandler.class);
    private final Acknowledgment acknowledgment = mock(Acknowledgment.class);

    @Test
    void consumeMaintenanceEvent_terminalOutcomeIsAcknowledged() {
        ProcessContextOutdatedEvent event = event();
        ProcessContextOutdatedEventConsumer consumer =
                new ProcessContextOutdatedEventConsumer(processInstanceService, Optional.of(maintenanceHandler));

        consumer.consumeProcessContextUpdatedEvent(event, acknowledgment);

        verify(maintenanceHandler).handle(event);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeMaintenanceEvent_terminalPersistenceFailureUsesKafkaErrorHandling() {
        ProcessContextOutdatedEvent event = event();
        doThrow(new IllegalStateException("database unavailable")).when(maintenanceHandler).handle(event);
        ProcessContextOutdatedEventConsumer consumer =
                new ProcessContextOutdatedEventConsumer(processInstanceService, Optional.of(maintenanceHandler));

        assertThatThrownBy(() -> consumer.consumeProcessContextUpdatedEvent(event, acknowledgment))
                .isInstanceOf(InternalMessageConsumerException.class);
        verifyNoInteractions(acknowledgment);
    }

    @Test
    void consumeUnknownUpdateType_isAcknowledgedAndSkipped() {
        ProcessContextOutdatedEvent event = event(ProcessUpdateType.UNKNOWN);
        ProcessContextOutdatedEventConsumer consumer =
                new ProcessContextOutdatedEventConsumer(processInstanceService, Optional.of(maintenanceHandler));

        consumer.consumeProcessContextUpdatedEvent(event, acknowledgment);

        verifyNoInteractions(processInstanceService, maintenanceHandler);
        verify(acknowledgment).acknowledge();
    }

    private static ProcessContextOutdatedEvent event() {
        return event(ProcessUpdateType.REPUBLISH_RELATION_JOB);
    }

    private static ProcessContextOutdatedEvent event(ProcessUpdateType processUpdateType) {
        ProcessContextOutdatedPayload payload = mock(ProcessContextOutdatedPayload.class);
        when(payload.getProcessUpdateType()).thenReturn(processUpdateType);
        ProcessContextOutdatedEvent event = mock(ProcessContextOutdatedEvent.class);
        when(event.getPayload()).thenReturn(payload);
        return event;
    }
}
