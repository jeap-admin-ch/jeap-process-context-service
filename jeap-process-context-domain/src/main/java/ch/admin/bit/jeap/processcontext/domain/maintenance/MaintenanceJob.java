package ch.admin.bit.jeap.processcontext.domain.maintenance;

import com.fasterxml.uuid.Generators;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MaintenanceJob(
        UUID jobId,
        MaintenanceJobType jobType,
        String processTemplateName,
        String requestHash,
        MaintenanceJobState jobState,
        MaintenanceJobResult jobResult,
        Instant startedAt,
        Instant completedAt,
        String startedByName,
        String startedByExtId,
        List<MaintenanceTask> tasks) {

    public MaintenanceJob {
        tasks = List.copyOf(tasks);
    }

    static MaintenanceJob createReevaluation(NormalizedReevaluationJobSubmission submission) {
        Instant now = Instant.now();
        MaintenanceJobSubmitter submitter = submission.submitter() == null
                ? new MaintenanceJobSubmitter(null, null)
                : submission.submitter();
        List<MaintenanceTask> tasks = submission.originProcessIds().stream()
                .map(originProcessId -> new MaintenanceTask(
                        Generators.timeBasedEpochGenerator().generate(),
                        MaintenanceTargetType.PROCESS,
                        originProcessId,
                        originProcessId,
                        MaintenanceTaskState.CREATED,
                        now,
                        null,
                        null,
                        null))
                .toList();
        return new MaintenanceJob(
                submission.jobId(),
                MaintenanceJobType.RELATION_REEVALUATION,
                submission.processTemplateName(),
                submission.requestHash(),
                MaintenanceJobState.OPEN,
                null,
                now,
                null,
                submitter.name(),
                submitter.extId(),
                tasks);
    }

    boolean hasSameRequest(NormalizedReevaluationJobSubmission submission) {
        return jobType == MaintenanceJobType.RELATION_REEVALUATION && requestHash.equals(submission.requestHash());
    }
}
