package ch.admin.bit.jeap.processcontext.domain.maintenance;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessDataValue;
import ch.admin.bit.jeap.processcontext.domain.tx.Transactions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackfillJobServiceTest {

    private static final UUID JOB_ID = UUID.fromString("88dbb65f-9634-4685-bc86-17b72d715d3e");

    @Mock
    private MaintenanceJobRepository maintenanceJobRepository;
    @Mock
    private MaintenanceCommandPublisher maintenanceCommandPublisher;
    @Mock
    private Transactions transactions;

    private MaintenanceProperties maintenanceProperties;
    private BackfillJobService service;

    @BeforeEach
    void setUp() {
        maintenanceProperties = new MaintenanceProperties();
        maintenanceProperties.getLimits().setMaxTasksPerJob(10);
        maintenanceProperties.getLimits().setMaxFieldLength(50);
        service = new BackfillJobService(maintenanceJobRepository, maintenanceCommandPublisher,
                maintenanceProperties, transactions);
        lenient().when(transactions.withinNewTransactionWithResult(any())).thenAnswer(invocation ->
                invocation.getArgument(0, Supplier.class).get());
    }

    @Test
    void submit_normalizesSortsAndCreatesBackfillTasks() {
        when(maintenanceJobRepository.findById(JOB_ID)).thenReturn(Optional.empty());

        boolean created = service.submit(new BackfillJobSubmission(JOB_ID, " assessmentProcess ", List.of(
                new BackfillJobEntry(" assessment-4712 ", List.of(
                        new ProcessDataValue(" artefactId ", " art-456 ", " FinalVersion "),
                        new ProcessDataValue(" assessmentId ", " a-123 ", "  "))),
                new BackfillJobEntry("assessment-4711", List.of(
                        new ProcessDataValue("assessmentId", "a-789", null)))),
                new MaintenanceJobSubmitter("John Doe", "287365")));

        ArgumentCaptor<MaintenanceJob> captor = ArgumentCaptor.forClass(MaintenanceJob.class);
        verify(maintenanceJobRepository).create(captor.capture());
        MaintenanceJob job = captor.getValue();
        assertThat(job.jobType()).isEqualTo(MaintenanceJobType.PROCESS_DATA_BACKFILL);
        assertThat(job.processTemplateName()).isEqualTo("assessmentProcess");
        assertThat(job.requestHash()).matches("[0-9a-f]{64}");
        assertThat(job.tasks()).extracting(MaintenanceTask::originProcessId)
                .containsExactly("assessment-4711", "assessment-4712");
        assertThat(job.tasks()).allSatisfy(task ->
                assertThat(task.taskState()).isEqualTo(MaintenanceTaskState.COMMAND_QUEUED));
        assertThat(job.tasks().get(1).processData()).containsExactly(
                new ProcessDataValue("artefactId", "art-456", "FinalVersion"),
                new ProcessDataValue("assessmentId", "a-123", null));
        assertThat(created).isTrue();
        job.tasks().forEach(task -> verify(maintenanceCommandPublisher).publish(job, task));
    }

    @Test
    void submit_sameNormalizedInputIsIdempotentAndHashIgnoresSubmitterAndOrder() {
        BackfillJobSubmission first = submission(List.of(
                entry("process-b", value("key-b", "value-b", "role")),
                entry("process-a", value("key-a", "value-a", null))));
        MaintenanceJob existing = MaintenanceJob.createBackfill(first.normalized(maintenanceProperties.getLimits()));
        when(maintenanceJobRepository.findById(JOB_ID)).thenReturn(Optional.of(existing));

        boolean created = service.submit(new BackfillJobSubmission(JOB_ID, " assessmentProcess ", List.of(
                entry(" process-a ", value(" key-a ", " value-a ", " ")),
                entry("process-b", value("key-b", "value-b", " role "))),
                new MaintenanceJobSubmitter("Another User", "999")));

        verify(maintenanceJobRepository, never()).create(any());
        verifyNoInteractions(maintenanceCommandPublisher);
        assertThat(created).isFalse();
    }

    @Test
    void submit_sameJobIdWithDifferentValuesThrowsConflict() {
        BackfillJobSubmission first = submission(List.of(entry("process-a", value("key", "value-a", null))));
        when(maintenanceJobRepository.findById(JOB_ID)).thenReturn(Optional.of(
                MaintenanceJob.createBackfill(first.normalized(maintenanceProperties.getLimits()))));
        BackfillJobSubmission conflictingSubmission =
                submission(List.of(entry("process-a", value("key", "value-b", null))));

        assertThatThrownBy(() -> service.submit(conflictingSubmission))
                .isInstanceOf(MaintenanceJobException.class)
                .extracting("reason")
                .isEqualTo(MaintenanceJobExceptionReason.CONFLICT);
    }

    @Test
    void submit_concurrentEquivalentCreationConvergesToSuccess() {
        BackfillJobSubmission submission = submission(List.of(entry("process-a", value("key", "value", null))));
        MaintenanceJob existing = MaintenanceJob.createBackfill(submission.normalized(maintenanceProperties.getLimits()));
        when(maintenanceJobRepository.findById(JOB_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));
        doThrow(MaintenanceJobAlreadyExistsException.class).when(maintenanceJobRepository).create(any());

        boolean created = service.submit(submission);

        verify(maintenanceJobRepository).create(any());
        verify(maintenanceJobRepository, times(2)).findById(JOB_ID);
        verifyNoInteractions(maintenanceCommandPublisher);
        assertThat(created).isFalse();
    }

    @Test
    void submit_rejectsDuplicateNormalizedOriginProcessIds() {
        assertInvalid(submission(List.of(
                entry("process-a", value("key-a", "value-a", null)),
                entry(" process-a ", value("key-b", "value-b", null)))));
    }

    @Test
    void submit_rejectsDuplicateNormalizedValues() {
        assertInvalid(submission(List.of(new BackfillJobEntry("process-a", List.of(
                value("key", "value", "role"), value(" key ", " value ", " role "))))));
    }

    @Test
    void submit_rejectsEmptyAndTooManyValuesPerTask() {
        assertInvalid(submission(List.of(new BackfillJobEntry("process-a", List.of()))));

        reset(maintenanceJobRepository);
        maintenanceProperties.getLimits().setMaxProcessDataValuesPerTask(1);
        assertInvalid(submission(List.of(new BackfillJobEntry("process-a", List.of(
                value("key-a", "value-a", null), value("key-b", "value-b", null))))));
    }

    @Test
    void submit_appliesUtf8ByteLimitToEveryValueField() {
        maintenanceProperties.getLimits().setMaxFieldLength(3);

        assertInvalid(new BackfillJobSubmission(JOB_ID, "tpl", List.of(
                entry("id", value("key", "éé", null))), null));
    }

    @Test
    void submit_rejectsTooManyProcessDataValuesAcrossJob() {
        maintenanceProperties.getLimits().setMaxProcessDataValuesPerJob(2);

        assertInvalid(submission(List.of(
                entry("process-a", value("key-a", "value-a", null), value("key-b", "value-b", null)),
                entry("process-b", value("key-c", "value-c", null)))));
    }

    @Test
    void maintenanceLimits_haveBoundedRequestDefaults() {
        MaintenanceProperties.Limits limits = new MaintenanceProperties().getLimits();

        assertThat(limits.getMaxProcessDataValuesPerJob()).isEqualTo(10_000);
        assertThat(limits.getMaxRequestBytes()).isEqualTo(10 * 1024 * 1024);
    }

    @Test
    void get_reevaluationJob_returnsEmpty() {
        MaintenanceJob job = MaintenanceJob.createReevaluation(new ReevaluationJobSubmission(
                JOB_ID, "assessmentProcess", List.of("process-a"), null)
                .normalized(maintenanceProperties.getLimits()));
        when(maintenanceJobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));

        assertThat(service.get(JOB_ID)).isEmpty();
    }

    private void assertInvalid(BackfillJobSubmission submission) {
        assertThatThrownBy(() -> service.submit(submission))
                .isInstanceOf(MaintenanceJobException.class)
                .extracting("reason")
                .isEqualTo(MaintenanceJobExceptionReason.INVALID_REQUEST);
        verifyNoInteractions(maintenanceJobRepository);
    }

    private BackfillJobSubmission submission(List<BackfillJobEntry> entries) {
        return new BackfillJobSubmission(JOB_ID, "assessmentProcess", entries,
                new MaintenanceJobSubmitter("John Doe", "287365"));
    }

    private static BackfillJobEntry entry(String originProcessId, ProcessDataValue... values) {
        return new BackfillJobEntry(originProcessId, List.of(values));
    }

    private static ProcessDataValue value(String key, String value, String role) {
        return new ProcessDataValue(key, value, role);
    }
}
