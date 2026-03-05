package ch.admin.bit.jeap.processcontext.domain.message;

import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContextMessageQueryRepository;
import ch.admin.bit.jeap.processcontext.plugin.api.message.MessageData;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Repository interface for searching messages within a process context based on various criteria.
 * Used by conditions in the API layer via {@link ProcessContextMessageQueryRepository}.
 */
public interface MessageSearchQueryRepository {

    /**
     * Checks whether the process context contains at least one message having the specified type.
     *
     * @param processInstanceId the process instance ID
     * @param messageType       the jEAP message type to check for
     * @return true if at least one message of the specified type exists, false otherwise
     */
    boolean containsMessageOfType(UUID processInstanceId, String messageType);

    /**
     * Checks whether the process context contains at least one message having any of the specified types.
     *
     * @param processInstanceId the process instance ID
     * @param messageType       the jEAP message types to check for
     * @return true if at least one message of any of the specified types exists, false otherwise
     */
    boolean containsMessageOfAnyType(UUID processInstanceId, Set<String> messageType);

    /**
     * Returns the message data of the latest message with the given name as a key/value map.
     *
     * @param processInstanceId the process instance ID
     * @param messageType       the jEAP message type to retrieve the message data for
     * @param processTemplateName Name of the process template
     * @return message data map for the latest message or empty if no such message exists / if it does not contain any message data
     */
    Set<MessageData> getMessageDataForMessageType(UUID processInstanceId, String messageType, String processTemplateName);

    /**
     * Counts messages for a single message type.
     *
     * @param processInstanceId the process instance ID
     * @param messageType       The jEAP message type of the messages to count
     * @return count of messages of the given type
     */
    long countMessagesByType(UUID processInstanceId, String messageType);

    /**
     * Counts messages for multiple message type
     *
     * @param processInstanceId the process instance ID
     * @param messageTypes      The jEAP message types of the messages to count
     * @return map of messageName -> count, same size as the input collection
     */
    Map<String, Long> countMessagesByTypes(UUID processInstanceId, Set<String> messageTypes);

    /**
     * Counts messages by name that satisfy message-data predicates. The message will be matched only if it contains
     * the specified message data key/value pair.
     *
     * @param processInstanceId the process instance ID
     * @param messageType       The jEAP message types to look up message data for
     * @param messageDataKey    the message data key to match
     * @param messageDataValue  the message data value to match
     * @return count of matching messages
     */
    long countMessagesByTypeWithMessageData(UUID processInstanceId, String messageType, String messageDataKey, String messageDataValue, String processTemplateName);

    /**
     * Counts messages by name that satisfy message-data predicates. The message will be matched if any message data
     * key/value pair matching the key to any of the provided values.
     *
     * @param processInstanceId  the process instance ID
     * @param messageType        message types
     * @param messageDataFilter  message data key / value pairs, message is only counted if any of the key/value pairs match
     * @param processTemplateName Name of the process template
     * @return count of matching messages
     */
    long countMessagesByTypeWithAnyMessageData(UUID processInstanceId, String messageType, Map<String, String> messageDataFilter, String processTemplateName);

    /**
     * Checks whether at least one message by name exists that contains a matching message data key/value pair.
     *
     * @param processInstanceId  the process instance ID
     * @param messageType        The jEAP message types to look up message data for
     * @param messageDataKey     the message data key to match
     * @param messageDataValue   the message data value to match
     * @param processTemplateName Name of the process template
     * @return true if at least one matching message exists, false otherwise
     */
    boolean containsMessageByTypeWithMessageData(UUID processInstanceId, String messageType, String messageDataKey, String messageDataValue, String processTemplateName);

    /**
     * Checks whether at least one message by name exists that contains a message data key/value pair matching the key
     * to any of the provided values.
     *
     * @param processInstanceId  the process instance ID
     * @param messageType        The jEAP message types to look up message data for
     * @param messageDataKey     message data key to match
     * @param messageDataValues  message data values to match
     * @param processTemplateName Name of the process template
     * @return true if at least one matching message exists, false otherwise
     */
    boolean containsMessageByTypeWithAnyMessageDataValue(UUID processInstanceId, String messageType, String messageDataKey, Set<String> messageDataValues, String processTemplateName);

    /**
     * Checks whether at least one message by name exists that contains a message data key/value pair matching a key
     * to any of the provided values.
     *
     * @param processInstanceId  the process instance ID
     * @param messageType        The jEAP message types to look up message data for
     * @param messageDataFilter  message data key / value pairs, message is only counted if any of the key/value pairs match
     * @param processTemplateName Name of the process template
     * @return true if at least one matching message exists, false otherwise
     */
    boolean containsMessageByTypeWithAnyMessageDataKeyValue(UUID processInstanceId, String messageType, Map<String, Set<String>> messageDataFilter, String processTemplateName);
}
