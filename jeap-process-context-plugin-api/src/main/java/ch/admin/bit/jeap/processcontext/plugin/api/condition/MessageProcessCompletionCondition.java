package ch.admin.bit.jeap.processcontext.plugin.api.condition;

import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessCompletionConclusion;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext;
import lombok.Value;

@Value
public class MessageProcessCompletionCondition implements ProcessCompletionCondition {

    String messageName;
    ProcessCompletionConclusion conclusion;
    String name;

    @Override
    public ProcessCompletionConditionResult isProcessCompleted(ProcessContext processContext) {
        boolean containsExpectedMessage = processContext.containsMessageOfType(messageName);

        if (containsExpectedMessage) {
            return ProcessCompletionConditionResult.completedBuilder()
                    .conclusion(conclusion)
                    .name(name)
                    .build();
        } else {
            return ProcessCompletionConditionResult.IN_PROGRESS;
        }
    }
}
