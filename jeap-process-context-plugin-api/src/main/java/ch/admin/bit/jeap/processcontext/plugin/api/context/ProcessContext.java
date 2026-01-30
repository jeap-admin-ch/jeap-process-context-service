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
}
