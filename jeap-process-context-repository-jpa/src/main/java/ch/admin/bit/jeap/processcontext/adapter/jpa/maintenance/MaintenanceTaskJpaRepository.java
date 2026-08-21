package ch.admin.bit.jeap.processcontext.adapter.jpa.maintenance;

import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTaskState;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface MaintenanceTaskJpaRepository extends JpaRepository<MaintenanceTaskEntity, UUID> {
    List<MaintenanceTaskEntity> findByJobIdOrderByTargetKey(UUID jobId);

    long countByJobId(UUID jobId);

    long countByJobIdAndTaskStateIn(UUID jobId, List<MaintenanceTaskState> states);

    long countByJobIdAndTaskState(UUID jobId, MaintenanceTaskState state);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from MaintenanceTaskEntity t where t.taskId = :taskId")
    Optional<MaintenanceTaskEntity> findByIdForUpdate(@Param("taskId") UUID taskId);
}
