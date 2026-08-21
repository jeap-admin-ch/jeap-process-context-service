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
                        MaintenanceTaskState.EVENT_QUEUED,
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

    public MaintenanceTask task(UUID taskId) {
        return tasks.stream()
                .filter(task -> task.taskId().equals(taskId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Maintenance task does not belong to job"));
    }

    public MaintenanceJob transitionTask(UUID taskId, MaintenanceTaskState state, String errorMessage, Instant now) {
        return transitionTask(taskId, state, errorMessage, null, now);
    }

    public MaintenanceJob transitionTask(UUID taskId, MaintenanceTaskState state, String errorMessage,
                                         String errorTraceId, Instant now) {
        List<MaintenanceTask> updatedTasks = tasks.stream()
                .map(task -> task.taskId().equals(taskId)
                        ? task.transitionTo(state, errorMessage, errorTraceId, now)
                        : task)
                .toList();
        return withTasks(updatedTasks).completeIfAllTasksTerminal(now);
    }

    public MaintenanceJob completeIfAllTasksTerminal(Instant now) {
        long terminal = tasks.stream().filter(task -> task.taskState().isTerminal()).count();
        long succeeded = tasks.stream().filter(task -> task.taskState() == MaintenanceTaskState.SUCCEEDED).count();
        return completeIfAllTasksTerminal(now, new MaintenanceTaskCounts(tasks.size(), terminal, succeeded));
    }

    public MaintenanceJob completeIfAllTasksTerminal(Instant now, MaintenanceTaskCounts counts) {
        boolean completed = counts.terminal() == counts.total();
        if (!completed) {
            return this;
        }
        return new MaintenanceJob(jobId, jobType, processTemplateName, requestHash, MaintenanceJobState.COMPLETED,
                result(counts.total(), counts.succeeded()), startedAt, now, startedByName, startedByExtId, tasks);
    }

    private MaintenanceJob withTasks(List<MaintenanceTask> updatedTasks) {
        return new MaintenanceJob(jobId, jobType, processTemplateName, requestHash, jobState, jobResult, startedAt,
                completedAt, startedByName, startedByExtId, updatedTasks);
    }

    private static MaintenanceJobResult result(long total, long succeeded) {
        if (succeeded == total) {
            return MaintenanceJobResult.SUCCEEDED;
        }
        return succeeded == 0 ? MaintenanceJobResult.FAILED : MaintenanceJobResult.PARTIALLY_SUCCEEDED;
    }
}
