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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
    void findQueuedTaskAndUpdate_persistsLifecycleAndAggregateResult() {
        MaintenanceJob created = job(List.of(task(TASK_ID_1, "assessment-4711")));
        maintenanceJobRepository.create(created);
        entityManager.clear();

        MaintenanceJob claimed = maintenanceJobRepository.findByTaskIdForUpdate(TASK_ID_1).orElseThrow();
        maintenanceJobRepository.update(claimed.transitionTask(
                TASK_ID_1, MaintenanceTaskState.SUCCEEDED, null, STARTED.plusSeconds(1)));
        entityManager.flush();
        entityManager.clear();

        MaintenanceJob completed = maintenanceJobRepository.findById(JOB_ID).orElseThrow();
        assertThat(completed.jobState()).isEqualTo(MaintenanceJobState.COMPLETED);
        assertThat(completed.jobResult()).isEqualTo(MaintenanceJobResult.SUCCEEDED);
        assertThat(completed.tasks().getFirst().taskState()).isEqualTo(MaintenanceTaskState.SUCCEEDED);
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
}
