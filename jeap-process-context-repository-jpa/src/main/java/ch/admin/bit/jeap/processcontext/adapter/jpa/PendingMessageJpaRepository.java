package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.PendingMessage;
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
public interface PendingMessageJpaRepository extends JpaRepository<PendingMessage, Long> {

    List<PendingMessage> findByOriginProcessId(String originProcessId);

    @Modifying
    @Query(value = """
            INSERT INTO pending_message (id, origin_process_id, message_id, created_at)
            VALUES (NEXTVAL('pending_message_id_seq'), :originProcessId, :messageId, NOW())
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    void saveIfNew(@Param("originProcessId") String originProcessId, @Param("messageId") UUID messageId);

    @Query("select pm.id from PendingMessage pm where pm.createdAt < :createdBefore")
    Slice<UUID> findPendingMessagesCreatedBefore(@Param("createdBefore") ZonedDateTime createdBefore, Pageable pageable);

    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM pending_message WHERE id IN :ids")
    void deleteAllByIds(@Param("ids") Set<UUID> ids);
}
