package ch.admin.bit.jeap.processcontext.adapter.restapi.model;

import ch.admin.bit.jeap.processcontext.domain.processinstance.MessageReferenceMessageDTO;
import ch.admin.bit.jeap.processcontext.domain.processinstance.MessageReferenceMessageDataDTO;
import ch.admin.bit.jeap.processcontext.plugin.api.message.MessageData;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Value
@AllArgsConstructor
@Builder(access = AccessLevel.PRIVATE)
public class MessageDTO {

    static final int MAX_MESSAGE_DATA_SIZE = 10;

    UUID id;
    String name;
    Set<String> relatedOriginTaskIds;
    String traceId;
    List<MessageData> messageData;
    boolean messageDataTruncated;
    ZonedDateTime receivedAt;
    ZonedDateTime createdAt;

    static MessageDTO create(MessageReferenceMessageDTO messageReferenceMessageDTO) {
        List<MessageData> allMessageData = toMessageData(messageReferenceMessageDTO.getMessageData());
        boolean truncated = allMessageData.size() > MAX_MESSAGE_DATA_SIZE;
        return MessageDTO.builder()
                .id(messageReferenceMessageDTO.getMessageId())
                .name(messageReferenceMessageDTO.getMessageName())
                .relatedOriginTaskIds(messageReferenceMessageDTO.getRelatedOriginTaskIds())
                .messageData(truncated ? allMessageData.subList(0, MAX_MESSAGE_DATA_SIZE) : allMessageData)
                .messageDataTruncated(truncated)
                .receivedAt(messageReferenceMessageDTO.getMessageReceivedAt())
                .createdAt(messageReferenceMessageDTO.getMessageCreatedAt())
                .traceId(messageReferenceMessageDTO.getTraceId())
                .build();
    }

    static MessageDTO createWithFullMessageData(MessageReferenceMessageDTO messageReferenceMessageDTO) {
        List<MessageData> allMessageData = toMessageData(messageReferenceMessageDTO.getMessageData());
        return MessageDTO.builder()
                .id(messageReferenceMessageDTO.getMessageId())
                .name(messageReferenceMessageDTO.getMessageName())
                .relatedOriginTaskIds(messageReferenceMessageDTO.getRelatedOriginTaskIds())
                .messageData(allMessageData)
                .messageDataTruncated(false)
                .receivedAt(messageReferenceMessageDTO.getMessageReceivedAt())
                .createdAt(messageReferenceMessageDTO.getMessageCreatedAt())
                .traceId(messageReferenceMessageDTO.getTraceId())
                .build();
    }

    private static List<MessageData> toMessageData(List<MessageReferenceMessageDataDTO> dtos) {
        return dtos.stream()
                .map(data -> new MessageData(data.getMessageDataKey(), data.getMessageDataValue(), data.getMessageDataRole()))
                .sorted(Comparator.comparing(MessageData::getKey))
                .toList();
    }
}
