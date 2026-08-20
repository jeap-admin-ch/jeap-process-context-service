package ch.admin.bit.jeap.processcontext.domain.maintenance;

import ch.admin.bit.jeap.processcontext.domain.tx.Transactions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaintenanceTaskDispatcherTest {

    private static final UUID JOB_ID = UUID.fromString("88dbb65f-9634-4685-bc86-17b72d715d3e");
    private static final UUID TASK_ID = UUID.fromString("019c8c72-6fd1-7f25-a9a1-3b3d51fbb321");

    @Mock
    private MaintenanceJobRepository repository;
    @Mock
    private MaintenanceEventPublisher eventPublisher;
    @Mock
    private Transactions transactions;

    private MaintenanceTaskDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        MaintenanceProperties properties = new MaintenanceProperties();
        properties.getDispatcher().setBatchSize(25);
        dispatcher = new MaintenanceTaskDispatcher(repository, eventPublisher, transactions, properties);
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(transactions).withinNewTransaction(any());
    }

    @Test
    void dispatchCreatedTasks_queuesEventAndStateInOneTransaction() {
        MaintenanceJob job = job(MaintenanceTaskState.CREATED);
        when(repository.findTaskIdsByState(MaintenanceTaskState.CREATED, 25)).thenReturn(List.of(TASK_ID));
        when(repository.findByTaskIdForUpdate(TASK_ID)).thenReturn(Optional.of(job));

        dispatcher.dispatchCreatedTasks();

        verify(eventPublisher).publish(job, job.tasks().getFirst());
        ArgumentCaptor<MaintenanceJob> captor = ArgumentCaptor.forClass(MaintenanceJob.class);
        verify(repository).update(captor.capture());
        assertThat(captor.getValue().tasks().getFirst().taskState()).isEqualTo(MaintenanceTaskState.EVENT_QUEUED);
    }

    @Test
    void dispatchCreatedTasks_taskAlreadyClaimed_doesNotPublishAgain() {
        when(repository.findTaskIdsByState(MaintenanceTaskState.CREATED, 25)).thenReturn(List.of(TASK_ID));
        when(repository.findByTaskIdForUpdate(TASK_ID)).thenReturn(Optional.of(job(MaintenanceTaskState.EVENT_QUEUED)));

        dispatcher.dispatchCreatedTasks();

        verifyNoInteractions(eventPublisher);
        verify(repository, never()).update(any());
    }

    private static MaintenanceJob job(MaintenanceTaskState state) {
        Instant now = Instant.parse("2026-08-06T08:03:12Z");
        return new MaintenanceJob(JOB_ID, MaintenanceJobType.RELATION_REEVALUATION, "assessmentProcess",
                "a".repeat(64), MaintenanceJobState.OPEN, null, now, null, null, null,
                List.of(new MaintenanceTask(TASK_ID, MaintenanceTargetType.PROCESS, "assessment-4711",
                        "assessment-4711", state, now, null, null, null)));
    }
}
