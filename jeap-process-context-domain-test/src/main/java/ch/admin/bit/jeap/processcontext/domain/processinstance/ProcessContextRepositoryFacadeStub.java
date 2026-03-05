package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextRepositoryFacade;
import ch.admin.bit.jeap.processcontext.plugin.api.message.MessageData;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ProcessContextRepositoryFacadeStub implements ProcessContextRepositoryFacade {

    private boolean allTasksInFinalState = false;

    @Override
    public boolean areAllTasksInFinalState(UUID processInstanceId) {
        return allTasksInFinalState;
    }

    public void setAllTasksInFinalState(boolean allTasksInFinalState) {
        this.allTasksInFinalState = allTasksInFinalState;
    }

    @Override
    public boolean containsMessageOfType(UUID processInstanceId, String messageType) {
        throw unsupported();
    }

    @Override
    public boolean containsMessageOfAnyType(UUID processInstanceId, Set<String> messageTypes) {
        throw unsupported();
    }

    @Override
    public Set<MessageData> getMessageDataForMessageType(UUID processInstanceId, String messageType, String processTemplateName) {
        throw unsupported();
    }

    @Override
    public long countMessagesByType(UUID processInstanceId, String messageType) {
        throw unsupported();
    }

    @Override
    public Map<String, Long> countMessagesByTypes(UUID processInstanceId, Set<String> messageTypes) {
        throw unsupported();
    }

    @Override
    public long countMessagesByTypeWithMessageData(UUID processInstanceId, String messageType, String messageDataKey, String messageDataValue, String processTemplateName) {
        throw unsupported();
    }

    @Override
    public long countMessagesByTypeWithAnyMessageData(UUID processInstanceId, String messageType, Map<String, String> messageDataFilter, String processTemplateName) {
        throw unsupported();
    }

    @Override
    public boolean containsMessageByTypeWithAnyMessageDataValue(UUID processInstanceId, String messageType, String messageDataKey, Set<String> messageDataValues, String processTemplateName) {
        throw unsupported();
    }

    @Override
    public boolean containsMessageByTypeWithMessageData(UUID processInstanceId, String messageType, String messageDataKey, String messageDataValue, String processTemplateName) {
        throw unsupported();
    }

    @Override
    public boolean containsMessageByTypeWithAnyMessageDataKeyValue(UUID processInstanceId, String messageType, Map<String, Set<String>> messageDataFilter, String processTemplateName) {
        throw unsupported();
    }

    private static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("Not supported in stub");
    }
}
