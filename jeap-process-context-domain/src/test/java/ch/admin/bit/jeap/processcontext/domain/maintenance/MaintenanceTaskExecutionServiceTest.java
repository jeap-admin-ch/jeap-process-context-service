package ch.admin.bit.jeap.processcontext.domain.maintenance;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.relation.RelationService;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaintenanceTaskExecutionServiceTest {

    private static final UUID JOB_ID = UUID.fromString("88dbb65f-9634-4685-bc86-17b72d715d3e");
    private static final UUID TASK_ID = UUID.fromString("019c8c72-6fd1-7f25-a9a1-3b3d51fbb321");

    @Mock
    private MaintenanceJobRepository repository;
    @Mock
    private ProcessInstanceRepository processInstanceRepository;
    @Mock
    private RelationService relationService;
    @Mock
    private Transactions transactions;

    private MaintenanceTaskExecutionService service;

    @BeforeEach
    void setUp() {
        service = new MaintenanceTaskExecutionService(repository, processInstanceRepository, relationService, transactions);
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(transactions).withinNewTransaction(any());
    }

    @Test
    void execute_reevaluatesCurrentTemplateAndCompletesTask() {
        when(repository.findTaskForUpdate(TASK_ID)).thenReturn(Optional.of(job(MaintenanceTaskState.EVENT_QUEUED)));
        ProcessInstance processInstance = mock(ProcessInstance.class);
        ProcessTemplate template = mock(ProcessTemplate.class);
        when(template.getName()).thenReturn("assessmentProcess");
        when(processInstance.getProcessTemplate()).thenReturn(template);
        when(processInstanceRepository.findByOriginProcessId("assessment-4711")).thenReturn(Optional.of(processInstance));

        service.execute(TASK_ID, MaintenanceUpdateType.REEVALUATE_JOB);

        verify(relationService).reevaluateRelations(processInstance);
        ArgumentCaptor<MaintenanceTask> captor = ArgumentCaptor.forClass(MaintenanceTask.class);
        verify(repository).updateTaskAndJob(eq(job(MaintenanceTaskState.EVENT_QUEUED)), captor.capture());
        assertThat(captor.getValue().taskState()).isEqualTo(MaintenanceTaskState.SUCCEEDED);
    }

    @Test
    void execute_missingProcess_throwsTargetNotFound() {
        when(repository.findTaskForUpdate(TASK_ID)).thenReturn(Optional.of(job(MaintenanceTaskState.EVENT_QUEUED)));
        when(processInstanceRepository.findByOriginProcessId("assessment-4711")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(TASK_ID, MaintenanceUpdateType.REEVALUATE_JOB))
                .isInstanceOf(MaintenanceTargetNotFoundException.class);
    }

    @Test
    void execute_terminalTask_acknowledgesWithoutExecutingAgain() {
        when(repository.findTaskForUpdate(TASK_ID)).thenReturn(Optional.of(job(MaintenanceTaskState.SUCCEEDED)));

        service.execute(TASK_ID, MaintenanceUpdateType.REEVALUATE_JOB);

        verifyNoInteractions(processInstanceRepository, relationService);
        verify(repository, never()).updateTaskAndJob(any(), any());
    }

    private static MaintenanceJob job(MaintenanceTaskState state) {
        Instant now = Instant.parse("2026-08-06T08:03:12Z");
        return new MaintenanceJob(JOB_ID, MaintenanceJobType.RELATION_REEVALUATION, "assessmentProcess",
                "a".repeat(64), MaintenanceJobState.OPEN, null, now, null, null, null,
                List.of(new MaintenanceTask(TASK_ID, MaintenanceTargetType.PROCESS, "assessment-4711",
                        "assessment-4711", state, now, null, null, null)));
    }
}
