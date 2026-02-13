package ch.admin.bit.jeap.processcontext.domain.message;

import ch.admin.bit.jeap.processcontext.domain.processinstance.MessageReference;
import ch.admin.bit.jeap.processcontext.domain.processinstance.MessageReferenceMessageDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface MessageReferenceRepository {

    MessageReference save(MessageReference messageReference);

    boolean existsByProcessInstanceIdAndMessageId(UUID processInstanceId, UUID messageId);

    MessageReferenceMessageDTO findByProcessInstanceIdAndMessageId(UUID processInstanceId, UUID messageId);

    List<MessageReferenceMessageDTO> findByProcessInstanceId(UUID processInstanceId);

    Page<MessageReferenceMessageDTO> findByProcessInstanceId(UUID processInstanceId, Pageable pageable);

    /**
     * Finds the last message creation timestamp for each process instance in the given collection.
     *
     * @param processInstanceIds the collection of process instance IDs to query
     * @return a map of process instance ID to the last message creation timestamp
     */
    Map<UUID, ZonedDateTime> findLastMessageCreatedAtByProcessInstanceIds(Collection<UUID> processInstanceIds);

}
