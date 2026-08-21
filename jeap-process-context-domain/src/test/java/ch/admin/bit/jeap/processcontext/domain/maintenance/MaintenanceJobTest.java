package ch.admin.bit.jeap.processcontext.domain.maintenance;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MaintenanceJobTest {

    private static final Instant NOW = Instant.parse("2026-08-06T08:03:12Z");

    @Test
    void completeIfAllTasksTerminal_derivesAggregateResult() {
        MaintenanceJob partiallySucceeded = job().completeIfAllTasksTerminal(
                NOW.plusSeconds(1), new MaintenanceTaskCounts(3, 3, 1));
        MaintenanceJob failed = job().completeIfAllTasksTerminal(
                NOW.plusSeconds(1), new MaintenanceTaskCounts(3, 3, 0));

        assertThat(partiallySucceeded.jobState()).isEqualTo(MaintenanceJobState.COMPLETED);
        assertThat(partiallySucceeded.jobResult()).isEqualTo(MaintenanceJobResult.PARTIALLY_SUCCEEDED);
        assertThat(failed.jobResult()).isEqualTo(MaintenanceJobResult.FAILED);
    }

    @Test
    void completeIfAllTasksTerminal_keepsJobOpenWhileTaskIsNonterminal() {
        MaintenanceJob open = job().completeIfAllTasksTerminal(
                NOW.plusSeconds(1), new MaintenanceTaskCounts(3, 2, 2));

        assertThat(open.jobState()).isEqualTo(MaintenanceJobState.OPEN);
        assertThat(open.jobResult()).isNull();
        assertThat(open.completedAt()).isNull();
    }

    private static MaintenanceJob job() {
        return new MaintenanceJob(UUID.fromString("88dbb65f-9634-4685-bc86-17b72d715d3e"),
                MaintenanceJobType.RELATION_REEVALUATION, "assessmentProcess", "a".repeat(64),
                MaintenanceJobState.OPEN, null, NOW, null, null, null, List.of());
    }
}
