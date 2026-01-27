package ch.admin.bit.jeap.processcontext.processinstantiation.relations;

import ch.admin.bit.jeap.processcontext.event.test2.Test2Event;
import ch.admin.bit.jeap.processcontext.plugin.api.event.ProcessInstantiationCondition;

public class InstantiationConditionC implements ProcessInstantiationCondition<Test2Event> {

    @Override
    public boolean triggersProcessInstantiation(Test2Event message) {
        return message.getPayload().getObjectId().contains("C");
    }
}
