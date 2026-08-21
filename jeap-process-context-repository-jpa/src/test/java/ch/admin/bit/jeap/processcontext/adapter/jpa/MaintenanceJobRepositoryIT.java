package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.maintenance.*;
import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextFactory;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "jeap.processcontext.maintenance.enabled=true")
@ContextConfiguration(classes = JpaAdapterConfig.class)
class MaintenanceJobRepositoryIT {

    private static final UUID JOB_ID = UUID.fromString("88dbb65f-9634-4685-bc86-17b72d715d3e");
    private static final UUID TASK_ID_1 = UUID.fromString("019c8c72-6fd1-7f25-a9a1-3b3d51fbb321");
    private static final UUID TASK_ID_2 = UUID.fromString("019c8c72-7b42-7a04-9443-bf8ec98ce871");
    private static final Instant STARTED = Instant.parse("2026-08-06T08:03:12Z");

    @Autowired
    private MaintenanceJobRepository maintenanceJobRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private DataSource dataSource;
    @MockitoBean
    private ProcessTemplateRepository processTemplateRepository;
    @MockitoBean
    private ProcessContextFactory processContextFactory;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM pcs_maintenance_task");
        jdbcTemplate.update("DELETE FROM pcs_maintenance_job");
    }

    @Test
    void createAndFind_persistsJobAndTasksWithoutCascade() {
        MaintenanceJob expected = job(List.of(
                task(TASK_ID_1, "assessment-4711"),
                task(TASK_ID_2, "assessment-4712")));

        maintenanceJobRepository.create(expected);
        entityManager.clear();

        MaintenanceJob actual = maintenanceJobRepository.findById(JOB_ID).orElseThrow();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void find_unknownJob_returnsEmpty() {
        assertThat(maintenanceJobRepository.findById(JOB_ID)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void legacyCreatedTasks_areCompletedAsFailedByMigration() {
        MaintenanceTask legacyTask = new MaintenanceTask(TASK_ID_1, MaintenanceTargetType.PROCESS,
                "assessment-4711", "assessment-4711", MaintenanceTaskState.CREATED, STARTED,
                null, null, null);
        maintenanceJobRepository.create(job(List.of(legacyTask)));

        new ResourceDatabasePopulator(new ClassPathResource(
                "db/migration/common/V1_0_53__complete-record-only-maintenance-jobs.sql")).execute(dataSource);

        MaintenanceJob migrated = maintenanceJobRepository.findById(JOB_ID).orElseThrow();
        assertThat(migrated.jobState()).isEqualTo(MaintenanceJobState.COMPLETED);
        assertThat(migrated.jobResult()).isEqualTo(MaintenanceJobResult.FAILED);
        assertThat(migrated.completedAt()).isNotNull();
        assertThat(migrated.task(TASK_ID_1).taskState()).isEqualTo(MaintenanceTaskState.FAILED);
        assertThat(migrated.task(TASK_ID_1).errorMessage())
                .isEqualTo("Job was submitted before maintenance execution was available");
    }

    @Test
    void findQueuedTaskAndUpdate_persistsLifecycleAndAggregateResult() {
        MaintenanceJob created = job(List.of(task(TASK_ID_1, "assessment-4711")));
        maintenanceJobRepository.create(created);
        entityManager.clear();

        MaintenanceJob lockedJob = maintenanceJobRepository.findTaskForUpdate(TASK_ID_1).orElseThrow();
        Instant completedAt = STARTED.plusSeconds(1);
        maintenanceJobRepository.updateTaskAndJob(lockedJob,
                lockedJob.task(TASK_ID_1).transitionTo(MaintenanceTaskState.SUCCEEDED, null, null, completedAt));
        entityManager.flush();
        entityManager.clear();

        MaintenanceJob completed = maintenanceJobRepository.findById(JOB_ID).orElseThrow();
        assertThat(completed.jobState()).isEqualTo(MaintenanceJobState.COMPLETED);
        assertThat(completed.jobResult()).isEqualTo(MaintenanceJobResult.SUCCEEDED);
        assertThat(completed.completedAt()).isAfterOrEqualTo(completedAt);
        assertThat(completed.tasks().getFirst().taskState()).isEqualTo(MaintenanceTaskState.SUCCEEDED);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentTaskUpdates_doNotSerializeOnJobAndCompleteAggregate() throws Exception {
        maintenanceJobRepository.create(job(List.of(
                task(TASK_ID_1, "assessment-4711"),
                task(TASK_ID_2, "assessment-4712"))));

        CountDownLatch tasksLocked = new CountDownLatch(2);
        CountDownLatch completeTasks = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = updateTaskConcurrently(executor, TASK_ID_1, tasksLocked, completeTasks);
            Future<?> second = updateTaskConcurrently(executor, TASK_ID_2, tasksLocked, completeTasks);

            assertThat(tasksLocked.await(5, TimeUnit.SECONDS))
                    .as("both task rows can be locked before either transaction completes")
                    .isTrue();
            completeTasks.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        } finally {
            completeTasks.countDown();
            executor.shutdownNow();
        }

        MaintenanceJob completed = maintenanceJobRepository.findById(JOB_ID).orElseThrow();
        assertThat(completed.jobState()).isEqualTo(MaintenanceJobState.COMPLETED);
        assertThat(completed.jobResult()).isEqualTo(MaintenanceJobResult.SUCCEEDED);
        assertThat(completed.tasks()).extracting(MaintenanceTask::taskState)
                .containsExactly(MaintenanceTaskState.SUCCEEDED, MaintenanceTaskState.SUCCEEDED);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void create_duplicateTarget_rollsBackWithoutReportingExistingJob() {
        MaintenanceJob invalid = job(List.of(
                task(TASK_ID_1, "assessment-4711"),
                task(TASK_ID_2, "assessment-4711")));

        assertThatThrownBy(() -> maintenanceJobRepository.create(invalid))
                .isInstanceOf(PersistenceException.class)
                .isNotInstanceOf(MaintenanceJobAlreadyExistsException.class);

        assertThat(maintenanceJobRepository.findById(JOB_ID)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void create_existingJobId_keepsOriginalAndReportsAlreadyExists() {
        MaintenanceJob original = job(List.of(task(TASK_ID_1, "assessment-4711")));
        maintenanceJobRepository.create(original);

        MaintenanceJob conflicting = new MaintenanceJob(
                JOB_ID,
                MaintenanceJobType.RELATION_REEVALUATION,
                "otherTemplate",
                "b".repeat(64),
                MaintenanceJobState.OPEN,
                null,
                STARTED.plusSeconds(1),
                null,
                "Other User",
                "999",
                List.of(task(TASK_ID_2, "assessment-4712")));

        assertThatThrownBy(() -> maintenanceJobRepository.create(conflicting))
                .isInstanceOf(MaintenanceJobAlreadyExistsException.class);
        assertThat(maintenanceJobRepository.findById(JOB_ID)).contains(original);
    }

    @Test
    void createAndFind_acceptsLongSubmitterClaims() {
        String longClaim = "x".repeat(300);
        MaintenanceJob job = new MaintenanceJob(
                JOB_ID,
                MaintenanceJobType.RELATION_REEVALUATION,
                "assessmentProcess",
                "a".repeat(64),
                MaintenanceJobState.OPEN,
                null,
                STARTED,
                null,
                longClaim,
                longClaim,
                List.of(task(TASK_ID_1, "assessment-4711")));

        maintenanceJobRepository.create(job);
        entityManager.clear();

        MaintenanceJob persisted = maintenanceJobRepository.findById(JOB_ID).orElseThrow();
        assertThat(persisted.startedByName()).isEqualTo(longClaim);
        assertThat(persisted.startedByExtId()).isEqualTo(longClaim);
    }

    @Test
    void deleteCompletedBefore_deletesJobAndTasksByCascade() {
        MaintenanceJob completed = job(List.of(task(TASK_ID_1, "assessment-4711")))
                .transitionTask(TASK_ID_1, MaintenanceTaskState.SUCCEEDED, null, STARTED.plusSeconds(1));
        maintenanceJobRepository.create(completed);
        entityManager.clear();

        int deleted = maintenanceJobRepository.deleteCompletedBefore(STARTED.plusSeconds(2), 10);
        entityManager.flush();
        entityManager.clear();

        assertThat(deleted).isOne();
        assertThat(maintenanceJobRepository.findById(JOB_ID)).isEmpty();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM pcs_maintenance_task", Integer.class)).isZero();
    }

    private MaintenanceJob job(List<MaintenanceTask> tasks) {
        return new MaintenanceJob(
                JOB_ID,
                MaintenanceJobType.RELATION_REEVALUATION,
                "assessmentProcess",
                "a".repeat(64),
                MaintenanceJobState.OPEN,
                null,
                STARTED,
                null,
                "John Doe",
                "287365",
                tasks);
    }

    private MaintenanceTask task(UUID taskId, String originProcessId) {
        return new MaintenanceTask(
                taskId,
                MaintenanceTargetType.PROCESS,
                originProcessId,
                originProcessId,
                MaintenanceTaskState.EVENT_QUEUED,
                STARTED,
                null,
                null,
                null);
    }

    private Future<?> updateTaskConcurrently(ExecutorService executor, UUID taskId, CountDownLatch tasksLocked,
                                             CountDownLatch completeTasks) {
        return executor.submit(() -> {
            TransactionTemplate transaction = new TransactionTemplate(transactionManager);
            transaction.executeWithoutResult(ignored -> {
                MaintenanceJob lockedJob = maintenanceJobRepository.findTaskForUpdate(taskId).orElseThrow();
                tasksLocked.countDown();
                try {
                    if (!completeTasks.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to complete maintenance task");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting to complete maintenance task", e);
                }
                Instant completedAt = Instant.now();
                maintenanceJobRepository.updateTaskAndJob(lockedJob,
                        lockedJob.task(taskId).transitionTo(MaintenanceTaskState.SUCCEEDED, null, null, completedAt));
            });
        });
    }
}
