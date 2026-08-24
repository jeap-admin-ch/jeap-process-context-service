package ch.admin.bit.jeap.processcontext.adapter.jpa.maintenance;

import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTargetType;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTask;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTaskState;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessDataValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pcs_maintenance_task")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class MaintenanceTaskEntity {

    @Id
    private UUID taskId;
    private UUID jobId;
    @Enumerated(EnumType.STRING)
    private MaintenanceTargetType targetType;
    private String targetKey;
    private String originProcessId;
    @Enumerated(EnumType.STRING)
    private MaintenanceTaskState taskState;
    private Instant createdAt;
    private Instant modifiedAt;
    private String errorMessage;
    private String errorTraceId;
    @Version
    private int version;

    static MaintenanceTaskEntity fromDomain(UUID jobId, MaintenanceTask task) {
        return new MaintenanceTaskEntity(
                task.taskId(),
                jobId,
                task.targetType(),
                task.targetKey(),
                task.originProcessId(),
                task.taskState(),
                task.createdAt(),
                task.modifiedAt(),
                task.errorMessage(),
                task.errorTraceId(),
                0);
    }

    MaintenanceTask toDomain(List<ProcessDataValue> processData) {
        return new MaintenanceTask(
                taskId,
                targetType,
                targetKey,
                originProcessId,
                taskState,
                createdAt,
                modifiedAt,
                errorMessage,
                errorTraceId,
                processData);
    }

    UUID getJobId() {
        return jobId;
    }

    UUID getTaskId() {
        return taskId;
    }

    void apply(MaintenanceTask task) {
        this.taskState = task.taskState();
        this.modifiedAt = task.modifiedAt();
        this.errorMessage = task.errorMessage();
        this.errorTraceId = task.errorTraceId();
    }
}
