package ch.admin.bit.jeap.processcontext.domain.maintenance;

import java.time.Instant;
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
        String errorTraceId) {

    public MaintenanceTask transitionTo(MaintenanceTaskState state, String errorMessage, String errorTraceId,
                                        Instant now) {
        return new MaintenanceTask(taskId, targetType, targetKey, originProcessId, state, createdAt, now,
                errorMessage, errorTraceId);
    }
}
