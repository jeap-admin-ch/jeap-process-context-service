package ch.admin.bit.jeap.processcontext.plugin.api.context;

import java.util.List;

public interface ProcessContext {

    String getOriginProcessId();

    String getProcessName();

    ProcessState getProcessState();

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
     * @return
     */
    boolean isAllTasksInFinalState();
}
