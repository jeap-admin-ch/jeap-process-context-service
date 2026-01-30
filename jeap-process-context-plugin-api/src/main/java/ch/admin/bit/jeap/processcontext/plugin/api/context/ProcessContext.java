package ch.admin.bit.jeap.processcontext.plugin.api.context;

import java.util.List;

/**
 * Represents the process context of a process instance, providing access to its metadata and messages when implementing
 * conditions. When extending this interface, be careful not to expose data that is altered by conditions such as task
 * or process state. This would lead to inconsistent behavior when conditions are evaluated in a different order.
 */
public interface ProcessContext {

    String getOriginProcessId();

    String getProcessName();

    List<Message> getMessages();

    /**
     * Get all messages with the given name.
     *
     * @param messageName Name of the messages to retrieve
     * @return List of messages with the given name. If no message with the given name exists, an empty list is returned.
     */
    List<Message> getMessagesByName(String messageName);

    /**
     * Checks whether all tasks in the process context are in a final state. For this to be true,
     * the following conditions must be met:
     * <ul>
     *   <li>the process context contains at least one task</li>
     *   <li>all existing tasks are in a final state (COMPLETED, NOT_REQUIRED or DELETED)</li>
     * </ul>
     *
     * @return true if all tasks are in a final state and at least one task exists, false otherwise
     */
    boolean isAllTasksInFinalState();
}
