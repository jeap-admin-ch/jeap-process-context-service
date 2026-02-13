package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.message.MessageReferenceRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.MessageReference;
import ch.admin.bit.jeap.processcontext.domain.processinstance.MessageReferenceMessageDTO;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Timed(value = "jeap.pcs.repository.messagereference", percentiles = .95)
public class MessageReferenceRepositoryImpl implements MessageReferenceRepository {

    private final MessageReferenceJpaRepository messageReferenceJpaRepository;

    @Override
    public MessageReference save(MessageReference messageReference) {
        return messageReferenceJpaRepository.save(messageReference);
    }

    @Override
    public boolean existsByProcessInstanceIdAndMessageId(UUID processInstanceId, UUID messageId) {
        return messageReferenceJpaRepository.existsByProcessInstanceIdAndMessageId(processInstanceId, messageId);
    }

    @Override
    public MessageReferenceMessageDTO findByProcessInstanceIdAndMessageId(UUID processInstanceId, UUID messageId) {
        return messageReferenceJpaRepository.findMessageReferenceWithMessageByProcessInstanceIdAndMessageId(processInstanceId, messageId)
                .map(this::toDto)
                .orElse(null);
    }

    @Override
    public List<MessageReferenceMessageDTO> findByProcessInstanceId(UUID processInstanceId) {
        return messageReferenceJpaRepository.findMessageReferencesWithMessagesByProcessInstanceId(processInstanceId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public Page<MessageReferenceMessageDTO> findByProcessInstanceId(UUID processInstanceId, Pageable pageable) {
        return messageReferenceJpaRepository.findMessageReferencesWithMessagesByProcessInstanceId(processInstanceId, pageable)
                .map(this::toDto);
    }

    @Override
    public Map<UUID, ZonedDateTime> findLastMessageCreatedAtByProcessInstanceIds(Collection<UUID> processInstanceIds) {
        if (processInstanceIds.isEmpty()) {
            return Map.of();
        }
        return messageReferenceJpaRepository.findLastMessageCreatedAtByProcessInstanceIds(processInstanceIds)
                .stream()
                .collect(Collectors.toMap(
                        ProcessInstanceLastMessageProjection::getProcessInstanceId,
                        ProcessInstanceLastMessageProjection::getLastMessageCreatedAt));
    }

    private MessageReferenceMessageDTO toDto(MessageReferenceWithMessage result) {
        return MessageReferenceMessageDTO.of(
                result.processTemplateName(),
                result.messageReference().getId(),
                result.message());
    }
}
