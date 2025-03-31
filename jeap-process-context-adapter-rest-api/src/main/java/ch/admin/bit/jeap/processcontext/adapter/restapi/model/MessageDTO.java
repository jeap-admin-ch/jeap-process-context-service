package ch.admin.bit.jeap.processcontext.adapter.restapi.model;

import ch.admin.bit.jeap.processcontext.domain.processinstance.MessageReferenceMessageDTO;
import ch.admin.bit.jeap.processcontext.domain.processinstance.MessageReferenceMessageDataDTO;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.toSet;

@Value
@AllArgsConstructor
@Builder(access = AccessLevel.PRIVATE)
public class MessageDTO {

    UUID id;
    String name;
    Set<String> relatedOriginTaskIds;
    String traceId;
    Set<ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData> messageData;
    ZonedDateTime receivedAt;
    ZonedDateTime createdAt;

    static MessageDTO create(MessageReferenceMessageDTO messageReferenceMessageDTO) {
        return MessageDTO.builder()
                .id(messageReferenceMessageDTO.getMessageId())
                .name(messageReferenceMessageDTO.getMessageName())
                .relatedOriginTaskIds(messageReferenceMessageDTO.getRelatedOriginTaskIds())
                .messageData(toMessageData(messageReferenceMessageDTO.getMessageData()))
                .receivedAt(messageReferenceMessageDTO.getMessageReceivedAt())
                .createdAt(messageReferenceMessageDTO.getMessageCreatedAt())
                .traceId(messageReferenceMessageDTO.getTraceId())
                .build();
    }

    private static Set<ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData> toMessageData(Set<MessageReferenceMessageDataDTO> dtos) {
        return dtos.stream()
                .map(data -> new ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData(data.getMessageDataKey(), data.getMessageDataValue(), data.getMessageDataRole()))
                .collect(toSet());
    }
}
