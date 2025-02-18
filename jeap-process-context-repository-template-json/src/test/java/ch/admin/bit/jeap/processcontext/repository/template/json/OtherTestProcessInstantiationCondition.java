package ch.admin.bit.jeap.processcontext.repository.template.json;

import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.processcontext.plugin.api.event.ProcessInstantiationCondition;

@SuppressWarnings("unused")
public class OtherTestProcessInstantiationCondition implements ProcessInstantiationCondition<Message> {
    @Override
    public boolean triggersProcessInstantiation(Message message) {
        return true;
    }
}
