package ch.admin.bit.jeap.processcontext.plugin.api.event;

import ch.admin.bit.jeap.messaging.model.Message;

/**
 * A process instantiation condition that never triggers an instantiation.
 */
public class NeverProcessInstantiationCondition implements ProcessInstantiationCondition<Message> {

    public boolean triggersProcessInstantiation(Message message) {
        return false;
    }

}
