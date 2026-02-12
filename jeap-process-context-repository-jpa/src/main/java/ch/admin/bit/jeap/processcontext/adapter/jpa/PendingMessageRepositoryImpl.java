package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.PendingMessage;
import ch.admin.bit.jeap.processcontext.domain.processinstance.PendingMessageRepository;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Timed(value = "jeap.pcs.repository.pendingmessage", percentiles = .95)
public class PendingMessageRepositoryImpl implements PendingMessageRepository {

    private final PendingMessageJpaRepository pendingMessageJpaRepository;

    @Override
    public void saveIfNew(PendingMessage pendingMessage) {
        pendingMessageJpaRepository.saveIfNew(pendingMessage.getOriginProcessId(), pendingMessage.getMessageId());
    }

    @Override
    public List<PendingMessage> findByOriginProcessId(String originProcessId) {
        return pendingMessageJpaRepository.findByOriginProcessId(originProcessId);
    }

    @Override
    public void deleteAll(List<PendingMessage> pendingMessages) {
        pendingMessageJpaRepository.deleteAll(pendingMessages);
    }

    @Override
    public void deleteAll(Set<UUID> pendingMessageIds) {
        pendingMessageJpaRepository.deleteAllByIds(pendingMessageIds);
    }

    @Override
    public Slice<UUID> findPendingMessagesCreatedBefore(ZonedDateTime createdBefore, Pageable pageable) {
        return pendingMessageJpaRepository.findPendingMessagesCreatedBefore(createdBefore, pageable);
    }
}
