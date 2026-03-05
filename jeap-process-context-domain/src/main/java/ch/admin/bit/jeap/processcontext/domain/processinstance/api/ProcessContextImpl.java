package ch.admin.bit.jeap.processcontext.domain.processinstance.api;

import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext;
import ch.admin.bit.jeap.processcontext.plugin.api.message.MessageData;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ProcessContextImpl implements ProcessContext {

    @NonNull
    private final UUID processInstanceId;

    @Getter
    private final String originProcessId;

    private final String processTemplate;

    private final ProcessContextRepositoryFacade repositoryFacade;

    @Builder
    private ProcessContextImpl(@NonNull UUID processInstanceId,
                               @NonNull String originProcessId,
                               @NonNull String processTemplate,
                               @NonNull ProcessContextRepositoryFacade repositoryFacade) {
        this.processInstanceId = processInstanceId;
        this.originProcessId = originProcessId;
        this.processTemplate = processTemplate;
        this.repositoryFacade = repositoryFacade;
    }

    @Override
    public String getProcessName() {
        return processTemplate;
    }

    @Override
    public boolean areAllTasksInFinalState() {
        return repositoryFacade.areAllTasksInFinalState(processInstanceId);
    }

    @Override
    public boolean containsMessageOfType(String messageType) {
        return repositoryFacade.containsMessageOfType(processInstanceId, messageType);
    }

    @Override
    public boolean containsMessageOfAnyType(Set<String> messageType) {
        return repositoryFacade.containsMessageOfAnyType(processInstanceId, messageType);
    }

    @Override
    public Set<MessageData> getMessageDataForMessageType(String messageType) {
        return repositoryFacade.getMessageDataForMessageType(processInstanceId, messageType, processTemplate);
    }

    @Override
    public long countMessagesByType(String messageType) {
        return repositoryFacade.countMessagesByType(processInstanceId, messageType);
    }

    @Override
    public Map<String, Long> countMessagesByTypes(Set<String> messageTypes) {
        return repositoryFacade.countMessagesByTypes(processInstanceId, messageTypes);
    }

    @Override
    public long countMessagesByTypeWithMessageData(String messageType, String messageDataKey, String messageDataValue) {
        return repositoryFacade.countMessagesByTypeWithMessageData(processInstanceId, messageType, messageDataKey, messageDataValue, processTemplate);
    }

    @Override
    public long countMessagesByTypeWithAnyMessageData(String messageType, Map<String, String> messageDataFilter) {
        return repositoryFacade.countMessagesByTypeWithAnyMessageData(processInstanceId, messageType, messageDataFilter, processTemplate);
    }

    @Override
    public boolean containsMessageByTypeWithMessageData(String messageType, String messageDataKey, String messageDataValue) {
        return repositoryFacade.containsMessageByTypeWithMessageData(processInstanceId, messageType, messageDataKey, messageDataValue, processTemplate);
    }

    @Override
    public boolean containsMessageByTypeWithAnyMessageDataValue(String messageType, String messageDataKey, Set<String> messageDataValues) {
        return repositoryFacade.containsMessageByTypeWithAnyMessageDataValue(processInstanceId, messageType, messageDataKey, messageDataValues, processTemplate);
    }

    @Override
    public boolean containsMessageByTypeWithAnyMessageDataKeyValue(String messageType, Map<String, Set<String>> messageDataFilter) {
        return repositoryFacade.containsMessageByTypeWithAnyMessageDataKeyValue(processInstanceId, messageType, messageDataFilter, processTemplate);
    }
}
