package ch.admin.bit.jeap.processcontext.adapter.jpa.maintenance;

import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJob;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobAlreadyExistsException;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobState;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class MaintenanceJobRepositoryImplTest {

    private final MaintenanceJobJpaRepository jobJpaRepository = mock(MaintenanceJobJpaRepository.class);
    private final MaintenanceTaskJpaRepository taskJpaRepository = mock(MaintenanceTaskJpaRepository.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final MaintenanceJobRepositoryImpl repository =
            new MaintenanceJobRepositoryImpl(jobJpaRepository, taskJpaRepository, entityManager);

    @Test
    void create_uniqueConstraintViolation_reportsAlreadyExists() {
        PersistenceException exception = new PersistenceException(new SQLException("duplicate", "23505"));
        doThrow(exception).when(entityManager).flush();

        assertThatThrownBy(() -> repository.create(job()))
                .isInstanceOf(MaintenanceJobAlreadyExistsException.class)
                .hasCause(exception);
    }

    @Test
    void create_otherPersistenceFailure_isNotMisclassified() {
        PersistenceException exception = new PersistenceException(new SQLException("connection failed", "08006"));
        doThrow(exception).when(entityManager).flush();

        assertThatThrownBy(() -> repository.create(job())).isSameAs(exception);
    }

    private static MaintenanceJob job() {
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
                List.of());
    }
}
