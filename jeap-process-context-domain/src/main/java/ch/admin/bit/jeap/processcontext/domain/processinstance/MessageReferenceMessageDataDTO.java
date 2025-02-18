package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.message.MessageData;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class MessageReferenceMessageDataDTO {
    @NonNull
    String messageDataKey;
    @NonNull
    String messageDataValue;
    String messageDataRole;

    public static MessageReferenceMessageDataDTO from(MessageData messageData) {
        return MessageReferenceMessageDataDTO.builder()
                .messageDataKey(messageData.getKey())
                .messageDataValue(messageData.getValue())
                .messageDataRole(messageData.getRole())
                .build();
    }
}
