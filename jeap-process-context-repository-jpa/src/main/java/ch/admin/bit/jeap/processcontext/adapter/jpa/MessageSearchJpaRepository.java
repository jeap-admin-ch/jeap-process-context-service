package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.message.MessageData;
import ch.admin.bit.jeap.processcontext.domain.processinstance.MessageReference;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
class MessageSearchJpaRepository {

    private final EntityManager entityManager;

    /**
     * Executes a JPA query equivalent to the following SQL to check if at least one message of the given type exists
     * for the process instance and has at least one message data entry matching any of the key/values specified in the filter:
     * <pre>
     * SELECT CASE WHEN EXISTS (
     *       SELECT 1 FROM event_reference r
     *       JOIN events e ON e.id = r.events_id
     *       JOIN events_event_data d ON d.events_id = e.id
     *       WHERE r.process_instance_id = :processInstanceId
     *         AND e.event_name = :messageType
     *         AND d.template_name = :processTemplateName
     *         AND (
     *             (d.key_ = :key1 AND d.value_ IN (:values1))
     *             OR (d.key_ = :key2 AND d.value_ IN (:values2))
     *             ...
     *         )
     *   ) THEN TRUE ELSE FALSE END
     * </pre>
     *
     * @param processInstanceId  ID of the process instance to check messages for
     * @param messageType        jEAP message type name to check messages for
     * @param messageDataFilter  a map of message data key to a set of acceptable values
     * @param processTemplateName Name of the process template
     * @return <code>true</code> if at least one message of the given type exists for the process instance and has at least
     * one message data entry matching any of the key/values specified in the filter, <code>false</code> otherwise.
     */
    boolean containsMessageByTypeWithAnyMessageDataKeyValue(UUID processInstanceId, String messageType, Map<String, Set<String>> messageDataFilter, String processTemplateName) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Boolean> query = cb.createQuery(Boolean.class);

        // Build EXISTS subquery
        Subquery<Integer> subquery = query.subquery(Integer.class);
        Root<MessageReference> messageReferenceRoot = subquery.from(MessageReference.class);

        // Since MessageReference.messageId is a UUID (not a @ManyToOne), we need a separate root for Message
        Root<Message> messageRoot = subquery.from(Message.class);
        Join<Message, MessageData> messageDataJoin = messageRoot.join("messageData");

        // Build OR predicates for each key/values entry in the filter
        List<Predicate> orPredicates = buildKeyValueMatchPredicates(messageDataFilter, cb, messageDataJoin);

        // Combine all conditions
        Predicate processInstanceMatches = cb.equal(messageReferenceRoot.get("processInstance").get("id"), processInstanceId);
        Predicate messageIdJoin = cb.equal(messageRoot.get("id"), messageReferenceRoot.get("messageId"));
        Predicate messageTypeMatches = cb.equal(messageRoot.get("messageName"), messageType);
        Predicate templateNameMatches = cb.equal(messageDataJoin.get("templateName"), processTemplateName);
        Predicate anyKeyValueMatches = cb.or(orPredicates.toArray(new Predicate[0]));

        subquery.select(cb.literal(1));
        subquery.where(cb.and(
                processInstanceMatches,
                messageIdJoin,
                messageTypeMatches,
                templateNameMatches,
                anyKeyValueMatches
        ));

        // Main query: SELECT CASE WHEN EXISTS(...) THEN TRUE ELSE FALSE END
        query.select(cb.<Boolean>selectCase()
                .when(cb.exists(subquery), true)
                .otherwise(false));

        return Boolean.TRUE.equals(entityManager.createQuery(query).getSingleResult());
    }

    /**
     * Builds a list of OR predicates for matching message data key/value pairs.
     */
    private static List<Predicate> buildKeyValueMatchPredicates(Map<String, Set<String>> messageDataFilter, CriteriaBuilder cb, Join<Message, MessageData> d) {
        List<Predicate> orPredicates = new ArrayList<>();
        messageDataFilter.forEach((dataKey, dataValues) -> {
            Predicate keyMatches = cb.equal(d.get("key"), dataKey);
            Predicate valueMatches = d.get("value").in(dataValues);
            orPredicates.add(cb.and(keyMatches, valueMatches));
        });
        return orPredicates;
    }

    /**
     * Counts messages by name that satisfy message-data predicates. The message will be matched if any message data
     * key/value pair matching the key to any of the provided values.
     * <p>
     * Executes a JPA query equivalent to the following SQL:
     * <pre>
     * SELECT COUNT(DISTINCT e.id)
     * FROM event_reference r
     * JOIN events e ON e.id = r.events_id
     * JOIN events_event_data d ON d.events_id = e.id
     * WHERE r.process_instance_id = :processInstanceId
     *   AND e.message_name = :messageType
     *   AND d.template_name = :processTemplateName
     *   AND (
     *       (d.key_ = :key1 AND d.value_ = :value1)
     *       OR (d.key_ = :key2 AND d.value_ = :value2)
     *       ...
     *   )
     * </pre>
     *
     * @param processInstanceId  the process instance ID
     * @param messageType        message types
     * @param messageDataFilter  message data key / value pairs, message is only counted if any of the key/value pairs match
     * @param processTemplateName Name of the process template
     * @return count of matching messages
     */
    long countMessagesByTypeWithAnyMessageData(UUID processInstanceId, String messageType, Map<String, String> messageDataFilter, String processTemplateName) {
        if (messageDataFilter.isEmpty()) {
            return 0;
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);

        Root<MessageReference> messageReferenceRoot = query.from(MessageReference.class);

        // Since MessageReference.messageId is a UUID (not a @ManyToOne), we need a separate root for Message
        Root<Message> messageRoot = query.from(Message.class);
        Join<Message, MessageData> messageDataJoin = messageRoot.join("messageData");

        // Build OR predicates for each key/value entry in the filter
        List<Predicate> orPredicates = buildKeyValueMatchPredicatesForSingleValues(messageDataFilter, cb, messageDataJoin);

        // Combine all conditions
        Predicate processInstanceMatches = cb.equal(messageReferenceRoot.get("processInstance").get("id"), processInstanceId);
        Predicate messageIdJoin = cb.equal(messageRoot.get("id"), messageReferenceRoot.get("messageId"));
        Predicate messageTypeMatches = cb.equal(messageRoot.get("messageName"), messageType);
        Predicate templateNameMatches = cb.equal(messageDataJoin.get("templateName"), processTemplateName);
        Predicate anyKeyValueMatches = cb.or(orPredicates.toArray(new Predicate[0]));

        query.select(cb.countDistinct(messageRoot));
        query.where(cb.and(
                processInstanceMatches,
                messageIdJoin,
                messageTypeMatches,
                templateNameMatches,
                anyKeyValueMatches
        ));

        return entityManager.createQuery(query).getSingleResult();
    }

    /**
     * Builds a list of OR predicates for matching message data key/value pairs (single values).
     */
    private static List<Predicate> buildKeyValueMatchPredicatesForSingleValues(Map<String, String> messageDataFilter, CriteriaBuilder cb, Join<Message, MessageData> d) {
        List<Predicate> orPredicates = new ArrayList<>();
        messageDataFilter.forEach((dataKey, dataValue) -> {
            Predicate keyMatches = cb.equal(d.get("key"), dataKey);
            Predicate valueMatches = cb.equal(d.get("value"), dataValue);
            orPredicates.add(cb.and(keyMatches, valueMatches));
        });
        return orPredicates;
    }
}
