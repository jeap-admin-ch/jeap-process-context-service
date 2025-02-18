package ch.admin.bit.jeap.processcontext.plugin.api.condition;

import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext;

public interface MilestoneCondition {

    boolean isMilestoneReached(ProcessContext processContext);
}
