package ch.admin.bit.jeap.processcontext.adapter.jpa.maintenance;

import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJob;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobAlreadyExistsException;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobRepository;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTask;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTaskCounts;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTaskState;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
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
        } catch (PersistenceException e) {
            if (isUniqueConstraintViolation(e)) {
                throw new MaintenanceJobAlreadyExistsException(e);
            }
            throw e;
        }
        job.tasks().stream()
                .map(task -> MaintenanceTaskEntity.fromDomain(job.jobId(), task))
                .forEach(entityManager::persist);
        entityManager.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MaintenanceJob> findById(UUID jobId) {
        return maintenanceJobJpaRepository.findById(jobId)
                .map(job -> job.toDomain(maintenanceTaskJpaRepository.findByJobIdOrderByTargetKey(jobId).stream()
                        .map(MaintenanceTaskEntity::toDomain)
                        .toList()));
    }

    @Override
    @Transactional
    public Optional<MaintenanceJob> findTaskForUpdate(UUID taskId) {
        return maintenanceTaskJpaRepository.findByIdForUpdate(taskId).flatMap(task ->
                maintenanceJobJpaRepository.findById(task.getJobId())
                        .map(job -> job.toDomain(List.of(task.toDomain()))));
    }

    @Override
    @Transactional
    public void updateTaskAndJob(MaintenanceJob job, MaintenanceTask task) {
        MaintenanceTaskEntity taskEntity = maintenanceTaskJpaRepository.findById(task.taskId())
                .filter(entity -> entity.getJobId().equals(job.jobId()))
                .orElseThrow(() -> new IllegalArgumentException("Maintenance task not found"));
        taskEntity.apply(task);
        entityManager.flush();

        MaintenanceJobEntity jobEntity = maintenanceJobJpaRepository.findByIdForUpdate(job.jobId())
                .orElseThrow(() -> new IllegalArgumentException("Maintenance job not found"));
        MaintenanceTaskCounts counts = new MaintenanceTaskCounts(
                maintenanceTaskJpaRepository.countByJobId(job.jobId()),
                maintenanceTaskJpaRepository.countByJobIdAndTaskStateIn(job.jobId(), List.of(
                        MaintenanceTaskState.SUCCEEDED, MaintenanceTaskState.NOT_FOUND, MaintenanceTaskState.FAILED)),
                maintenanceTaskJpaRepository.countByJobIdAndTaskState(
                        job.jobId(), MaintenanceTaskState.SUCCEEDED));
        MaintenanceJob currentJob = jobEntity.toDomain(List.of(task));
        jobEntity.apply(currentJob.completeIfAllTasksTerminal(Instant.now(), counts));
    }

    @Override
    @Transactional
    public int deleteCompletedBefore(Instant completedBefore, int limit) {
        List<UUID> jobIds = maintenanceJobJpaRepository.findCompletedIdsBefore(
                completedBefore, PageRequest.of(0, limit));
        maintenanceJobJpaRepository.deleteAllByIdInBatch(jobIds);
        return jobIds.size();
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
