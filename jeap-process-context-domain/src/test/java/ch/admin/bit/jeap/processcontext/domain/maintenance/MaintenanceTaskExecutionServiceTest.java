package ch.admin.bit.jeap.processcontext.domain.maintenance;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessDataValue;
import ch.admin.bit.jeap.processcontext.domain.processinstance.Relation;
import ch.admin.bit.jeap.processcontext.domain.processinstance.RelationRepository;
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
    private static final UUID RELATION_ID = UUID.fromString("019c8c72-6fd1-7f25-a9a1-3b3d51fbb322");

    @Mock
    private MaintenanceJobRepository repository;
    @Mock
    private ProcessInstanceRepository processInstanceRepository;
    @Mock
    private RelationService relationService;
    @Mock
    private RelationRepository relationRepository;
    @Mock
    private Transactions transactions;

    private MaintenanceTaskExecutionService service;

    @BeforeEach
    void setUp() {
        service = new MaintenanceTaskExecutionService(repository, processInstanceRepository, relationService,
                relationRepository, transactions);
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(transactions).withinNewTransaction(any());
    }

    @Test
    void execute_reevaluatesCurrentTemplateAndCompletesTask() {
        when(repository.findByTaskIdForUpdate(TASK_ID)).thenReturn(Optional.of(job(MaintenanceTaskState.EVENT_QUEUED)));
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
        when(repository.findByTaskIdForUpdate(TASK_ID)).thenReturn(Optional.of(job(MaintenanceTaskState.EVENT_QUEUED)));
        when(processInstanceRepository.findByOriginProcessId("assessment-4711")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(TASK_ID, MaintenanceUpdateType.REEVALUATE_JOB))
                .isInstanceOf(MaintenanceTargetNotFoundException.class);
        verify(repository, never()).updateTaskAndJob(any(), any());
    }

    @Test
    void execute_terminalTask_acknowledgesWithoutExecutingAgain() {
        when(repository.findByTaskIdForUpdate(TASK_ID)).thenReturn(Optional.of(job(MaintenanceTaskState.SUCCEEDED)));

        service.execute(TASK_ID, MaintenanceUpdateType.REEVALUATE_JOB);

        verifyNoInteractions(processInstanceRepository, relationService);
        verify(repository, never()).updateTaskAndJob(any(), any());
    }

    @Test
    void execute_backfillReevaluatesAndCompletesTask() {
        MaintenanceJob job = backfillJob(MaintenanceTaskState.EVENT_QUEUED);
        when(repository.findByTaskIdForUpdate(TASK_ID)).thenReturn(Optional.of(job));
        ProcessInstance processInstance = processInstance("assessmentProcess");
        when(processInstanceRepository.findByOriginProcessId("assessment-4711")).thenReturn(Optional.of(processInstance));

        service.execute(TASK_ID, MaintenanceUpdateType.BACKFILL_JOB);

        verify(relationService).reevaluateRelations(processInstance);
        ArgumentCaptor<MaintenanceTask> captor = ArgumentCaptor.forClass(MaintenanceTask.class);
        verify(repository).updateTaskAndJob(eq(job), captor.capture());
        MaintenanceTask completedTask = captor.getValue();
        assertThat(completedTask.taskState()).isEqualTo(MaintenanceTaskState.SUCCEEDED);
        assertThat(completedTask.processData()).isEqualTo(job.task(TASK_ID).processData());
    }

    @Test
    void execute_backfillMissingProcessThrowsTargetNotFound() {
        when(repository.findByTaskIdForUpdate(TASK_ID))
                .thenReturn(Optional.of(backfillJob(MaintenanceTaskState.EVENT_QUEUED)));
        when(processInstanceRepository.findByOriginProcessId("assessment-4711")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(TASK_ID, MaintenanceUpdateType.BACKFILL_JOB))
                .isInstanceOf(MaintenanceTargetNotFoundException.class);
    }

    @Test
    void execute_backfillTemplateMismatchThrowsTargetNotFound() {
        when(repository.findByTaskIdForUpdate(TASK_ID))
                .thenReturn(Optional.of(backfillJob(MaintenanceTaskState.EVENT_QUEUED)));
        ProcessInstance processInstance = processInstance("otherTemplate");
        when(processInstanceRepository.findByOriginProcessId("assessment-4711"))
                .thenReturn(Optional.of(processInstance));

        assertThatThrownBy(() -> service.execute(TASK_ID, MaintenanceUpdateType.BACKFILL_JOB))
                .isInstanceOf(MaintenanceTargetNotFoundException.class);
        verifyNoInteractions(relationService);
    }

    @Test
    void execute_backfillRejectsMismatchingJobType() {
        when(repository.findByTaskIdForUpdate(TASK_ID))
                .thenReturn(Optional.of(job(MaintenanceTaskState.EVENT_QUEUED)));

        assertThatThrownBy(() -> service.execute(TASK_ID, MaintenanceUpdateType.BACKFILL_JOB))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(processInstanceRepository, relationService);
    }

    @Test
    void execute_republishesRelationAndCompletesOnlyAfterListenerReturns() {
        MaintenanceJob job = republicationJob(MaintenanceTaskState.EVENT_QUEUED, RELATION_ID);
        Relation relation = mock(Relation.class);
        when(repository.findByTaskIdForUpdate(TASK_ID)).thenReturn(Optional.of(job));
        when(relationRepository.findById(RELATION_ID)).thenReturn(Optional.of(relation));

        service.execute(TASK_ID, MaintenanceUpdateType.REPUBLISH_RELATION_JOB);

        var inOrder = inOrder(relationService, repository);
        inOrder.verify(relationService).republishRelation(relation);
        ArgumentCaptor<MaintenanceTask> captor = ArgumentCaptor.forClass(MaintenanceTask.class);
        inOrder.verify(repository).updateTaskAndJob(eq(job), captor.capture());
        assertThat(captor.getValue().taskState()).isEqualTo(MaintenanceTaskState.SUCCEEDED);
        assertThat(captor.getValue().relationId()).isEqualTo(RELATION_ID);
    }

    @Test
    void execute_republicationMissingRelationThrowsTargetNotFound() {
        when(repository.findByTaskIdForUpdate(TASK_ID))
                .thenReturn(Optional.of(republicationJob(MaintenanceTaskState.EVENT_QUEUED, RELATION_ID)));
        when(relationRepository.findById(RELATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(TASK_ID, MaintenanceUpdateType.REPUBLISH_RELATION_JOB))
                .isInstanceOf(MaintenanceTargetNotFoundException.class);
        verifyNoInteractions(relationService);
        verify(repository, never()).updateTaskAndJob(any(), any());
    }

    @Test
    void execute_republicationListenerFailureDoesNotCompleteTask() {
        Relation relation = mock(Relation.class);
        when(repository.findByTaskIdForUpdate(TASK_ID))
                .thenReturn(Optional.of(republicationJob(MaintenanceTaskState.EVENT_QUEUED, RELATION_ID)));
        when(relationRepository.findById(RELATION_ID)).thenReturn(Optional.of(relation));
        doThrow(new IllegalStateException("listener failed")).when(relationService).republishRelation(relation);

        assertThatThrownBy(() -> service.execute(TASK_ID, MaintenanceUpdateType.REPUBLISH_RELATION_JOB))
                .hasMessage("listener failed");
        verify(repository, never()).updateTaskAndJob(any(), any());
    }

    @Test
    void execute_republicationTerminalTaskIsNoOpAndMismatchIsRejected() {
        when(repository.findByTaskIdForUpdate(TASK_ID))
                .thenReturn(Optional.of(republicationJob(MaintenanceTaskState.SUCCEEDED, RELATION_ID)),
                        Optional.of(republicationJob(MaintenanceTaskState.EVENT_QUEUED, RELATION_ID)));

        service.execute(TASK_ID, MaintenanceUpdateType.REPUBLISH_RELATION_JOB);
        assertThatThrownBy(() -> service.execute(TASK_ID, MaintenanceUpdateType.REEVALUATE_JOB))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(relationRepository, relationService);
    }

    private static ProcessInstance processInstance(String templateName) {
        ProcessInstance processInstance = mock(ProcessInstance.class);
        ProcessTemplate template = mock(ProcessTemplate.class);
        when(template.getName()).thenReturn(templateName);
        when(processInstance.getProcessTemplate()).thenReturn(template);
        return processInstance;
    }

    private static MaintenanceJob backfillJob(MaintenanceTaskState state) {
        Instant now = Instant.parse("2026-08-06T08:03:12Z");
        return new MaintenanceJob(JOB_ID, MaintenanceJobType.PROCESS_DATA_BACKFILL, "assessmentProcess",
                "b".repeat(64), MaintenanceJobState.OPEN, null, now, null, null, null,
                List.of(new MaintenanceTask(TASK_ID, MaintenanceTargetType.PROCESS, "assessment-4711",
                        "assessment-4711", state, now, null, null, null, List.of(
                        new ProcessDataValue("assessmentId", "a-123", null),
                        new ProcessDataValue("artefactId", "art-456", "FinalVersion")))));
    }

    private static MaintenanceJob job(MaintenanceTaskState state) {
        Instant now = Instant.parse("2026-08-06T08:03:12Z");
        return new MaintenanceJob(JOB_ID, MaintenanceJobType.RELATION_REEVALUATION, "assessmentProcess",
                "a".repeat(64), state == MaintenanceTaskState.SUCCEEDED ? MaintenanceJobState.COMPLETED : MaintenanceJobState.OPEN,
                state == MaintenanceTaskState.SUCCEEDED ? MaintenanceJobResult.SUCCEEDED : null, now,
                state == MaintenanceTaskState.SUCCEEDED ? now : null, null, null,
                List.of(new MaintenanceTask(TASK_ID, MaintenanceTargetType.PROCESS, "assessment-4711",
                        "assessment-4711", state, now, null, null, null)));
    }

    private static MaintenanceJob republicationJob(MaintenanceTaskState state, UUID relationId) {
        Instant now = Instant.parse("2026-08-06T08:03:12Z");
        return new MaintenanceJob(JOB_ID, MaintenanceJobType.RELATION_REPUBLICATION, null, "c".repeat(64),
                state.isTerminal() ? MaintenanceJobState.COMPLETED : MaintenanceJobState.OPEN,
                state == MaintenanceTaskState.SUCCEEDED ? MaintenanceJobResult.SUCCEEDED : null, now,
                state.isTerminal() ? now : null, null, null,
                List.of(new MaintenanceTask(TASK_ID, MaintenanceTargetType.RELATION, relationId.toString(), null,
                        relationId, state, now, null, null, null, List.of())));
    }
}
