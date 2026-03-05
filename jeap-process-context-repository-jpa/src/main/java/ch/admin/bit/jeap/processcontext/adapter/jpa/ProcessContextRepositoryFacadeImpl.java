package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextRepositoryFacade;
import ch.admin.bit.jeap.processcontext.plugin.api.message.MessageData;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@Component
@RequiredArgsConstructor
@Timed(value = "jeap_pcs_repository_processcontextfacade")
public class ProcessContextRepositoryFacadeImpl implements ProcessContextRepositoryFacade {

    private final ProcessInstanceJpaRepository processInstanceJpaRepository;
    private final MessageJpaRepository messageJpaRepository;
    private final MessageSearchJpaRepository messageSearchJpaRepository;

    @Override
    public boolean areAllTasksInFinalState(UUID processInstanceId) {
        return Boolean.TRUE.equals(processInstanceJpaRepository.areAllTasksInFinalState(processInstanceId));
    }

    @Override
    public boolean containsMessageOfType(UUID processInstanceId, String messageType) {
        return messageJpaRepository.containsMessageOfType(processInstanceId, messageType);
    }

    @Override
    public boolean containsMessageOfAnyType(UUID processInstanceId, Set<String> messageTypes) {
        return messageJpaRepository.containsMessageOfAnyType(processInstanceId, messageTypes);
    }

    @Override
    public Set<MessageData> getMessageDataForMessageType(UUID processInstanceId, String messageType, String processTemplateName) {
        return messageJpaRepository.findMessageDataForMessageType(processInstanceId, messageType, processTemplateName).stream()
                .map(MessageDataProjection::toMessageData)
                .collect(toSet());
    }

    @Override
    public long countMessagesByType(UUID processInstanceId, String messageType) {
        return messageJpaRepository.countMessagesByType(processInstanceId, messageType);
    }

    @Override
    public Map<String, Long> countMessagesByTypes(UUID processInstanceId, Set<String> messageTypes) {
        Map<String, Long> result = initializeCountPerTypeToZero(messageTypes);
        messageJpaRepository.countMessagesByTypes(processInstanceId, messageTypes).forEach(count ->
                result.put(count.getMessageName(), count.getMessageCount()));
        return result;
    }

    private static Map<String, Long> initializeCountPerTypeToZero(Set<String> messageTypes) {
        return messageTypes.stream().collect(toMap(type -> type, type -> 0L));
    }

    @Override
    public long countMessagesByTypeWithMessageData(UUID processInstanceId, String messageType, String messageDataKey, String messageDataValue, String processTemplateName) {
        return messageJpaRepository.countMessagesByTypeWithMessageData(processInstanceId, messageType, messageDataKey, messageDataValue, processTemplateName);
    }

    @Override
    public boolean containsMessageByTypeWithMessageData(UUID processInstanceId, String messageType, String messageDataKey, String messageDataValue, String processTemplateName) {
        return messageJpaRepository.containsMessageByTypeWithMessageData(processInstanceId, messageType, messageDataKey, messageDataValue, processTemplateName);
    }

    @Override
    public long countMessagesByTypeWithAnyMessageData(UUID processInstanceId, String messageType, Map<String, String> messageDataFilter, String processTemplateName) {
        return messageSearchJpaRepository.countMessagesByTypeWithAnyMessageData(processInstanceId, messageType, messageDataFilter, processTemplateName);
    }

    @Override
    public boolean containsMessageByTypeWithAnyMessageDataValue(UUID processInstanceId, String messageType, String messageDataKey, Set<String> messageDataValues, String processTemplateName) {
        return messageJpaRepository.containsMessageByTypeWithAnyMessageDataValue(processInstanceId, messageType, messageDataKey, messageDataValues, processTemplateName);
    }

    @Override
    public boolean containsMessageByTypeWithAnyMessageDataKeyValue(UUID processInstanceId, String messageType, Map<String, Set<String>> messageDataFilter, String processTemplateName) {
        return messageSearchJpaRepository.containsMessageByTypeWithAnyMessageDataKeyValue(processInstanceId, messageType, messageDataFilter, processTemplateName);
    }
}
