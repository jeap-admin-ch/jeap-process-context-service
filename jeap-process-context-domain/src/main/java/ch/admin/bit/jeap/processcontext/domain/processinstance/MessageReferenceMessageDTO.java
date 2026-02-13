package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.message.MessageData;
import ch.admin.bit.jeap.processcontext.domain.message.OriginTaskId;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.toSet;

@Value
@Builder(toBuilder = true)
public class MessageReferenceMessageDTO {

    @NonNull
    UUID messageReferenceId;
    @NonNull
    UUID messageId;
    @NonNull
    String messageName;
    @NonNull
    ZonedDateTime messageReceivedAt;
    @NonNull
    ZonedDateTime messageCreatedAt;
    @NonNull
    List<MessageReferenceMessageDataDTO> messageData;
    @NonNull
    Set<String> relatedOriginTaskIds;
    String traceId;

    public static MessageReferenceMessageDTO of(String processTemplateName, UUID messageReferenceId, Message message) {
        return MessageReferenceMessageDTO.builder()
                .messageReferenceId(messageReferenceId)
                .messageId(message.getId())
                .messageName(message.getMessageName())
                .messageReceivedAt(message.getReceivedAt())
                .messageCreatedAt(message.getMessageCreatedAt())
                .messageData(toDto(message.getMessageData(processTemplateName)))
                .relatedOriginTaskIds(toRelatedOriginTaskIds(message.getOriginTaskIds(processTemplateName)))
                .traceId(message.getTraceId())
                .build();
    }

    private static List<MessageReferenceMessageDataDTO> toDto(List<MessageData> messageData) {
        return messageData.stream().map(MessageReferenceMessageDataDTO::from).toList();
    }

    private static Set<String> toRelatedOriginTaskIds(Set<OriginTaskId> originTaskIds) {
        return originTaskIds.stream().map(OriginTaskId::getOriginTaskId).collect(toSet());
    }

}
