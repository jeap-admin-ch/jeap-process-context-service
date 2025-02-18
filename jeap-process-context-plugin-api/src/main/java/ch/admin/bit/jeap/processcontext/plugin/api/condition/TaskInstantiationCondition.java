package ch.admin.bit.jeap.processcontext.plugin.api.condition;

import ch.admin.bit.jeap.processcontext.plugin.api.context.Message;

/**
 * Classes implementing this interface can be used as conditions to decide whether a Task should be instantiated
 */
public interface TaskInstantiationCondition {

    /**
     * Method to determine whether a Task should be instantiated based on the incoming message information
     * @param message The received message
     * @return <b>true</b> if the task should be instantiated, <b>false</b> otherwise
     */
    boolean instantiate(Message message);

}
