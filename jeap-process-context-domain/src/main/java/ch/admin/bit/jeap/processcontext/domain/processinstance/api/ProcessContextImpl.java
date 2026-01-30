package ch.admin.bit.jeap.processcontext.domain.processinstance.api;

import ch.admin.bit.jeap.processcontext.plugin.api.context.Message;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessState;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.groupingBy;

public final class ProcessContextImpl implements ProcessContext {

    @NonNull
    private final UUID processInstanceId;

    @Getter
    private final String originProcessId;

    @Getter
    private final String processName;

    @Getter
    private final ProcessState processState;

    @Getter
    private final List<Message> messages;

    private final Map<String, List<Message>> messagesByName;

    private final ProcessContextRepositoryFacade repositoryFacade;

    @Builder
    private ProcessContextImpl(@NonNull UUID processInstanceId,
                               @NonNull String originProcessId,
                               @NonNull String processName,
                               @NonNull ProcessState processState,
                               @NonNull List<Message> messages,
                               @NonNull ProcessContextRepositoryFacade repositoryFacade) {
        this.processInstanceId = processInstanceId;
        this.originProcessId = originProcessId;
        this.processName = processName;
        this.processState = processState;
        this.messages = messages;
        this.messagesByName = messages.stream()
                .collect(groupingBy(Message::getName));
        this.repositoryFacade = repositoryFacade;
    }

    @Override
    public List<Message> getMessagesByName(String messageName) {
        return messagesByName.getOrDefault(messageName, List.of());
    }

    @Override
    public boolean isAllTasksInFinalState() {
        return repositoryFacade.isAllTasksInFinalState(processInstanceId);
    }
}
