package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextRepositoryFacade;
import ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@Component
@RequiredArgsConstructor
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
    public Set<MessageData> getMessageDataForMessageType(UUID processInstanceId, String messageType) {
        return messageJpaRepository.findMessageDataForMessageType(processInstanceId, messageType).stream()
                .map(MessageDataProjection::toMessageData)
                .collect(toSet());
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
    public long countMessagesByTypeWithMessageData(UUID processInstanceId, String messageType, String messageDataKey, String messageDataValue) {
        return messageJpaRepository.countMessagesByTypeWithMessageData(processInstanceId, messageType, messageDataKey, messageDataValue);
    }

    @Override
    public boolean containsMessageByTypeWithMessageData(UUID processInstanceId, String messageType, String messageDataKey, String messageDataValue) {
        return messageJpaRepository.containsMessageByTypeWithMessageData(processInstanceId, messageType, messageDataKey, messageDataValue);
    }

    @Override
    public long countMessagesByTypeWithAnyMessageData(UUID processInstanceId, String messageType, Map<String, String> messageDataFilter) {
        return messageSearchJpaRepository.countMessagesByTypeWithAnyMessageData(processInstanceId, messageType, messageDataFilter);
    }

    @Override
    public boolean containsMessageByTypeWithAnyMessageDataValue(UUID processInstanceId, String messageType, String messageDataKey, Set<String> messageDataValues) {
        return messageJpaRepository.containsMessageByTypeWithAnyMessageDataValue(processInstanceId, messageType, messageDataKey, messageDataValues);
    }

    @Override
    public boolean containsMessageByTypeWithAnyMessageDataKeyValue(UUID processInstanceId, String messageType, Map<String, Set<String>> messageDataFilter) {
        return messageSearchJpaRepository.containsMessageByTypeWithAnyMessageDataKeyValue(processInstanceId, messageType, messageDataFilter);
    }
}
