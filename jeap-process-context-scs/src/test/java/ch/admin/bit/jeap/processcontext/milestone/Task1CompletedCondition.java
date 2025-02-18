package ch.admin.bit.jeap.processcontext.milestone;

import ch.admin.bit.jeap.processcontext.plugin.api.condition.MilestoneCondition;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext;
import ch.admin.bit.jeap.processcontext.plugin.api.context.TaskState;

public class Task1CompletedCondition implements MilestoneCondition {

    @Override
    public boolean isMilestoneReached(ProcessContext processContext) {
        return processContext.isTasksInState("task1", TaskState.COMPLETED);
    }
}
