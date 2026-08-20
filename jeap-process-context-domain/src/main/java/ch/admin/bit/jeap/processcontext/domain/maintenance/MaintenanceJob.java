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
                        ? transition(task, state, errorMessage, errorTraceId, now)
                        : task)
                .toList();
        boolean completed = updatedTasks.stream().allMatch(task -> task.taskState().isTerminal());
        return new MaintenanceJob(jobId, jobType, processTemplateName, requestHash,
                completed ? MaintenanceJobState.COMPLETED : MaintenanceJobState.OPEN,
                completed ? result(updatedTasks) : null, startedAt, completed ? now : null,
                startedByName, startedByExtId, updatedTasks);
    }

    private static MaintenanceTask transition(MaintenanceTask task, MaintenanceTaskState state,
                                              String errorMessage, String errorTraceId, Instant now) {
        return new MaintenanceTask(task.taskId(), task.targetType(), task.targetKey(), task.originProcessId(), state,
                task.createdAt(), now, errorMessage, errorTraceId);
    }

    private static MaintenanceJobResult result(List<MaintenanceTask> tasks) {
        long succeeded = tasks.stream().filter(task -> task.taskState() == MaintenanceTaskState.SUCCEEDED).count();
        if (succeeded == tasks.size()) {
            return MaintenanceJobResult.SUCCEEDED;
        }
        return succeeded == 0 ? MaintenanceJobResult.FAILED : MaintenanceJobResult.PARTIALLY_SUCCEEDED;
    }
}
