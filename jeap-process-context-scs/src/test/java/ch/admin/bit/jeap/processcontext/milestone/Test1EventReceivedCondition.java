package ch.admin.bit.jeap.processcontext.milestone;

import ch.admin.bit.jeap.processcontext.plugin.api.condition.MilestoneCondition;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext;

public class Test1EventReceivedCondition implements MilestoneCondition {
    @Override
    public boolean isMilestoneReached(ProcessContext processContext) {
        return !processContext.getMessagesByName("Test1Event").isEmpty();
    }
}
