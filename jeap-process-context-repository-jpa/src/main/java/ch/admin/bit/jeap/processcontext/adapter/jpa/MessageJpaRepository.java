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
}
