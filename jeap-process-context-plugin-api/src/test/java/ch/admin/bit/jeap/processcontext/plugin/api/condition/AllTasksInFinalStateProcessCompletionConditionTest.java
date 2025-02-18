package ch.admin.bit.jeap.processcontext.plugin.api.condition;

import ch.admin.bit.jeap.processcontext.plugin.api.context.*;
import com.fasterxml.uuid.Generators;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessCompletionConclusion.SUCCEEDED;
import static ch.admin.bit.jeap.processcontext.plugin.api.context.TaskState.PLANNED;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class AllTasksInFinalStateProcessCompletionConditionTest {

    @Test
    void testIsProcessCompleted_whenAllTasksInFinalState_theCompleted() {
        ProcessContext processContext = createProcessContextWithTasksInStates(
                Arrays.stream(TaskState.values()).filter(TaskState::isFinalState).toList());
        AllTasksInFinalStateProcessCompletionCondition condition = new AllTasksInFinalStateProcessCompletionCondition();

        ProcessCompletionConditionResult result = condition.isProcessCompleted(processContext);

        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getConclusion()).isPresent();
        assertThat(result.getConclusion()).contains(SUCCEEDED);
        assertThat(result.getName()).isPresent();
        assertThat(result.getName()).contains("allTasksInFinalStateProcessCompletionCondition");
    }

    @Test
    void testIsProcessCompleted_whenNoTaskInFinalState_theUncompleted() {
        ProcessContext processContext = createProcessContextWithTasksInStates(
                Arrays.stream(TaskState.values()).filter(state -> !state.isFinalState()).toList());
        AllTasksInFinalStateProcessCompletionCondition condition = new AllTasksInFinalStateProcessCompletionCondition();

        ProcessCompletionConditionResult result = condition.isProcessCompleted(processContext);

        assertThat(result.isCompleted()).isFalse();
        assertThat(result.getConclusion()).isNotPresent();
        assertThat(result.getName()).isNotPresent();
    }

    @Test
    void testIsProcessCompleted_whenSomeTasksInFinalStateButOneNot_theUncompleted() {
        ProcessContext processContext = createProcessContextWithTasksInStates(
                Stream.concat(Arrays.stream(TaskState.values()).filter(state -> !state.isFinalState()), Stream.of(PLANNED)).toList());
        AllTasksInFinalStateProcessCompletionCondition condition = new AllTasksInFinalStateProcessCompletionCondition();

        ProcessCompletionConditionResult result = condition.isProcessCompleted(processContext);

        assertThat(result.isCompleted()).isFalse();
    }

    @Test
    void testIsProcessCompleted_whenNoTasks_theUncompleted() {
        ProcessContext processContext = createProcessContextWithTasksInStates(List.of());
        AllTasksInFinalStateProcessCompletionCondition condition = new AllTasksInFinalStateProcessCompletionCondition();

        ProcessCompletionConditionResult result = condition.isProcessCompleted(processContext);

        assertThat(result.isCompleted()).isFalse();
    }

    private ProcessContext createProcessContextWithTasksInStates(List<TaskState> taskStates) {
        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < taskStates.size(); i++) {
            tasks.add(createTask("task-" + i, taskStates.get(i)));
        }
        return ProcessContext.builder()
                .originProcessId("id")
                .processName("name")
                .processState(ProcessState.STARTED)
                .messages(List.of())
                .tasks(tasks)
                .build();
    }

    private Task createTask(String taskTypeName, TaskState taskState) {
        TaskType taskType = TaskType.builder().name(taskTypeName).lifecycle(TaskLifecycle.STATIC).cardinality(TaskCardinality.SINGLE_INSTANCE).build();
        return Task.builder().type(taskType).state(taskState).id(Generators.timeBasedEpochGenerator().generate().toString()).build();
    }
}

