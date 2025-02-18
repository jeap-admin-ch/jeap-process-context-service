package ch.admin.bit.jeap.processcontext.repository.template.json.stubs;

import ch.admin.bit.jeap.processcontext.plugin.api.condition.MilestoneCondition;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext;

public class TestMilestoneCondition implements MilestoneCondition {

    @Override
    public boolean isMilestoneReached(ProcessContext processContext) {
        return false;
    }
}
