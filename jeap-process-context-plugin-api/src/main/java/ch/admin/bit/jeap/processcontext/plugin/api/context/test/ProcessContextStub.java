package ch.admin.bit.jeap.processcontext.plugin.api.context.test;

import ch.admin.bit.jeap.processcontext.plugin.api.context.Message;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessState;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

/**
 * Test stub implementation of {@link ProcessContext}. Useful for testing conditions.
 */
public class ProcessContextStub implements ProcessContext {

    @Getter
    private final String originProcessId;

    @Getter
    private final String processName;

    @Getter
    private final ProcessState processState;

    @Getter
    private final List<Message> messages;

    private final Map<String, List<Message>> messagesByName;

    private final boolean allTasksCompleted;

    @Builder
    private ProcessContextStub(@NonNull String originProcessId,
                               @NonNull String processName,
                               ProcessState processState,
                               List<Message> messages,
                               boolean allTasksCompleted) {
        this.originProcessId = originProcessId;
        this.processName = processName;
        if (processState == null) {
            processState = ProcessState.STARTED;
        }
        this.processState = processState;
        if (messages == null) {
            messages = List.of();
        }
        this.messages = messages;
        this.messagesByName = messages.stream()
                .collect(groupingBy(Message::getName));
        this.allTasksCompleted = allTasksCompleted;
    }

    @Override
    public List<Message> getMessagesByName(String messageName) {
        return messagesByName.getOrDefault(messageName, List.of());
    }

    @Override
    public boolean isAllTasksInFinalState() {
        return allTasksCompleted;
    }
}
