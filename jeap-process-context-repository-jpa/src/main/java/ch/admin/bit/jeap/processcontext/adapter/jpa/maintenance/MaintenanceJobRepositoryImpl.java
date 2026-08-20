package ch.admin.bit.jeap.processcontext.adapter.jpa.maintenance;

import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJob;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobAlreadyExistsException;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(value = "jeap.processcontext.maintenance.enabled", havingValue = "true")
@RequiredArgsConstructor
class MaintenanceJobRepositoryImpl implements MaintenanceJobRepository {

    private final MaintenanceJobJpaRepository maintenanceJobJpaRepository;
    private final MaintenanceTaskJpaRepository maintenanceTaskJpaRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public void create(MaintenanceJob job) {
        try {
            entityManager.persist(MaintenanceJobEntity.fromDomain(job));
            entityManager.flush();
            job.tasks().stream()
                    .map(task -> MaintenanceTaskEntity.fromDomain(job.jobId(), task))
                    .forEach(entityManager::persist);
            entityManager.flush();
        } catch (PersistenceException e) {
            if (isUniqueConstraintViolation(e)) {
                throw new MaintenanceJobAlreadyExistsException(e);
            }
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MaintenanceJob> findById(UUID jobId) {
        return maintenanceJobJpaRepository.findById(jobId)
                .map(job -> job.toDomain(maintenanceTaskJpaRepository.findByJobIdOrderByTargetKey(jobId).stream()
                        .map(MaintenanceTaskEntity::toDomain)
                        .toList()));
    }

    private static boolean isUniqueConstraintViolation(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException && "23505".equals(sqlException.getSQLState())) {
                return true;
            }
        }
        return false;
    }
}
