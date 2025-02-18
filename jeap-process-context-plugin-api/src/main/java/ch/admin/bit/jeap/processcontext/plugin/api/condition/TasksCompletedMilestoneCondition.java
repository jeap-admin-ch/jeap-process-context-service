package ch.admin.bit.jeap.processcontext.plugin.api.condition;

import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext;
import ch.admin.bit.jeap.processcontext.plugin.api.context.TaskState;
import lombok.Value;

import java.util.Set;

@Value
public class TasksCompletedMilestoneCondition implements MilestoneCondition {

    Set<String> taskNames;

    @Override
    public boolean isMilestoneReached(ProcessContext processContext) {
        return taskNames.stream().allMatch(taskName ->
                processContext.isTasksInState(taskName, TaskState.COMPLETED));
    }
}
