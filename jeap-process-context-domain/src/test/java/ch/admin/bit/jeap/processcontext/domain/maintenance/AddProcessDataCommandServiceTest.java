package ch.admin.bit.jeap.processcontext.domain.maintenance;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessDataValue;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessDataService;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceRepository;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.tx.Transactions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddProcessDataCommandServiceTest {
    private static final UUID JOB_ID = UUID.fromString("88dbb65f-9634-4685-bc86-17b72d715d3e");
    private static final UUID TASK_ID = UUID.fromString("019c8c72-6fd1-7f25-a9a1-3b3d51fbb321");
    private static final List<ProcessDataValue> PROCESS_DATA = List.of(
            new ProcessDataValue("assessmentId", "a-123", null),
            new ProcessDataValue("artefactId", "art-456", "FinalVersion"));

    @Mock
    private MaintenanceJobRepository repository;
    @Mock
    private ProcessInstanceRepository processInstanceRepository;
    @Mock
    private ProcessDataService processDataService;
    @Mock
    private MaintenanceEventPublisher eventPublisher;
    @Mock
    private Transactions transactions;

    private AddProcessDataCommandService service;

    @BeforeEach
    void setUp() {
        service = new AddProcessDataCommandService(repository, processInstanceRepository, processDataService,
                eventPublisher, transactions);
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(transactions).withinNewTransaction(any());
    }

    @Test
    void handle_validCommandQueuesInternalEventAndStateAtomically() {
        MaintenanceJob job = job(MaintenanceTaskState.COMMAND_QUEUED);
        when(repository.findByTaskIdForUpdate(TASK_ID)).thenReturn(Optional.of(job));
        ProcessInstance processInstance = processInstance("assessmentProcess");
        when(processInstanceRepository.findByOriginProcessId("assessment-4711"))
                .thenReturn(Optional.of(processInstance));

        service.handle(command());

        ArgumentCaptor<MaintenanceTask> captor = ArgumentCaptor.forClass(MaintenanceTask.class);
        InOrder inOrder = inOrder(processDataService, eventPublisher, repository);
        inOrder.verify(processDataService).addProcessData(processInstance, job.task(TASK_ID).processData());
        inOrder.verify(eventPublisher).publish(job, job.task(TASK_ID));
        inOrder.verify(repository).updateTask(eq(job), captor.capture());
        assertThat(captor.getValue().taskState()).isEqualTo(MaintenanceTaskState.EVENT_QUEUED);
    }

    @Test
    void handle_missingProcessThrowsTargetNotFoundWithoutQueuingEvent() {
        when(repository.findByTaskIdForUpdate(TASK_ID))
                .thenReturn(Optional.of(job(MaintenanceTaskState.COMMAND_QUEUED)));
        when(processInstanceRepository.findByOriginProcessId("assessment-4711")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handle(command()))
                .isInstanceOf(MaintenanceTargetNotFoundException.class);

        verifyNoInteractions(processDataService, eventPublisher);
        verify(repository, never()).updateTask(any(), any());
    }

    @Test
    void handle_templateMismatchFailsWithoutAddingDataOrQueuingEvent() {
        when(repository.findByTaskIdForUpdate(TASK_ID))
                .thenReturn(Optional.of(job(MaintenanceTaskState.COMMAND_QUEUED)));
        ProcessInstance processInstance = processInstance("otherProcess");
        when(processInstanceRepository.findByOriginProcessId("assessment-4711"))
                .thenReturn(Optional.of(processInstance));

        assertThatThrownBy(() -> service.handle(command()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("template");

        verifyNoInteractions(processDataService, eventPublisher);
        verify(repository, never()).updateTask(any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = MaintenanceTaskState.class, names = {"EVENT_QUEUED", "SUCCEEDED", "NOT_FOUND", "FAILED"})
    void handle_duplicateCommandIsNoOp(MaintenanceTaskState state) {
        when(repository.findByTaskIdForUpdate(TASK_ID)).thenReturn(Optional.of(job(state)));

        service.handle(command());

        verifyNoInteractions(processInstanceRepository, processDataService, eventPublisher);
        verify(repository, never()).updateTask(any(), any());
    }

    @ParameterizedTest(name = "rejects mismatching {0}")
    @MethodSource("mismatchingCommands")
    void handle_rejectsMismatchingCommandField(String fieldName, MaintenanceCommand command) {
        when(repository.findByTaskIdForUpdate(TASK_ID))
                .thenReturn(Optional.of(job(MaintenanceTaskState.COMMAND_QUEUED)));

        assertThatThrownBy(() -> service.handle(command))
                .isInstanceOf(MaintenanceCommandRejectedException.class)
                .hasMessageContaining(fieldName);
        verifyNoInteractions(processInstanceRepository, processDataService, eventPublisher);
    }

    @Test
    void handle_rejectsCommandForNonBackfillJob() {
        MaintenanceJob job = job(MaintenanceTaskState.COMMAND_QUEUED);
        MaintenanceJob reevaluationJob = new MaintenanceJob(job.jobId(), MaintenanceJobType.RELATION_REEVALUATION,
                job.processTemplateName(), job.requestHash(), job.jobState(), job.jobResult(), job.startedAt(),
                job.completedAt(), job.startedByName(), job.startedByExtId(), job.tasks());
        when(repository.findByTaskIdForUpdate(TASK_ID)).thenReturn(Optional.of(reevaluationJob));

        assertThatThrownBy(() -> service.handle(command()))
                .isInstanceOf(MaintenanceCommandRejectedException.class)
                .hasMessageContaining("PROCESS_DATA_BACKFILL");
        verifyNoInteractions(eventPublisher);
    }

    private static MaintenanceCommand command() {
        return new MaintenanceCommand(JOB_ID, TASK_ID, "assessmentProcess", PROCESS_DATA,
                "assessment-4711", MaintenanceCommand.idempotenceId(JOB_ID, TASK_ID));
    }

    private static Stream<Arguments> mismatchingCommands() {
        return Stream.of(
                Arguments.of("jobId", new MaintenanceCommand(UUID.randomUUID(), TASK_ID, "assessmentProcess",
                        PROCESS_DATA, "assessment-4711", MaintenanceCommand.idempotenceId(JOB_ID, TASK_ID))),
                Arguments.of("processTemplateName", new MaintenanceCommand(JOB_ID, TASK_ID, "otherProcess",
                        PROCESS_DATA, "assessment-4711", MaintenanceCommand.idempotenceId(JOB_ID, TASK_ID))),
                Arguments.of("processData", new MaintenanceCommand(JOB_ID, TASK_ID, "assessmentProcess",
                        List.of(new ProcessDataValue("changed", "value", null)), "assessment-4711",
                        MaintenanceCommand.idempotenceId(JOB_ID, TASK_ID))),
                Arguments.of("processId", new MaintenanceCommand(JOB_ID, TASK_ID, "assessmentProcess",
                        PROCESS_DATA, "other-process", MaintenanceCommand.idempotenceId(JOB_ID, TASK_ID))),
                Arguments.of("idempotenceId", new MaintenanceCommand(JOB_ID, TASK_ID, "assessmentProcess",
                        PROCESS_DATA, "assessment-4711", "other-id")));
    }

    private static ProcessInstance processInstance(String templateName) {
        ProcessInstance processInstance = mock(ProcessInstance.class);
        ProcessTemplate template = mock(ProcessTemplate.class);
        when(template.getName()).thenReturn(templateName);
        when(processInstance.getProcessTemplate()).thenReturn(template);
        return processInstance;
    }

    private static MaintenanceJob job(MaintenanceTaskState state) {
        Instant now = Instant.parse("2026-08-06T08:03:12Z");
        MaintenanceTask task = new MaintenanceTask(TASK_ID, MaintenanceTargetType.PROCESS, "assessment-4711",
                "assessment-4711", state, now, null, null, null, PROCESS_DATA);
        return new MaintenanceJob(JOB_ID, MaintenanceJobType.PROCESS_DATA_BACKFILL, "assessmentProcess",
                "a".repeat(64), state.isTerminal() ? MaintenanceJobState.COMPLETED : MaintenanceJobState.OPEN,
                state.isTerminal() ? MaintenanceJobResult.FAILED : null, now,
                state.isTerminal() ? now : null, null, null, List.of(task));
    }
}
