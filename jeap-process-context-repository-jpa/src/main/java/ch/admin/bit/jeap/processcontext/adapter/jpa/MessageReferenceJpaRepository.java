package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.MessageReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
interface MessageReferenceJpaRepository extends JpaRepository<MessageReference, UUID> {

    boolean existsByProcessInstanceIdAndMessageId(UUID processInstanceId, UUID messageId);

    /**
     * Finds all message references for a process instance with their associated messages.
     */
    @Query("""
            select new ch.admin.bit.jeap.processcontext.adapter.jpa.MessageReferenceWithMessage(r, e, p.processTemplateName)
            from MessageReference r
            join r.processInstance p
            join events e on e.id = r.messageId
            where p.id = :processInstanceId
            """)
    List<MessageReferenceWithMessage> findMessageReferencesWithMessagesByProcessInstanceId(@Param("processInstanceId") UUID processInstanceId);

    /**
     * Finds the last message creation timestamp for each process instance in the given list.
     */
    @Query("""
            select r.processInstance.id as processInstanceId, max(e.createdAt) as lastMessageCreatedAt
            from MessageReference r
            join events e on e.id = r.messageId
            where r.processInstance.id in :processInstanceIds
            group by r.processInstance.id
            """)
    List<ProcessInstanceLastMessageProjection> findLastMessageCreatedAtByProcessInstanceIds(
            @Param("processInstanceIds") Collection<UUID> processInstanceIds);
}
