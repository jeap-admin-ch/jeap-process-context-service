package ch.admin.bit.jeap.processcontext.domain.maintenance;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessDataValue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MaintenanceTask(
        UUID taskId,
        MaintenanceTargetType targetType,
        String targetKey,
        String originProcessId,
        MaintenanceTaskState taskState,
        Instant createdAt,
        Instant modifiedAt,
        String errorMessage,
        String errorTraceId,
        List<ProcessDataValue> processData) {

    public MaintenanceTask {
        processData = ProcessDataValue.canonicalize(processData);
    }

    public MaintenanceTask(UUID taskId, MaintenanceTargetType targetType, String targetKey, String originProcessId,
                           MaintenanceTaskState taskState, Instant createdAt, Instant modifiedAt,
                           String errorMessage, String errorTraceId) {
        this(taskId, targetType, targetKey, originProcessId, taskState, createdAt, modifiedAt,
                errorMessage, errorTraceId, List.of());
    }

    public MaintenanceTask transitionTo(MaintenanceTaskState state, String errorMessage, String errorTraceId,
                                         Instant now) {
        return new MaintenanceTask(taskId, targetType, targetKey, originProcessId, state, createdAt, now,
                errorMessage, errorTraceId, processData);
    }

    static MaintenanceTask reevaluation(UUID taskId, String originProcessId, Instant now) {
        return new MaintenanceTask(taskId, MaintenanceTargetType.PROCESS, originProcessId, originProcessId,
                MaintenanceTaskState.EVENT_QUEUED, now, null, null, null);
    }

    static MaintenanceTask backfill(UUID taskId, String originProcessId, List<ProcessDataValue> processData,
                                    Instant now) {
        return new MaintenanceTask(taskId, MaintenanceTargetType.PROCESS, originProcessId, originProcessId,
                MaintenanceTaskState.COMMAND_QUEUED, now, null, null, null, processData);
    }
}
