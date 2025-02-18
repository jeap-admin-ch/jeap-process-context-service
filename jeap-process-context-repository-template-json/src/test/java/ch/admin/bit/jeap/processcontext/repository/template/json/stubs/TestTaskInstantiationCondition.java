package ch.admin.bit.jeap.processcontext.repository.template.json.stubs;

import ch.admin.bit.jeap.processcontext.plugin.api.condition.TaskInstantiationCondition;
import ch.admin.bit.jeap.processcontext.plugin.api.context.Message;

public class TestTaskInstantiationCondition implements TaskInstantiationCondition {
    @Override
    public boolean instantiate(Message message) {
        return false;
    }
}
