package ch.admin.bit.jeap.processcontext.plugin.api.event;

import ch.admin.bit.jeap.messaging.model.Message;

/**
 * A process instantiation condition that always triggers an instantiation.
 */
public class AlwaysProcessInstantiationCondition implements ProcessInstantiationCondition<Message> {

    public boolean triggersProcessInstantiation(Message message) {
        return true;
    }

}
