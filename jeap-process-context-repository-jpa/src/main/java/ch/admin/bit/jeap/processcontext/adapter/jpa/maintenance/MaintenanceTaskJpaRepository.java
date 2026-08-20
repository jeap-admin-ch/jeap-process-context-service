package ch.admin.bit.jeap.processcontext.adapter.jpa.maintenance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface MaintenanceTaskJpaRepository extends JpaRepository<MaintenanceTaskEntity, UUID> {
    List<MaintenanceTaskEntity> findByJobIdOrderByTargetKey(UUID jobId);
}
