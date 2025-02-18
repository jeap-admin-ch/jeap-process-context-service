package ch.admin.bit.jeap.processcontext.plugin.api.condition;

import ch.admin.bit.jeap.processcontext.plugin.api.context.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TasksCompletedMilestoneConditionTest {

    @Test
    void isMilestoneReached_shouldReturnTrueIfTaskIsCompleted() {
        TaskState taskState = TaskState.COMPLETED;
        ProcessContext processContext = createProcessContext(taskState);

        TasksCompletedMilestoneCondition condition = new TasksCompletedMilestoneCondition(Set.of("task"));
        assertTrue(condition.isMilestoneReached(processContext));
    }

    @Test
    void isMilestoneReached_shouldReturnFalseIfTaskIsNotCompletedOrDoesNotExist() {
        TaskState taskState = TaskState.PLANNED;
        ProcessContext processContext = createProcessContext(taskState);

        TasksCompletedMilestoneCondition condition = new TasksCompletedMilestoneCondition(Set.of("task"));
        assertFalse(condition.isMilestoneReached(processContext));

        TasksCompletedMilestoneCondition conditionTaskNotExists = new TasksCompletedMilestoneCondition(Set.of("doesNotExist"));
        assertFalse(conditionTaskNotExists.isMilestoneReached(processContext));
    }

    private ProcessContext createProcessContext(TaskState taskState) {
        return ProcessContext.builder()
                .originProcessId("id")
                .processName("name")
                .processState(ProcessState.STARTED)
                .messages(List.of())
                .tasks(List.of(Task.builder()
                        .type(TaskType.builder()
                                .name("task")
                                .lifecycle(TaskLifecycle.STATIC)
                                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                                .build())
                        .state(taskState)
                        .id("id")
                        .build()))
                .build();
    }
}
