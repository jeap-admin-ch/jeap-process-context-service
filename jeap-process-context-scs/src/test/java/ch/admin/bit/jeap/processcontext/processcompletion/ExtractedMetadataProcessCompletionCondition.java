package ch.admin.bit.jeap.processcontext.processcompletion;

import ch.admin.bit.jeap.processcontext.plugin.api.condition.ProcessCompletionCondition;
import ch.admin.bit.jeap.processcontext.plugin.api.condition.ProcessCompletionConditionResult;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessCompletionConclusion;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext;

public class ExtractedMetadataProcessCompletionCondition implements ProcessCompletionCondition {

    @Override
    public ProcessCompletionConditionResult isProcessCompleted(ProcessContext processContext) {
        if (processContext.containsMessageByTypeWithMessageData("Test3Event", "messageType", "Test3Event")) {
            return ProcessCompletionConditionResult.completedBuilder()
                    .conclusion(ProcessCompletionConclusion.SUCCEEDED)
                    .name("extractedMetadata")
                    .build();
        }
        return ProcessCompletionConditionResult.IN_PROGRESS;
    }
}
