package ch.admin.bit.jeap.processcontext.adapter.kafka.internalevent.consumer;

import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTargetNotFoundException;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTaskExecutionService;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTaskResultService;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceUpdateType;
import ch.admin.bit.jeap.processcontext.internal.event.outdated.MaintenanceJobTask;
import ch.admin.bit.jeap.processcontext.internal.event.outdated.ProcessContextOutdatedEvent;
import ch.admin.bit.jeap.processcontext.internal.event.outdated.ProcessContextOutdatedPayload;
import ch.admin.bit.jeap.processcontext.internal.event.outdated.ProcessUpdateType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class MaintenanceProcessContextOutdatedEventHandlerTest {
    private static final UUID JOB_ID = UUID.fromString("88dbb65f-9634-4685-bc86-17b72d715d3e");
    private static final UUID TASK_ID = UUID.fromString("019c8c72-6fd1-7f25-a9a1-3b3d51fbb321");

    private final MaintenanceTaskExecutionService executionService = mock(MaintenanceTaskExecutionService.class);
    private final MaintenanceTaskResultService resultService = mock(MaintenanceTaskResultService.class);
    private final MaintenanceProcessContextOutdatedEventHandler handler =
            new MaintenanceProcessContextOutdatedEventHandler(executionService, resultService);

    @Test
    void handle_success_executesTask() {
        handler.handle(event());

        verify(executionService).execute(TASK_ID, MaintenanceUpdateType.REEVALUATE_JOB);
        verifyNoInteractions(resultService);
    }

    @Test
    void handle_missingProcess_persistsNotFoundWithoutRethrowing() {
        doThrow(new MaintenanceTargetNotFoundException()).when(executionService)
                .execute(TASK_ID, MaintenanceUpdateType.REEVALUATE_JOB);

        handler.handle(event());

        verify(resultService).markNotFoundInNewTransaction(TASK_ID);
    }

    @Test
    void handle_processingFailure_persistsSanitizedFailureWithoutRethrowing() {
        IllegalStateException exception = new IllegalStateException("secret internal value");
        doThrow(exception).when(executionService).execute(TASK_ID, MaintenanceUpdateType.REEVALUATE_JOB);

        handler.handle(event());

        verify(resultService).markFailedInNewTransaction(TASK_ID, exception);
    }

    @Test
    void handle_terminalPersistenceFailure_rethrowsForKafkaRetry() {
        doThrow(new IllegalStateException("processing failed")).when(executionService)
                .execute(TASK_ID, MaintenanceUpdateType.REEVALUATE_JOB);
        doThrow(new IllegalStateException("database unavailable")).when(resultService)
                .markFailedInNewTransaction(eq(TASK_ID), any(RuntimeException.class));
        ProcessContextOutdatedEvent event = event();

        assertThatThrownBy(() -> handler.handle(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");
    }

    private static ProcessContextOutdatedEvent event() {
        ProcessContextOutdatedPayload payload = mock(ProcessContextOutdatedPayload.class);
        MaintenanceJobTask task = mock(MaintenanceJobTask.class);
        when(task.getJobId()).thenReturn(JOB_ID);
        when(task.getTaskId()).thenReturn(TASK_ID);
        when(payload.getMaintenanceJobTask()).thenReturn(task);
        when(payload.getProcessUpdateType()).thenReturn(ProcessUpdateType.REEVALUATE_JOB);
        ProcessContextOutdatedEvent event = mock(ProcessContextOutdatedEvent.class);
        when(event.getPayload()).thenReturn(payload);
        return event;
    }
}
