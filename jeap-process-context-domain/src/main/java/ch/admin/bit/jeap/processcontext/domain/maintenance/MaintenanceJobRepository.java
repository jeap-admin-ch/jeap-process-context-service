package ch.admin.bit.jeap.processcontext.domain.maintenance;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MaintenanceJobRepository {
    void create(MaintenanceJob job);

    Optional<MaintenanceJob> findById(UUID jobId);

    List<UUID> findTaskIdsByState(MaintenanceTaskState state, int limit);

    Optional<MaintenanceJob> findByTaskIdForUpdate(UUID taskId);

    void update(MaintenanceJob job);

    int deleteCompletedBefore(Instant completedBefore, int limit);
}
