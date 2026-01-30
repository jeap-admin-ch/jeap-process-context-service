package ch.admin.bit.jeap.processcontext.plugin.api.condition;

import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessCompletionConclusion;
import ch.admin.bit.jeap.processcontext.plugin.api.context.test.ProcessContextStub;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AllTasksInFinalStateProcessCompletionConditionTest {

    private final AllTasksInFinalStateProcessCompletionCondition condition = new AllTasksInFinalStateProcessCompletionCondition();

    @Test
    void testIsProcessCompleted_whenAllTasksInFinalState_thenCompleted() {
        ProcessContextStub processContext = ProcessContextStub.builder()
                .originProcessId("test-process-id")
                .processName("test-process")
                .allTasksCompleted(true)
                .build();

        ProcessCompletionConditionResult result = condition.isProcessCompleted(processContext);

        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getConclusion()).hasValue(ProcessCompletionConclusion.SUCCEEDED);
        assertThat(result.getName()).hasValue("allTasksInFinalStateProcessCompletionCondition");
    }

    @Test
    void testIsProcessCompleted_whenNotAllTasksInFinalState_thenInProgress() {
        ProcessContextStub processContext = ProcessContextStub.builder()
                .originProcessId("test-process-id")
                .processName("test-process")
                .allTasksCompleted(false)
                .build();

        ProcessCompletionConditionResult result = condition.isProcessCompleted(processContext);

        assertThat(result.isCompleted()).isFalse();
        assertThat(result.getConclusion()).isEmpty();
        assertThat(result.getName()).isEmpty();
    }

}

