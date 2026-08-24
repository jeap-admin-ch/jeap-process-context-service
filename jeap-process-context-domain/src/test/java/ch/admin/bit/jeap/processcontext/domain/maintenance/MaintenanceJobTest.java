package ch.admin.bit.jeap.processcontext.domain.maintenance;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MaintenanceJobTest {

    private static final UUID JOB_ID = UUID.fromString("88dbb65f-9634-4685-bc86-17b72d715d3e");
    private static final UUID TASK_ID = UUID.fromString("019c8c72-6fd1-7f25-a9a1-3b3d51fbb321");
    private static final Instant NOW = Instant.parse("2026-08-06T08:03:12Z");

    @Test
    void nonterminalTransitionKeepsPartiallyLoadedJobOpen() {
        MaintenanceJob transitioned = job(MaintenanceTaskState.COMMAND_QUEUED)
                .transitionTask(TASK_ID, MaintenanceTaskState.EVENT_QUEUED, null, NOW.plusSeconds(1));

        assertThat(transitioned.jobState()).isEqualTo(MaintenanceJobState.OPEN);
        assertThat(transitioned.jobResult()).isNull();
        assertThat(transitioned.completedAt()).isNull();
    }

    @Test
    void terminalTransitionCompletesJobWhenAdjustedTerminalCountReachesTotal() {
        MaintenanceJob transitioned = job(MaintenanceTaskState.EVENT_QUEUED)
                .transitionTask(TASK_ID, MaintenanceTaskState.SUCCEEDED, null, NOW.plusSeconds(1),
                        new MaintenanceTaskCounts(3, 2, 2));

        assertThat(transitioned.jobState()).isEqualTo(MaintenanceJobState.COMPLETED);
        assertThat(transitioned.jobResult()).isEqualTo(MaintenanceJobResult.SUCCEEDED);
    }

    @Test
    void terminalTransitionKeepsJobOpenWhenOtherTasksAreNonterminal() {
        MaintenanceJob transitioned = job(MaintenanceTaskState.EVENT_QUEUED)
                .transitionTask(TASK_ID, MaintenanceTaskState.SUCCEEDED, null, NOW.plusSeconds(1),
                        new MaintenanceTaskCounts(3, 1, 1));

        assertThat(transitioned.jobState()).isEqualTo(MaintenanceJobState.OPEN);
        assertThat(transitioned.jobResult()).isNull();
    }

    @Test
    void terminalTransitionDerivesPartialAndFailedResultsFromAdjustedSucceededCount() {
        MaintenanceJob partiallySucceeded = job(MaintenanceTaskState.EVENT_QUEUED)
                .transitionTask(TASK_ID, MaintenanceTaskState.FAILED, "failure", NOW.plusSeconds(1),
                        new MaintenanceTaskCounts(3, 2, 1));
        MaintenanceJob failed = job(MaintenanceTaskState.EVENT_QUEUED)
                .transitionTask(TASK_ID, MaintenanceTaskState.NOT_FOUND, "missing", NOW.plusSeconds(1),
                        new MaintenanceTaskCounts(3, 2, 0));

        assertThat(partiallySucceeded.jobResult()).isEqualTo(MaintenanceJobResult.PARTIALLY_SUCCEEDED);
        assertThat(failed.jobResult()).isEqualTo(MaintenanceJobResult.FAILED);
    }

    @Test
    void terminalTransitionAdjustsCountsForTargetsThatWereAlreadyTerminal() {
        MaintenanceJob transitioned = job(MaintenanceTaskState.FAILED)
                .transitionTask(TASK_ID, MaintenanceTaskState.SUCCEEDED, null, NOW.plusSeconds(1),
                        new MaintenanceTaskCounts(2, 2, 1));

        assertThat(transitioned.jobState()).isEqualTo(MaintenanceJobState.COMPLETED);
        assertThat(transitioned.jobResult()).isEqualTo(MaintenanceJobResult.SUCCEEDED);
    }

    private static MaintenanceJob job(MaintenanceTaskState state) {
        MaintenanceTask task = new MaintenanceTask(TASK_ID, MaintenanceTargetType.PROCESS, "assessment-4711",
                "assessment-4711", state, NOW, null, null, null);
        return new MaintenanceJob(JOB_ID, MaintenanceJobType.RELATION_REEVALUATION, "assessmentProcess",
                "a".repeat(64), MaintenanceJobState.OPEN, null, NOW, null, null, null, List.of(task));
    }
}
