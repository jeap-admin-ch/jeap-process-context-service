package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.message.MessageRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
interface MessageJpaRepository extends JpaRepository<Message, UUID>, MessageRepository {

    @Query("from events e join e.messageData d where e.messageName = :messageName and d.templateName = :messageDataTemplateName and d.key = :messageDataKey and d.value = :messageDataValue and d.role = :messageDataRole and e.id not in (:alreadyCorrelatedMessageIds)")
    List<Message> findMessagesToCorrelate(@Param("messageName") String messageName, @Param("messageDataTemplateName") String messageDataTemplateName, @Param("messageDataKey") String messageDataKey, @Param("messageDataValue") String messageDataValue, @Param("messageDataRole") String messageDataRole, @Param("alreadyCorrelatedMessageIds") List<UUID> alreadyCorrelatedMessageIds);

    @Query("from events e join e.messageData d where e.messageName = :messageName and d.templateName = :messageDataTemplateName and d.key = :messageDataKey and d.value = :messageDataValue and d.role = :messageDataRole")
    List<Message> findMessagesToCorrelate(@Param("messageName") String messageName, @Param("messageDataTemplateName") String messageDataTemplateName, @Param("messageDataKey") String messageDataKey, @Param("messageDataValue") String messageDataValue, @Param("messageDataRole") String messageDataRole);

    @Query("from events e join e.messageData d where e.messageName = :messageName and d.templateName = :messageDataTemplateName and d.key = :messageDataKey and d.value = :messageDataValue and d.role is null and e.id not in (:alreadyCorrelatedMessageIds)")
    List<Message> findMessagesToCorrelate(@Param("messageName") String messageName, @Param("messageDataTemplateName") String messageDataTemplateName, @Param("messageDataKey") String messageDataKey, @Param("messageDataValue") String messageDataValue, @Param("alreadyCorrelatedMessageIds") List<UUID> alreadyCorrelatedMessageIds);

    @Query("from events e join e.messageData d where e.messageName = :messageName and d.templateName = :messageDataTemplateName and d.key = :messageDataKey and d.value = :messageDataValue and d.role is null")
    List<Message> findMessagesToCorrelate(@Param("messageName") String messageName, @Param("messageDataTemplateName") String messageDataTemplateName, @Param("messageDataKey") String messageDataKey, @Param("messageDataValue") String messageDataValue);

    @Query(nativeQuery = true, value = "SELECT e.key_, e.value_ FROM events_user_data e WHERE e.events_id = :id")
    List<String[]> findMessageUserDataByMessageId(@Param("id") UUID messageId);

    @Query("select e.id from events e left join MessageReference er on e.id = er.messageId where e.createdAt < :createdBefore and er.id is null")
    Slice<UUID> findMessagesWithoutProcessCorrelation(@Param("createdBefore") ZonedDateTime createdBefore, Pageable pageable);

    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM events_event_data e WHERE e.events_id in (:messageIds) ")
    void deleteMessageDataByMessageIds(@Param("messageIds") Set<UUID> messageIds);

    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM events_user_data e WHERE e.events_id in (:messageIds) ")
    void deleteMessageUserDataByMessageIds(@Param("messageIds") Set<UUID> messageIds);

    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM events_origin_task_ids e WHERE e.events_id in (:messageIds) ")
    void deleteOriginTaskIdByMessageIds(@Param("messageIds") Set<UUID> messageIds);

    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM events e WHERE e.id in (:messageIds) ")
    void deleteMessageByIds(@Param("messageIds") Set<UUID> messageIds);

    /**
     * Checks if the process instance contains at least one message of the given type.
     */
    @Query("""
            select case when exists (
                select 1 from ProcessInstance p
                join p.messageReferences r
                join events e on e.id = r.messageId
                where p.id = :processInstanceId and e.messageName = :messageType
            ) then true else false end
            """)
    boolean containsMessageOfType(@Param("processInstanceId") UUID processInstanceId,
                                  @Param("messageType") String messageType);

    /**
     * Checks if the process instance contains at least one message of any of the given types.
     */
    @Query("""
            select case when exists (
                select 1 from ProcessInstance p
                join p.messageReferences r
                join events e on e.id = r.messageId
                where p.id = :processInstanceId and e.messageName in :messageTypes
            ) then true else false end
            """)
    boolean containsMessageOfAnyType(@Param("processInstanceId") UUID processInstanceId,
                                     @Param("messageTypes") Set<String> messageTypes);

    /**
     * Retrieves message data for messages of a given type within a process instance.
     */
    @Query("""
            select d.key as key, d.value as value, d.role as role
            from ProcessInstance p
            join p.messageReferences r
            join events e on e.id = r.messageId
            join e.messageData d
            where p.id = :processInstanceId
              and e.messageName = :messageType
              and d.templateName = p.processTemplateName
            """)
    List<MessageDataProjection> findMessageDataForMessageType(@Param("processInstanceId") UUID processInstanceId,
                                                              @Param("messageType") String messageType);

    /**
     * Counts messages by type for multiple message types within a process instance.
     */
    @Query("""
            select e.messageName as messageName, count(e) as messageCount
            from ProcessInstance p
            join p.messageReferences r
            join events e on e.id = r.messageId
            where p.id = :processInstanceId and e.messageName in :messageTypes
            group by e.messageName
            """)
    List<MessageTypeCount> countMessagesByTypes(@Param("processInstanceId") UUID processInstanceId,
                                                @Param("messageTypes") Set<String> messageTypes);

    /**
     * Counts messages by type that have the specified message data key/value pair.
     */
    @Query("""
            select count(distinct e)
            from ProcessInstance p
            join p.messageReferences r
            join events e on e.id = r.messageId
            join e.messageData d
            where p.id = :processInstanceId
              and e.messageName = :messageType
              and d.templateName = p.processTemplateName
              and d.key = :dataKey
              and d.value = :dataValue
            """)
    long countMessagesByTypeWithMessageData(@Param("processInstanceId") UUID processInstanceId,
                                            @Param("messageType") String messageType,
                                            @Param("dataKey") String dataKey,
                                            @Param("dataValue") String dataValue);

    /**
     * Checks if any message of the given type exists with the specified message data key and any of the given values.
     */
    @Query("""
            select case when exists (
                select 1 from ProcessInstance p
                join p.messageReferences r
                join events e on e.id = r.messageId
                join e.messageData d
                where p.id = :processInstanceId
                  and e.messageName = :messageType
                  and d.templateName = p.processTemplateName
                  and d.key = :dataKey
                  and d.value in :dataValues
            ) then true else false end
            """)
    boolean containsMessageByTypeWithAnyMessageDataValue(@Param("processInstanceId") UUID processInstanceId,
                                                         @Param("messageType") String messageType,
                                                         @Param("dataKey") String dataKey,
                                                         @Param("dataValues") Set<String> dataValues);

    /**
     * Checks if any message of the given type exists with the specified message data key/value pair.
     */
    @Query("""
            select case when exists (
                select 1 from ProcessInstance p
                join p.messageReferences r
                join events e on e.id = r.messageId
                join e.messageData d
                where p.id = :processInstanceId
                  and e.messageName = :messageType
                  and d.templateName = p.processTemplateName
                  and d.key = :messageDataKey
                  and d.value = :messageDataValue
            ) then true else false end
            """)
    boolean containsMessageByTypeWithMessageData(@Param("processInstanceId") UUID processInstanceId,
                                                 @Param("messageType") String messageType,
                                                 @Param("messageDataKey") String messageDataKey,
                                                 @Param("messageDataValue") String messageDataValue);
}
