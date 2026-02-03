package ch.admin.bit.jeap.processcontext.plugin.api.context;

import ch.admin.bit.jeap.processcontext.plugin.api.message.MessageData;

import java.util.Map;
import java.util.Set;

/**
 * Provides query methods to access messages within a process context when implementing conditions. These query methods
 * will provide optimize PCS database access for conditions. Condition implementations should be simple and fast. If
 * you miss a certain accessor method when implementing conditions against this interface, please get in touch with the
 * JEAP team to request its addition. This also applies if you find a condition having to loop over many methods in
 * this interface to achieve a certain result - this is a sign that an optimized accessor method is missing.
 */
public interface ProcessContextMessageQueryRepository {
    /**
     * Checks whether the process context contains at least one message having the specified type.
     *
     * @param messageType the jEAP message type to check for
     * @return true if at least one message of the specified type exists, false otherwise
     */
    boolean containsMessageOfType(String messageType);

    /**
     * Checks whether the process context contains at least one message having any of the specified types.
     *
     * @param messageType the jEAP message types to check for
     * @return true if at least one message of any of the specified types exists, false otherwise
     */
    boolean containsMessageOfAnyType(Set<String> messageType);

    /**
     * Returns the message data of the latest message with the given name as a key/value map.
     *
     * @param messageType the jEAP message type to retrieve the message data for
     * @return message data map for the latest message or empty if no such message exists / if it does not contain any message data
     */
    Set<MessageData> getMessageDataForMessageType(String messageType);

    /**
     * Counts messages for a message type
     *
     * @param messageType The jEAP message type of the messages to count
     * @return count of messages with the given message type
     */
    long countMessagesByType(String messageType);

    /**
     * Counts messages for multiple message type
     *
     * @param messageTypes The jEAP message types of the messages to count
     * @return map of messageName -> count, same size as the input collection
     */
    Map<String, Long> countMessagesByTypes(Set<String> messageTypes);

    /**
     * Counts messages by name that satisfy message-data predicates. The message will be matched only if it contains
     * the specified message data key/value pair.
     *
     * @param messageType      The jEAP message types to look up message data for
     * @param messageDataKey   the message data key to match
     * @param messageDataValue the message data value to match
     * @return count of matching messages
     */
    long countMessagesByTypeWithMessageData(String messageType, String messageDataKey, String messageDataValue);

    /**
     * Counts messages by name that satisfy message-data predicates.
     *
     * @param messageType       message types
     * @param messageDataFilter message data key / value pairs, message is only counted if key/value both match
     * @return count of matching messages
     */
    long countMessagesByTypeWithAnyMessageData(String messageType, Map<String, String> messageDataFilter);

    /**
     * Checks whether at least one message by name exists with the specified message data key/value pair.
     *
     * @param messageType      The jEAP message type to look up message data for
     * @param messageDataKey   the message data key to match
     * @param messageDataValue the message data value to match
     * @return true if at least one matching message exists, false otherwise
     */
    boolean containsMessageByTypeWithMessageData(String messageType, String messageDataKey, String messageDataValue);

    /**
     * Checks whether at least one message by name exists that contains a message data key/value pair matching the key
     * to any of the provided values.
     *
     * @param messageType       The jEAP message types to look up message data for
     * @param messageDataKey    message data key to mtach
     * @param messageDataValues message data values to match
     * @return true if at least one matching message exists, false otherwise
     */
    boolean containsMessageByTypeWithAnyMessageDataValue(String messageType, String messageDataKey, Set<String> messageDataValues);

    /**
     * Checks whether at least one message by name exists that contains a message data key/value pair matching the key
     * to any of the provided values.
     *
     * @param messageType       The jEAP message types to look up message data for
     * @param messageDataFilter message data key / values pairs, message is only counted if a matching key/value pair is found
     * @return true if at least one matching message exists, false otherwise
     */
    boolean containsMessageByTypeWithAnyMessageDataKeyValue(String messageType, Map<String, Set<String>> messageDataFilter);
}
