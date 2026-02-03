package ch.admin.bit.jeap.processcontext.domain.processinstance.api;

import ch.admin.bit.jeap.processcontext.domain.processinstance.MessageReferenceMessageDTO;
import ch.admin.bit.jeap.processcontext.domain.processinstance.MessageReferenceMessageDataDTO;
import ch.admin.bit.jeap.processcontext.plugin.api.context.Message;
import ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData;
import lombok.experimental.UtilityClass;

import java.util.Set;
import java.util.stream.Collectors;

@UtilityClass
public class MessageFactory {

    public static Message createMessage(MessageReferenceMessageDTO messageReferenceMessageDTO) {
        return Message.builder()
                .name(messageReferenceMessageDTO.getMessageName())
                .relatedOriginTaskIds(messageReferenceMessageDTO.getRelatedOriginTaskIds())
                .messageData(toMessageData(messageReferenceMessageDTO.getMessageData()))
                .build();
    }

    private static Set<MessageData> toMessageData(Set<MessageReferenceMessageDataDTO> messageReferenceMessageDataDTOS) {
        return messageReferenceMessageDataDTOS.stream()
                .map(data -> new MessageData(data.getMessageDataKey(), data.getMessageDataValue(), data.getMessageDataRole()))
                .collect(Collectors.toSet());
    }
}
