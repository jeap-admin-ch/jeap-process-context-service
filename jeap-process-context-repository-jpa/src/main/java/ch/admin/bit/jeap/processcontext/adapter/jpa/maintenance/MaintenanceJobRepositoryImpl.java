package ch.admin.bit.jeap.processcontext.adapter.jpa.maintenance;

import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJob;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobAlreadyExistsException;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobRepository;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTask;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTaskCounts;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTaskState;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessDataValue;
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
    private final MaintenanceProcessDataJpaRepository maintenanceProcessDataJpaRepository;
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
        job.tasks().forEach(task -> task.processData().stream()
                .map(value -> MaintenanceProcessDataEntity.fromDomain(task.taskId(), value))
                .forEach(entityManager::persist));
        entityManager.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MaintenanceJob> findById(UUID jobId) {
        return maintenanceJobJpaRepository.findById(jobId)
                .map(job -> job.toDomain(loadTaskMetadata(jobId)));
    }

    @Override
    @Transactional
    public Optional<MaintenanceJob> findByTaskIdForUpdate(UUID taskId) {
        return maintenanceTaskJpaRepository.findByIdForUpdate(taskId).flatMap(task ->
                maintenanceJobJpaRepository.findById(task.getJobId())
                        .map(job -> job.toDomain(List.of(loadTaskWithProcessData(task)))));
    }

    @Override
    @Transactional
    public void updateTask(MaintenanceJob job, MaintenanceTask task) {
        applyTask(job, task);
    }

    @Override
    @Transactional
    public void updateTaskAndJob(MaintenanceJob job, MaintenanceTask task) {
        applyTask(job, task);
        entityManager.flush();

        MaintenanceJobEntity jobEntity = maintenanceJobJpaRepository.findByIdForUpdate(job.jobId())
                .orElseThrow(() -> new IllegalArgumentException("Maintenance job not found"));
        jobEntity.apply(jobEntity.toDomain(List.of(task))
                .completeIfAllTasksTerminal(Instant.now(), taskCounts(job.jobId())));
    }

    private void applyTask(MaintenanceJob job, MaintenanceTask task) {
        maintenanceTaskJpaRepository.findById(task.taskId())
                .filter(entity -> entity.getJobId().equals(job.jobId()))
                .orElseThrow(() -> new IllegalArgumentException("Maintenance task not found"))
                .apply(task);
    }

    private MaintenanceTaskCounts taskCounts(UUID jobId) {
        long total = maintenanceTaskJpaRepository.countByJobId(jobId);
        long terminal = maintenanceTaskJpaRepository.countByJobIdAndTaskStateIn(jobId, List.of(
                MaintenanceTaskState.SUCCEEDED, MaintenanceTaskState.NOT_FOUND, MaintenanceTaskState.FAILED));
        long succeeded = maintenanceTaskJpaRepository.countByJobIdAndTaskState(
                jobId, MaintenanceTaskState.SUCCEEDED);
        return new MaintenanceTaskCounts(total, terminal, succeeded);
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

    private List<MaintenanceTask> loadTaskMetadata(UUID jobId) {
        return maintenanceTaskJpaRepository.findByJobIdOrderByTargetKey(jobId).stream()
                .map(task -> task.toDomain(List.of()))
                .toList();
    }

    private MaintenanceTask loadTaskWithProcessData(MaintenanceTaskEntity task) {
        List<ProcessDataValue> targetProcessData =
                maintenanceProcessDataJpaRepository.findByTaskId(task.getTaskId()).stream()
                        .map(MaintenanceProcessDataEntity::toDomain)
                        .toList();
        return task.toDomain(targetProcessData);
    }
}
