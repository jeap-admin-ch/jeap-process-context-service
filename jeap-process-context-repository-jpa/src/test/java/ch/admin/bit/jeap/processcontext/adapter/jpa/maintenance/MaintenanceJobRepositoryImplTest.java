package ch.admin.bit.jeap.processcontext.adapter.jpa.maintenance;

import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJob;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobAlreadyExistsException;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobState;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobType;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTargetType;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTask;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTaskState;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessDataValue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class MaintenanceJobRepositoryImplTest {

    private final MaintenanceJobJpaRepository jobJpaRepository = mock(MaintenanceJobJpaRepository.class);
    private final MaintenanceTaskJpaRepository taskJpaRepository = mock(MaintenanceTaskJpaRepository.class);
    private final MaintenanceProcessDataJpaRepository processDataJpaRepository =
            mock(MaintenanceProcessDataJpaRepository.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final MaintenanceJobRepositoryImpl repository =
            new MaintenanceJobRepositoryImpl(jobJpaRepository, taskJpaRepository, processDataJpaRepository, entityManager);

    @Test
    void create_uniqueConstraintViolation_reportsAlreadyExists() {
        PersistenceException exception = new PersistenceException(new SQLException("duplicate", "23505"));
        doThrow(exception).when(entityManager).flush();
        MaintenanceJob maintenanceJob = job();

        assertThatThrownBy(() -> repository.create(maintenanceJob))
                .isInstanceOf(MaintenanceJobAlreadyExistsException.class)
                .hasCause(exception);
    }

    @Test
    void create_otherPersistenceFailure_isNotMisclassified() {
        PersistenceException exception = new PersistenceException(new SQLException("connection failed", "08006"));
        doThrow(exception).when(entityManager).flush();
        MaintenanceJob maintenanceJob = job();

        assertThatThrownBy(() -> repository.create(maintenanceJob)).isSameAs(exception);
    }

    @Test
    void create_taskConstraintViolation_isNotMisclassifiedAsExistingJob() {
        PersistenceException exception = new PersistenceException(new SQLException("duplicate task", "23505"));
        doNothing().doThrow(exception).when(entityManager).flush();
        MaintenanceJob maintenanceJob = job();

        assertThatThrownBy(() -> repository.create(maintenanceJob)).isSameAs(exception);
    }

    @Test
    void findById_loadsTaskMetadataWithoutQueryingProcessData() {
        UUID taskId = UUID.fromString("019c8c72-6fd1-7f25-a9a1-3b3d51fbb321");
        MaintenanceTask task = new MaintenanceTask(taskId, MaintenanceTargetType.PROCESS, "assessment-4711",
                "assessment-4711", MaintenanceTaskState.COMMAND_QUEUED, Instant.parse("2026-08-06T08:03:12Z"),
                null, null, null);
        MaintenanceJob job = job(List.of(task));
        when(jobJpaRepository.findById(job.jobId())).thenReturn(Optional.of(MaintenanceJobEntity.fromDomain(job)));
        when(taskJpaRepository.findByJobIdOrderByTargetKey(job.jobId()))
                .thenReturn(List.of(MaintenanceTaskEntity.fromDomain(job.jobId(), task)));

        MaintenanceJob loaded = repository.findById(job.jobId()).orElseThrow();

        assertThat(loaded.task(taskId).processData()).isEmpty();
        verifyNoInteractions(processDataJpaRepository);
    }

    @Test
    void taskEntity_preservesRelationIdAcrossMappingAndTransition() {
        UUID relationId = UUID.fromString("019c8c72-6fd1-7f25-a9a1-3b3d51fbb322");
        Instant now = Instant.parse("2026-08-06T08:03:12Z");
        MaintenanceTask task = new MaintenanceTask(UUID.randomUUID(), MaintenanceTargetType.RELATION,
                relationId.toString(), null, relationId, MaintenanceTaskState.EVENT_QUEUED,
                now, null, null, null, List.of());

        MaintenanceTask mapped = MaintenanceTaskEntity.fromDomain(UUID.randomUUID(), task).toDomain(List.of())
                .transitionTo(MaintenanceTaskState.SUCCEEDED, null, null, now.plusSeconds(1));

        assertThat(mapped.relationId()).isEqualTo(relationId);
        assertThat(mapped.targetKey()).isEqualTo(relationId.toString());
    }

    @Test
    void findByTaskIdForUpdate_loadsOnlyTargetTaskAndItsProcessData() {
        UUID taskId = UUID.fromString("019c8c72-6fd1-7f25-a9a1-3b3d51fbb321");
        MaintenanceTask task = new MaintenanceTask(taskId, MaintenanceTargetType.PROCESS, "assessment-4711",
                "assessment-4711", MaintenanceTaskState.EVENT_QUEUED, Instant.parse("2026-08-06T08:03:12Z"),
                null, null, null);
        MaintenanceJob job = job(List.of(task));
        MaintenanceTaskEntity taskEntity = MaintenanceTaskEntity.fromDomain(job.jobId(), task);
        ProcessDataValue processData = new ProcessDataValue("assessmentId", "a-123", null);
        when(jobJpaRepository.findById(job.jobId()))
                .thenReturn(Optional.of(MaintenanceJobEntity.fromDomain(job)));
        when(taskJpaRepository.findByIdForUpdate(taskId)).thenReturn(Optional.of(taskEntity));
        when(processDataJpaRepository.findByTaskId(taskId))
                .thenReturn(List.of(MaintenanceProcessDataEntity.fromDomain(taskId, processData)));

        MaintenanceJob loaded = repository.findByTaskIdForUpdate(taskId).orElseThrow();

        assertThat(loaded.tasks()).hasSize(1);
        assertThat(loaded.task(taskId).processData()).containsExactly(processData);
        verify(taskJpaRepository, never()).findByJobIdOrderByTargetKey(job.jobId());
        verify(processDataJpaRepository).findByTaskId(taskId);
    }

    @Test
    void updateTaskAndJobUsesBoundedAggregateQueries() {
        UUID jobId = UUID.fromString("88dbb65f-9634-4685-bc86-17b72d715d3e");
        UUID taskId = UUID.fromString("019c8c72-6fd1-7f25-a9a1-3b3d51fbb321");
        Instant now = Instant.parse("2026-08-06T08:03:13Z");
        MaintenanceTask queuedTask = new MaintenanceTask(taskId, MaintenanceTargetType.PROCESS, "assessment-4711",
                "assessment-4711", MaintenanceTaskState.EVENT_QUEUED, now.minusSeconds(1), null, null, null);
        MaintenanceTask succeededTask = queuedTask.transitionTo(MaintenanceTaskState.SUCCEEDED, null, null, now);
        MaintenanceJob job = job(List.of(queuedTask));
        MaintenanceTaskEntity taskEntity = MaintenanceTaskEntity.fromDomain(jobId, queuedTask);
        MaintenanceJobEntity jobEntity = MaintenanceJobEntity.fromDomain(job);
        when(taskJpaRepository.findById(taskId)).thenReturn(Optional.of(taskEntity));
        when(jobJpaRepository.findByIdForUpdate(jobId)).thenReturn(Optional.of(jobEntity));
        when(taskJpaRepository.countByJobId(jobId)).thenReturn(5L);
        when(taskJpaRepository.countByJobIdAndTaskStateIn(jobId,
                List.of(MaintenanceTaskState.SUCCEEDED, MaintenanceTaskState.NOT_FOUND, MaintenanceTaskState.FAILED)))
                .thenReturn(3L);
        when(taskJpaRepository.countByJobIdAndTaskState(jobId, MaintenanceTaskState.SUCCEEDED)).thenReturn(2L);

        repository.updateTaskAndJob(job, succeededTask);

        verify(entityManager).flush();
        verify(taskJpaRepository).countByJobId(jobId);
        verify(taskJpaRepository).countByJobIdAndTaskStateIn(jobId,
                List.of(MaintenanceTaskState.SUCCEEDED, MaintenanceTaskState.NOT_FOUND, MaintenanceTaskState.FAILED));
        verify(taskJpaRepository).countByJobIdAndTaskState(jobId, MaintenanceTaskState.SUCCEEDED);
    }

    private static MaintenanceJob job() {
        return job(List.of());
    }

    private static MaintenanceJob job(List<MaintenanceTask> tasks) {
        return new MaintenanceJob(
                UUID.fromString("88dbb65f-9634-4685-bc86-17b72d715d3e"),
                MaintenanceJobType.RELATION_REEVALUATION,
                "assessmentProcess",
                "a".repeat(64),
                MaintenanceJobState.OPEN,
                null,
                Instant.parse("2026-08-06T08:03:12Z"),
                null,
                null,
                null,
                tasks);
    }
}
