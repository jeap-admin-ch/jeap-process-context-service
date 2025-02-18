package ch.admin.bit.jeap.processcontext.plugin.api.condition;

import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessCompletion;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessCompletionConclusion;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessState;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessCompletionProcessSnapshotConditionTest {

    @Test
    void testTriggerSnapshot_WhenAnyConclusionCondition_ThenTriggerOnAnyProcessCompletion() {
        ProcessCompletionProcessSnapshotCondition conditionAnyCompletion =  new ProcessCompletionProcessSnapshotCondition();

        // not triggered without completion
        ProcessContext contextWithoutCompletion = createProcessContext(null);
        assertThat(conditionAnyCompletion.triggerSnapshot(contextWithoutCompletion).isSnapShotTriggered()).isFalse();

        // triggered for all completion conclusions
        for (ProcessCompletionConclusion conclusion : ProcessCompletionConclusion.values()) {
            ProcessContext contextWithCompletion = createProcessContext(conclusion);
            ProcessSnapshotConditionResult result = conditionAnyCompletion.triggerSnapshot(contextWithCompletion);
            assertThat(result.isSnapShotTriggered()).isTrue();
            assertThat(result.getSnapshotName()).isEqualTo("JeapProcessCompletionSnapshotCondition");
        }

    }

    @Test
    void testTriggerSnapshot_WhenSpecificConclusionCondition_ThenTriggerOnyOnSpecificProcessCompletion() {
        ProcessCompletionProcessSnapshotCondition conditionOnlySucceededCompletions =
                new ProcessCompletionProcessSnapshotCondition(ProcessCompletionConclusion.SUCCEEDED);

        // not triggered without completion
        ProcessContext contextWithoutCompletion = createProcessContext(null);
        assertThat(conditionOnlySucceededCompletions.triggerSnapshot(contextWithoutCompletion).isSnapShotTriggered()).isFalse();

        // not triggered for completions with conclusions different from the expected one
        Arrays.stream(ProcessCompletionConclusion.values()).
                filter(conclusion -> conclusion != conditionOnlySucceededCompletions.getTriggeringConclusion()).
                map(this::createProcessContext).
                forEach(contextWithDifferentCompletion ->
                    assertThat(conditionOnlySucceededCompletions.triggerSnapshot(contextWithDifferentCompletion).isSnapShotTriggered()).isFalse());

        // triggered for completion with the expected conclusion
        ProcessContext contextWithExpectedCompletion = createProcessContext(conditionOnlySucceededCompletions.getTriggeringConclusion());
        ProcessSnapshotConditionResult result = conditionOnlySucceededCompletions.triggerSnapshot(contextWithExpectedCompletion);
        assertThat(result.isSnapShotTriggered()).isTrue();
        assertThat(result.getSnapshotName()).isEqualTo("JeapProcessCompletionSnapshotCondition:" + ProcessCompletionConclusion.SUCCEEDED.name());
    }

    private ProcessContext createProcessContext(ProcessCompletionConclusion conclusion) {
        ProcessCompletion processCompletion = null;
        if (conclusion != null) {
            processCompletion = ProcessCompletion.builder().
                    conclusion(conclusion).
                    name(conclusion.name()).
                    completedAt(ZonedDateTime.now()).
                    build();
        }
        return ProcessContext.builder().
                originProcessId("origin-process-id").
                processName("process-name").
                processState(ProcessState.STARTED).
                tasks(List.of()).
                messages(List.of()).
                processCompletion(processCompletion).
                build();
    }

}
