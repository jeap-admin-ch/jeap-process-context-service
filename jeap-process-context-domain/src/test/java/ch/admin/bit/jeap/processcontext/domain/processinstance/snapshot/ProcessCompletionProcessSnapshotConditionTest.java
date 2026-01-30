package ch.admin.bit.jeap.processcontext.domain.processinstance.snapshot;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessCompletion;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessCompletionConclusion;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessCompletionProcessSnapshotConditionTest {

    @Test
    void testTriggerSnapshot_WhenAnyConclusionCondition_ThenTriggerOnAnyProcessCompletion() {
        ProcessCompletionProcessSnapshotCondition conditionAnyCompletion = new ProcessCompletionProcessSnapshotCondition();

        // not triggered without completion
        ProcessInstance instanceWithoutCompletion = createProcessContext(null);
        assertThat(conditionAnyCompletion.triggerSnapshot(instanceWithoutCompletion).isSnapShotTriggered()).isFalse();

        // triggered for all completion conclusions
        for (ProcessCompletionConclusion conclusion : ProcessCompletionConclusion.values()) {
            ProcessInstance instanceWithCompletion = createProcessContext(conclusion);
            ProcessSnapshotConditionResult result = conditionAnyCompletion.triggerSnapshot(instanceWithCompletion);
            assertThat(result.isSnapShotTriggered()).isTrue();
            assertThat(result.getSnapshotName()).isEqualTo("JeapProcessCompletionSnapshotCondition");
        }

    }

    @Test
    void testTriggerSnapshot_WhenSpecificConclusionCondition_ThenTriggerOnyOnSpecificProcessCompletion() {
        ProcessCompletionProcessSnapshotCondition conditionOnlySucceededCompletions =
                new ProcessCompletionProcessSnapshotCondition(ProcessCompletionConclusion.SUCCEEDED);

        // not triggered without completion
        ProcessInstance instanceWithoutCompletion = createProcessContext(null);
        assertThat(conditionOnlySucceededCompletions.triggerSnapshot(instanceWithoutCompletion).isSnapShotTriggered()).isFalse();

        // not triggered for completions with conclusions different from the expected one
        Arrays.stream(ProcessCompletionConclusion.values()).
                filter(conclusion -> conclusion != conditionOnlySucceededCompletions.getTriggeringConclusion()).
                map(this::createProcessContext).
                forEach(contextWithDifferentCompletion ->
                        assertThat(conditionOnlySucceededCompletions.triggerSnapshot(contextWithDifferentCompletion).isSnapShotTriggered()).isFalse());

        // triggered for completion with the expected conclusion
        ProcessInstance instanceWithExpectedCompletion = createProcessContext(conditionOnlySucceededCompletions.getTriggeringConclusion());
        ProcessSnapshotConditionResult result = conditionOnlySucceededCompletions.triggerSnapshot(instanceWithExpectedCompletion);
        assertThat(result.isSnapShotTriggered()).isTrue();
        assertThat(result.getSnapshotName()).isEqualTo("JeapProcessCompletionSnapshotCondition:" + ProcessCompletionConclusion.SUCCEEDED.name());
    }

    private ProcessInstance createProcessContext(ProcessCompletionConclusion conclusion) {
        ProcessCompletion processCompletion = null;
        if (conclusion != null) {
            processCompletion = ProcessCompletion.builder().
                    conclusion(conclusion).
                    name(conclusion.name()).
                    completedAt(ZonedDateTime.now()).
                    build();
        }
        ProcessInstance instance = mock(ProcessInstance.class);
        when(instance.getProcessCompletion())
                .thenReturn(java.util.Optional.ofNullable(processCompletion));
        return instance;
    }

}
