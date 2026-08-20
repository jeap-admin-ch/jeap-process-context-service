package ch.admin.bit.jeap.processcontext.adapter.jpa.maintenance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface MaintenanceJobJpaRepository extends JpaRepository<MaintenanceJobEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from MaintenanceJobEntity j where j.jobId = :jobId")
    Optional<MaintenanceJobEntity> findByIdForUpdate(@Param("jobId") UUID jobId);

    @Query("select j.jobId from MaintenanceJobEntity j " +
            "where j.jobState = ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobState.COMPLETED " +
            "and j.completedAt < :completedBefore order by j.completedAt")
    List<UUID> findCompletedIdsBefore(@Param("completedBefore") Instant completedBefore, Pageable pageable);
}
