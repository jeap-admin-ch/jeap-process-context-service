package ch.admin.bit.jeap.processcontext.domain.maintenance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReevaluationJobServiceTest {

    private static final UUID JOB_ID = UUID.fromString("88dbb65f-9634-4685-bc86-17b72d715d3e");

    @Mock
    private MaintenanceJobRepository maintenanceJobRepository;

    private MaintenanceProperties maintenanceProperties;
    private ReevaluationJobService service;

    @BeforeEach
    void setUp() {
        maintenanceProperties = new MaintenanceProperties();
        maintenanceProperties.getLimits().setMaxTasksPerJob(10);
        maintenanceProperties.getLimits().setMaxFieldLength(50);
        service = new ReevaluationJobService(maintenanceJobRepository, maintenanceProperties);
    }

    @Test
    void submit_validRequest_normalizesSortsAndPersistsCreatedTasks() {
        when(maintenanceJobRepository.findById(JOB_ID)).thenReturn(Optional.empty());

        service.submit(new ReevaluationJobSubmission(
                JOB_ID,
                " assessmentProcess ",
                List.of(" assessment-4712 ", "assessment-4711"),
                new MaintenanceJobSubmitter("John Doe", "287365")));

        ArgumentCaptor<MaintenanceJob> captor = ArgumentCaptor.forClass(MaintenanceJob.class);
        verify(maintenanceJobRepository).create(captor.capture());
        MaintenanceJob job = captor.getValue();
        assertThat(job.jobId()).isEqualTo(JOB_ID);
        assertThat(job.jobType()).isEqualTo(MaintenanceJobType.RELATION_REEVALUATION);
        assertThat(job.processTemplateName()).isEqualTo("assessmentProcess");
        assertThat(job.requestHash()).matches("[0-9a-f]{64}");
        assertThat(job.jobState()).isEqualTo(MaintenanceJobState.OPEN);
        assertThat(job.jobResult()).isNull();
        assertThat(job.startedAt()).isNotNull();
        assertThat(job.completedAt()).isNull();
        assertThat(job.startedByName()).isEqualTo("John Doe");
        assertThat(job.startedByExtId()).isEqualTo("287365");
        assertThat(job.tasks())
                .extracting(MaintenanceTask::originProcessId)
                .containsExactly("assessment-4711", "assessment-4712");
        assertThat(job.tasks())
                .allSatisfy(task -> {
                    assertThat(task.taskId()).isNotNull();
                    assertThat(task.targetType()).isEqualTo(MaintenanceTargetType.PROCESS);
                    assertThat(task.targetKey()).isEqualTo(task.originProcessId());
                    assertThat(task.taskState()).isEqualTo(MaintenanceTaskState.CREATED);
                    assertThat(task.createdAt()).isNotNull();
                    assertThat(task.modifiedAt()).isNull();
                    assertThat(task.errorMessage()).isNull();
                    assertThat(task.errorTraceId()).isNull();
                });
        assertThat(job.tasks()).extracting(MaintenanceTask::taskId).doesNotHaveDuplicates();
    }

    @Test
    void submit_sameNormalizedInput_isIdempotentAndIgnoresSubmitter() {
        ReevaluationJobSubmission first = submission("assessmentProcess", List.of("process-b", "process-a"));
        MaintenanceJob existing = MaintenanceJob.createReevaluation(first.normalized(maintenanceProperties.getLimits()));
        when(maintenanceJobRepository.findById(JOB_ID)).thenReturn(Optional.of(existing));

        service.submit(new ReevaluationJobSubmission(
                JOB_ID,
                " assessmentProcess ",
                List.of(" process-a ", "process-b"),
                new MaintenanceJobSubmitter("Another User", "999")));

        verify(maintenanceJobRepository, never()).create(any());
    }

    @Test
    void submit_sameJobIdWithDifferentInput_throwsConflict() {
        ReevaluationJobSubmission first = submission("assessmentProcess", List.of("process-a"));
        MaintenanceJob existing = MaintenanceJob.createReevaluation(first.normalized(maintenanceProperties.getLimits()));
        when(maintenanceJobRepository.findById(JOB_ID)).thenReturn(Optional.of(existing));
        ReevaluationJobSubmission differentSubmission = submission("assessmentProcess", List.of("process-b"));

        assertThatThrownBy(() -> service.submit(differentSubmission))
                .isInstanceOf(MaintenanceJobException.class)
                .extracting("reason")
                .isEqualTo(MaintenanceJobExceptionReason.CONFLICT);

        verify(maintenanceJobRepository, never()).create(any());
    }

    @Test
    void submit_concurrentEquivalentCreation_convergesToSuccess() {
        ReevaluationJobSubmission submission = submission("assessmentProcess", List.of("process-a"));
        MaintenanceJob existing = MaintenanceJob.createReevaluation(submission.normalized(maintenanceProperties.getLimits()));
        when(maintenanceJobRepository.findById(JOB_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));
        doThrow(MaintenanceJobAlreadyExistsException.class).when(maintenanceJobRepository).create(any());

        service.submit(submission);

        verify(maintenanceJobRepository).create(any());
        verify(maintenanceJobRepository, times(2)).findById(JOB_ID);
    }

    @Test
    void submit_concurrentDifferentCreation_throwsConflict() {
        ReevaluationJobSubmission submission = submission("assessmentProcess", List.of("process-a"));
        MaintenanceJob existing = MaintenanceJob.createReevaluation(
                submission("assessmentProcess", List.of("process-b")).normalized(maintenanceProperties.getLimits()));
        when(maintenanceJobRepository.findById(JOB_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));
        doThrow(MaintenanceJobAlreadyExistsException.class).when(maintenanceJobRepository).create(any());

        assertThatThrownBy(() -> service.submit(submission))
                .isInstanceOf(MaintenanceJobException.class)
                .extracting("reason")
                .isEqualTo(MaintenanceJobExceptionReason.CONFLICT);
    }

    @Test
    void submit_duplicateProcessIdAfterNormalization_throwsInvalidRequest() {
        assertInvalid(submission("assessmentProcess", List.of("process-a", " process-a ")));
    }

    @Test
    void submit_blankTemplate_throwsInvalidRequest() {
        assertInvalid(submission(" ", List.of("process-a")));
    }

    @Test
    void submit_blankProcessId_throwsInvalidRequest() {
        assertInvalid(submission("assessmentProcess", List.of(" ")));
    }

    @Test
    void submit_tooManyProcesses_throwsInvalidRequest() {
        maintenanceProperties.getLimits().setMaxTasksPerJob(1);
        assertInvalid(submission("assessmentProcess", List.of("process-a", "process-b")));
    }

    @Test
    void submit_fieldTooLong_throwsInvalidRequest() {
        maintenanceProperties.getLimits().setMaxFieldLength(5);
        assertInvalid(submission("assessmentProcess", List.of("process-a")));
    }

    @Test
    void submit_multiByteFieldExceedsByteLimit_throwsInvalidRequest() {
        maintenanceProperties.getLimits().setMaxFieldLength(3);
        assertInvalid(submission("abc", List.of("éé")));
    }

    @Test
    void get_returnsPersistedJob() {
        MaintenanceJob job = MaintenanceJob.createReevaluation(
                submission("assessmentProcess", List.of("process-a")).normalized(maintenanceProperties.getLimits()));
        when(maintenanceJobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));

        assertThat(service.get(JOB_ID)).containsSame(job);
    }

    private void assertInvalid(ReevaluationJobSubmission submission) {
        assertThatThrownBy(() -> service.submit(submission))
                .isInstanceOf(MaintenanceJobException.class)
                .extracting("reason")
                .isEqualTo(MaintenanceJobExceptionReason.INVALID_REQUEST);
        verifyNoInteractions(maintenanceJobRepository);
    }

    private ReevaluationJobSubmission submission(String templateName, List<String> processIds) {
        return new ReevaluationJobSubmission(
                JOB_ID,
                templateName,
                processIds,
                new MaintenanceJobSubmitter("John Doe", "287365"));
    }
}
