package ch.admin.bit.jeap.processcontext.processcompletion;

import ch.admin.bit.jeap.processcontext.plugin.api.condition.ProcessCompletionCondition;
import ch.admin.bit.jeap.processcontext.plugin.api.condition.ProcessCompletionConditionResult;
import ch.admin.bit.jeap.processcontext.plugin.api.context.Message;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessCompletionConclusion;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext;

import java.util.Set;

public class Event1GotchaInDataKey1ProcessCompletionCondition implements ProcessCompletionCondition {
    @Override
    public ProcessCompletionConditionResult isProcessCompleted(ProcessContext processContext) {
        return processContext.getMessagesByName("Test1Event").stream()
                .map(Message::getMessageData)
                .flatMap(Set::stream)
                .filter(eventData -> eventData.getKey().equals("key1") && eventData.getValue().equals("gotcha"))
                .findFirst()
                .map( eventData -> ProcessCompletionConditionResult.completedBuilder()
                        .conclusion(ProcessCompletionConclusion.SUCCEEDED)
                        .name("event1GotchaInDataKey1ProcessCompletionCondition")
                        .build())
                .orElse(ProcessCompletionConditionResult.IN_PROGRESS);
    }
}
