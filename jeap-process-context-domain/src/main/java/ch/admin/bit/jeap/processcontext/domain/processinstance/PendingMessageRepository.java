package ch.admin.bit.jeap.processcontext.domain.processinstance;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface PendingMessageRepository {

    void saveIfNew(PendingMessage pendingMessage);

    List<PendingMessage> findByOriginProcessId(String originProcessId);

    void deleteAll(List<PendingMessage> pendingMessages);

    void deleteAll(Set<UUID> pendingMessageIds);

    Slice<UUID> findPendingMessagesCreatedBefore(ZonedDateTime createdBefore, Pageable pageable);
}
