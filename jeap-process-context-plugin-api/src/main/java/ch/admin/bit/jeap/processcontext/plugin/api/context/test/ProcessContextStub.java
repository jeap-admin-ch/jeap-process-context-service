package ch.admin.bit.jeap.processcontext.plugin.api.context.test;

import ch.admin.bit.jeap.processcontext.plugin.api.context.Message;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext;
import ch.admin.bit.jeap.processcontext.plugin.api.message.MessageData;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import static java.util.stream.Collectors.*;

/**
 * Test stub implementation of {@link ProcessContext}. Useful for testing conditions.
 */
public class ProcessContextStub implements ProcessContext {

    @Getter
    private final String originProcessId;

    @Getter
    private final String processName;

    private final List<Message> messages;


    private final boolean allTasksCompleted;

    @Builder
    private ProcessContextStub(@NonNull String originProcessId,
                               @NonNull String processName,
                               List<Message> messages,
                               boolean allTasksCompleted) {
        this.originProcessId = originProcessId;
        this.processName = processName;
        this.messages = Objects.requireNonNullElse(messages, List.of());
        this.allTasksCompleted = allTasksCompleted;
    }

    @Override
    public boolean areAllTasksInFinalState() {
        return allTasksCompleted;
    }

    @Override
    public boolean containsMessageByTypeWithMessageData(String messageType, String messageDataKey, String messageDataValue) {
        List<Message> matches = messages.stream().filter(message -> message.getName().equals(messageType)).toList();
        return matches.stream()
                .anyMatch(message -> message.getMessageData().contains(new MessageData(messageDataKey, messageDataValue)));
    }

    @Override
    public boolean containsMessageByTypeWithAnyMessageDataValue(String messageType, String messageDataKey, Set<String> messageDataValues) {
        return containsMessageByTypeWithAnyMessageDataKeyValue(messageType, Map.of(messageDataKey, messageDataValues));
    }

    @Override
    public boolean containsMessageByTypeWithAnyMessageDataKeyValue(String messageType, Map<String, Set<String>> messageDataFilter) {
        List<Message> matches = messages.stream().filter(message -> message.getName().equals(messageType)).toList();
        return matches.stream().anyMatch(message -> matchesMessageData(message, messageDataFilter));
    }

    @Override
    public long countMessagesByTypeWithMessageData(String messageType, String messageDataKey, String messageDataValue) {
        return countMessagesByTypeWithAnyMessageData(messageType, Map.of(messageDataKey, messageDataValue));
    }

    @Override
    public long countMessagesByTypeWithAnyMessageData(String messageType, Map<String, String> messageDataFilter) {
        List<Message> matches = messages.stream().filter(message -> message.getName().equals(messageType)).toList();
        return matches.stream()
                .filter(message -> messageDataFilter.entrySet().stream()
                        .anyMatch(filterEntry -> message.getMessageData().contains(
                                new MessageData(filterEntry.getKey(), filterEntry.getValue()))))
                .count();
    }

    @Override
    public long countMessagesByType(String messageType) {
        return countMessagesByTypes(Set.of(messageType)).get(messageType);
    }

    @Override
    public Map<String, Long> countMessagesByTypes(Set<String> messageTypes) {
        Map<String, Long> result = messageTypes.stream().collect(toMap(type -> type, type -> 0L));
        result.putAll(messages.stream()
                .filter(message -> messageTypes.contains(message.getName()))
                .collect(groupingBy(Message::getName, counting())));
        return result;
    }

    @Override
    public Set<MessageData> getMessageDataForMessageType(String messageType) {
        return messages.stream()
                .filter(message -> message.getName().equals(messageType))
                .findFirst()
                .map(Message::getMessageData).orElse(Set.of());
    }

    @Override
    public boolean containsMessageOfAnyType(Set<String> messageType) {
        return messageType.stream().anyMatch(this::containsMessageOfType);
    }

    @Override
    public boolean containsMessageOfType(String messageType) {
        return messages.stream().anyMatch(msg -> msg.getName().equals(messageType));
    }

    private static boolean matchesMessageData(Message message, Map<String, Set<String>> messageDataFilter) {
        return messageDataFilter.entrySet().stream()
                .anyMatch(filterEntry -> matchesMessageData(message, filterEntry));
    }

    private static boolean matchesMessageData(Message message, Entry<String, Set<String>> filterEntry) {
        return message.getMessageData().stream()
                .anyMatch(messageData -> matchesMessageData(messageData, filterEntry));
    }

    private static boolean matchesMessageData(MessageData messageData, Entry<String, Set<String>> filterEntry) {
        return filterEntry.getValue().contains(messageData.getValue())
                && messageData.getKey().equals(filterEntry.getKey());
    }
}
