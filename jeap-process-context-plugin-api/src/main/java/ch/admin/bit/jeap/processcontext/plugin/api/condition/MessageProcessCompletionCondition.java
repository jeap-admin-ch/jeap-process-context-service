package ch.admin.bit.jeap.processcontext.plugin.api.condition;

import ch.admin.bit.jeap.processcontext.plugin.api.context.Message;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessCompletionConclusion;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext;
import lombok.Value;

import java.util.Optional;

@Value
public class MessageProcessCompletionCondition implements ProcessCompletionCondition {

    String messageName;
    ProcessCompletionConclusion conclusion;
    String name;

    @Override
    public ProcessCompletionConditionResult isProcessCompleted(ProcessContext processContext) {
        Optional<Message> expectedMessage = processContext.getMessagesByName(messageName).stream().findFirst();
        if (expectedMessage.isPresent()) {
            return ProcessCompletionConditionResult.completedBuilder()
                    .conclusion(conclusion)
                    .name(name)
                    .build();
        } else {
            return ProcessCompletionConditionResult.IN_PROGRESS;
        }
    }

}
