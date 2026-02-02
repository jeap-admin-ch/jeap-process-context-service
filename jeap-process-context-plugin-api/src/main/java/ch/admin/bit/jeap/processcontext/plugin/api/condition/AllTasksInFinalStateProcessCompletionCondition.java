package ch.admin.bit.jeap.processcontext.plugin.api.condition;

import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessCompletionConclusion;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext;

public class AllTasksInFinalStateProcessCompletionCondition implements ProcessCompletionCondition {

    @Override
    public ProcessCompletionConditionResult isProcessCompleted(ProcessContext processContext) {

        boolean completed = processContext.areAllTasksInFinalState();

        if (completed) {
            return ProcessCompletionConditionResult.completedBuilder()
                    .conclusion(ProcessCompletionConclusion.SUCCEEDED)
                    .name("allTasksInFinalStateProcessCompletionCondition")
                    .build();
        } else {
            return ProcessCompletionConditionResult.IN_PROGRESS;
        }
    }
}
