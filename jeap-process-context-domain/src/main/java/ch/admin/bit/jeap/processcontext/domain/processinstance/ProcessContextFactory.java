package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessState;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@UtilityClass
class ProcessContextFactory {

    ProcessContext createProcessContext(ProcessInstance processInstance) {
        return ProcessContext.builder()
                .originProcessId(processInstance.getOriginProcessId())
                .processName(processInstance.getProcessTemplateName())
                .processState(ProcessState.valueOf(processInstance.getState().name()))
                .messages(createMessages(processInstance))
                .build();
    }

    public static ch.admin.bit.jeap.processcontext.plugin.api.context.Message createMessage(MessageReferenceMessageDTO messageReferenceMessageDTO) {
        return ch.admin.bit.jeap.processcontext.plugin.api.context.Message.builder()
                .name(messageReferenceMessageDTO.getMessageName())
                .relatedOriginTaskIds(messageReferenceMessageDTO.getRelatedOriginTaskIds())
                .messageData(toMessageData(messageReferenceMessageDTO.getMessageData()))
                .build();
    }

    private List<ch.admin.bit.jeap.processcontext.plugin.api.context.Message> createMessages(ProcessInstance processInstance) {
        return processInstance.getMessageReferences().stream()
                .map(ProcessContextFactory::createMessage)
                .toList();
    }

    private Set<ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData> toMessageData(Set<MessageReferenceMessageDataDTO> messageReferenceMessageDataDTOS) {
        return messageReferenceMessageDataDTOS.stream()
                .map(data -> new ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData(data.getMessageDataKey(), data.getMessageDataValue(), data.getMessageDataRole()))
                .collect(Collectors.toSet());
    }

}
