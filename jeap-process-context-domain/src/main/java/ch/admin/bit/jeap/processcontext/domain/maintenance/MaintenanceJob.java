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
                .map(originProcessId -> MaintenanceTask.reevaluation(
                        Generators.timeBasedEpochGenerator().generate(), originProcessId, now))
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

    static MaintenanceJob createBackfill(NormalizedBackfillJobSubmission submission) {
        Instant now = Instant.now();
        MaintenanceJobSubmitter submitter = submission.submitter() == null
                ? new MaintenanceJobSubmitter(null, null)
                : submission.submitter();
        List<MaintenanceTask> tasks = submission.entries().stream()
                .map(entry -> MaintenanceTask.backfill(
                        Generators.timeBasedEpochGenerator().generate(), entry.originProcessId(), entry.processData(), now))
                .toList();
        return new MaintenanceJob(
                submission.jobId(),
                MaintenanceJobType.PROCESS_DATA_BACKFILL,
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

    boolean hasSameRequest(NormalizedBackfillJobSubmission submission) {
        return jobType == MaintenanceJobType.PROCESS_DATA_BACKFILL && requestHash.equals(submission.requestHash());
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
        if (state.isTerminal()) {
            throw new IllegalArgumentException("Terminal task transitions require task counts");
        }
        List<MaintenanceTask> updatedTasks = tasks.stream()
                .map(task -> task.taskId().equals(taskId)
                        ? task.transitionTo(state, errorMessage, errorTraceId, now)
                        : task)
                .toList();
        return new MaintenanceJob(jobId, jobType, processTemplateName, requestHash,
                MaintenanceJobState.OPEN, null, startedAt, null,
                startedByName, startedByExtId, updatedTasks);
    }

    public MaintenanceJob transitionTask(UUID taskId, MaintenanceTaskState state, String errorMessage, Instant now,
                                         MaintenanceTaskCounts counts) {
        return transitionTask(taskId, state, errorMessage, null, now, counts);
    }

    public MaintenanceJob transitionTask(UUID taskId, MaintenanceTaskState state, String errorMessage,
                                         String errorTraceId, Instant now, MaintenanceTaskCounts counts) {
        if (!state.isTerminal()) {
            throw new IllegalArgumentException("Task-count transitions require a terminal state");
        }
        MaintenanceTask oldTask = task(taskId);
        List<MaintenanceTask> updatedTasks = tasks.stream()
                .map(task -> task.taskId().equals(taskId)
                        ? task.transitionTo(state, errorMessage, errorTraceId, now)
                        : task)
                .toList();
        long terminal = counts.terminal() - (oldTask.taskState().isTerminal() ? 1 : 0) + 1;
        long succeeded = counts.succeeded()
                - (oldTask.taskState() == MaintenanceTaskState.SUCCEEDED ? 1 : 0)
                + (state == MaintenanceTaskState.SUCCEEDED ? 1 : 0);
        boolean completed = terminal == counts.total();
        return new MaintenanceJob(jobId, jobType, processTemplateName, requestHash,
                completed ? MaintenanceJobState.COMPLETED : MaintenanceJobState.OPEN,
                completed ? result(counts.total(), succeeded) : null, startedAt, completed ? now : null,
                startedByName, startedByExtId, updatedTasks);
    }

    public MaintenanceJob completeIfAllTasksTerminal(Instant now, MaintenanceTaskCounts counts) {
        if (counts.terminal() != counts.total()) {
            return this;
        }
        return new MaintenanceJob(jobId, jobType, processTemplateName, requestHash, MaintenanceJobState.COMPLETED,
                result(counts.total(), counts.succeeded()), startedAt, now, startedByName, startedByExtId, tasks);
    }

    private static MaintenanceJobResult result(long total, long succeeded) {
        if (succeeded == total) {
            return MaintenanceJobResult.SUCCEEDED;
        }
        return succeeded == 0 ? MaintenanceJobResult.FAILED : MaintenanceJobResult.PARTIALLY_SUCCEEDED;
    }
}
