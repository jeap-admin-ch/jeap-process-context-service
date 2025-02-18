package ch.admin.bit.jeap.processcontext.domain.message;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageQueryRepository {
    Optional<Message> findByMessageNameAndIdempotenceId(String messageName, String idempotenceId);

    Optional<Message> findById(UUID id);

    List<String[]> findMessageUserDataByMessageId(UUID messageId);

    List<Message> findMessagesToCorrelate(String messageName, String messageDataTemplateName, String messageDataKey, String messageDataValue, String messageDataRole, List<UUID> alreadyCorrelatedMessageIds);

    List<Message> findMessagesToCorrelate(String messageName, String messageDataTemplateName, String messageDataKey, String messageDataValue, String messageDataRole);

    List<Message> findMessagesToCorrelate(String messageName, String messageDataTemplateName, String messageDataKey, String messageDataValue, List<UUID> alreadyCorrelatedMessageIds);

    List<Message> findMessagesToCorrelate(String messageName, String messageDataTemplateName, String messageDataKey, String messageDataValue);

    Slice<UUID> findMessagesWithoutProcessCorrelation(ZonedDateTime createdBefore, Pageable pageable);

}
