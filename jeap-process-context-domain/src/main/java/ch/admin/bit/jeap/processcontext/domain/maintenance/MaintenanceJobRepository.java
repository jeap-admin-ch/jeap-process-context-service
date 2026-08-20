package ch.admin.bit.jeap.processcontext.domain.maintenance;

import java.util.Optional;
import java.util.UUID;

public interface MaintenanceJobRepository {
    void create(MaintenanceJob job);

    Optional<MaintenanceJob> findById(UUID jobId);
}
