package ch.admin.bit.jeap.processcontext.domain.maintenance;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface MaintenanceJobRepository {
    void create(MaintenanceJob job);

    Optional<MaintenanceJob> findById(UUID jobId);

    Optional<MaintenanceJob> findByTaskIdForUpdate(UUID taskId);

    void updateTask(MaintenanceJob job, MaintenanceTask task);

    void updateTaskAndJob(MaintenanceJob job, MaintenanceTask task);

    int deleteCompletedBefore(Instant completedBefore, int limit);
}
