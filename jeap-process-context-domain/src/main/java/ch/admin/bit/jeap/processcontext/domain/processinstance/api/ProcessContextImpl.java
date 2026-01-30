package ch.admin.bit.jeap.processcontext.domain.processinstance.api;

import ch.admin.bit.jeap.processcontext.plugin.api.context.Message;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessState;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

public final class ProcessContextImpl implements ProcessContext {

    @Getter
    private final String originProcessId;

    @Getter
    private final String processName;

    @Getter
    private final ProcessState processState;

    @Getter
    private final List<Message> messages;

    private final Map<String, List<Message>> messagesByName;

    @Builder
    private ProcessContextImpl(@NonNull String originProcessId,
                               @NonNull String processName,
                               @NonNull ProcessState processState,
                               @NonNull List<Message> messages) {
        this.originProcessId = originProcessId;
        this.processName = processName;
        this.processState = processState;
        this.messages = messages;
        this.messagesByName = messages.stream()
                .collect(groupingBy(Message::getName));
    }

    @Override
    public List<Message> getMessagesByName(String messageName) {
        return messagesByName.getOrDefault(messageName, List.of());
    }
}
