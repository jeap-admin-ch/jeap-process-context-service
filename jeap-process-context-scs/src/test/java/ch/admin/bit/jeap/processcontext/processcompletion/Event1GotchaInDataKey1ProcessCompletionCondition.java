package ch.admin.bit.jeap.processcontext.processcompletion;

import ch.admin.bit.jeap.processcontext.plugin.api.condition.ProcessCompletionCondition;
import ch.admin.bit.jeap.processcontext.plugin.api.condition.ProcessCompletionConditionResult;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessCompletionConclusion;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext;

public class Event1GotchaInDataKey1ProcessCompletionCondition implements ProcessCompletionCondition {
    @Override
    public ProcessCompletionConditionResult isProcessCompleted(ProcessContext processContext) {
        boolean matches = processContext
                .containsMessageByTypeWithMessageData("Test1Event", "key1", "gotcha");

        if (matches) {
            return ProcessCompletionConditionResult.completedBuilder()
                    .conclusion(ProcessCompletionConclusion.SUCCEEDED)
                    .name("event1GotchaInDataKey1ProcessCompletionCondition")
                    .build();
        }
        return ProcessCompletionConditionResult.IN_PROGRESS;
    }
}
