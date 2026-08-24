package ch.admin.bit.jeap.processcontext.domain.maintenance;

import ch.admin.bit.jeap.processcontext.domain.tx.Transactions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.junit.jupiter.api.AfterEach;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaintenanceTaskResultServiceTest {

    private static final UUID JOB_ID = UUID.fromString("88dbb65f-9634-4685-bc86-17b72d715d3e");
    private static final UUID TASK_ID = UUID.fromString("019c8c72-6fd1-7f25-a9a1-3b3d51fbb321");
    @Mock
    private MaintenanceJobRepository repository;
    @Mock
    private Transactions transactions;

    private MaintenanceTaskResultService service;

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @BeforeEach
    void setUp() {
        service = new MaintenanceTaskResultService(repository, transactions);
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(transactions).withinNewTransaction(any());
    }

    @Test
    void markFailed_completesMixedJobAsPartiallySucceeded() {
        when(repository.findByTaskIdForUpdate(TASK_ID)).thenReturn(Optional.of(job()));
        MDC.put("traceId", "4bf92f3577b34da6a3ce929d0e0e4736");

        service.markFailedInNewTransaction(TASK_ID, new IllegalStateException("relation failed"));

        ArgumentCaptor<MaintenanceTask> captor = ArgumentCaptor.forClass(MaintenanceTask.class);
        verify(repository).updateTaskAndJob(eq(job()), captor.capture());
        MaintenanceTask updated = captor.getValue();
        assertThat(updated.taskState()).isEqualTo(MaintenanceTaskState.FAILED);
        assertThat(updated.errorMessage())
                .isEqualTo("IllegalStateException: relation failed")
                .doesNotContain("MaintenanceTaskResultServiceTest");
        assertThat(updated.errorTraceId()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
    }

    @Test
    void markFailed_sanitizesAndLimitsActionableMessage() {
        when(repository.findByTaskIdForUpdate(TASK_ID)).thenReturn(Optional.of(job()));

        service.markFailedInNewTransaction(TASK_ID, new IllegalArgumentException("invalid relation\n" + "x".repeat(600)));

        ArgumentCaptor<MaintenanceTask> captor = ArgumentCaptor.forClass(MaintenanceTask.class);
        verify(repository).updateTaskAndJob(any(), captor.capture());
        String errorMessage = captor.getValue().errorMessage();
        assertThat(errorMessage).startsWith("IllegalArgumentException: invalid relation x").hasSize(500).doesNotContain("\n");
    }

    @Test
    void markFailed_missingTask_rethrowsSoEventIsNotAcknowledged() {
        when(repository.findByTaskIdForUpdate(TASK_ID)).thenReturn(Optional.empty());
        IllegalStateException processingFailure = new IllegalStateException();

        assertThatThrownBy(() -> service.markFailedInNewTransaction(TASK_ID, processingFailure))
                .isInstanceOf(MaintenanceTaskNotFoundException.class);
        verify(repository, never()).updateTaskAndJob(any(), any());
    }

    private static MaintenanceJob job() {
        Instant now = Instant.parse("2026-08-06T08:03:12Z");
        return new MaintenanceJob(JOB_ID, MaintenanceJobType.RELATION_REEVALUATION, "assessmentProcess",
                "a".repeat(64), MaintenanceJobState.OPEN, null, now, null, null, null,
                List.of(new MaintenanceTask(TASK_ID, MaintenanceTargetType.PROCESS, "assessment-4711",
                        "assessment-4711", MaintenanceTaskState.EVENT_QUEUED, now, null, null, null)));
    }
}
