package ch.admin.bit.jeap.processcontext.domain.processinstance;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;

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
    Set<MessageReferenceMessageDataDTO> messageData;
    @NonNull
    Set<String> relatedOriginTaskIds;
    String traceId;

}
