package ch.admin.bit.jeap.processcontext.processinstantiation;

import ch.admin.bit.jeap.processcontext.event.test5.Test5CreatingProcessInstanceEvent;
import ch.admin.bit.jeap.processcontext.plugin.api.event.ProcessInstantiationCondition;

public class TestProcessInstantiationCondition implements ProcessInstantiationCondition<Test5CreatingProcessInstanceEvent> {

    public static final String TRIGGER = "instantiation";
    public static final String NO_TRIGGER = "do not instantiate a process";

    @Override
    public boolean triggersProcessInstantiation(Test5CreatingProcessInstanceEvent event) {
        return TRIGGER.equals(event.getPayload().getTrigger());
    }
}
