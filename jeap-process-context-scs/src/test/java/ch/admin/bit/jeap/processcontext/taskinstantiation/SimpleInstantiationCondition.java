package ch.admin.bit.jeap.processcontext.taskinstantiation;

import ch.admin.bit.jeap.processcontext.plugin.api.condition.TaskInstantiationCondition;
import ch.admin.bit.jeap.processcontext.plugin.api.context.Message;

public class SimpleInstantiationCondition implements TaskInstantiationCondition {
    @Override
    public boolean instantiate(Message message) {
        return message.getMessageData().stream().anyMatch(messageData -> {
            return messageData.getKey().equals("someField") && messageData.getValue().equals("foo");
        });
    }
}
