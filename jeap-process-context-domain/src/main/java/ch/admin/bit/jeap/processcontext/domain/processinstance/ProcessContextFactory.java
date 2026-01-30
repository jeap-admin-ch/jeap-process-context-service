package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextImpl;
import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextRepositoryFacade;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProcessContextFactory {

    private final ProcessContextRepositoryFacade repositoryFacade;

    ProcessContext createProcessContext(ProcessInstance processInstance) {
        return ProcessContextImpl.builder()
                .processInstanceId(processInstance.getId())
                .originProcessId(processInstance.getOriginProcessId())
                .processName(processInstance.getProcessTemplateName())
                .processState(ProcessState.valueOf(processInstance.getState().name()))
                .messages(createMessages(processInstance))
                .repositoryFacade(repositoryFacade)
                .build();
    }

    public static ch.admin.bit.jeap.processcontext.plugin.api.context.Message createMessage(MessageReferenceMessageDTO messageReferenceMessageDTO) {
        return ch.admin.bit.jeap.processcontext.plugin.api.context.Message.builder()
                .name(messageReferenceMessageDTO.getMessageName())
                .relatedOriginTaskIds(messageReferenceMessageDTO.getRelatedOriginTaskIds())
                .messageData(toMessageData(messageReferenceMessageDTO.getMessageData()))
                .build();
    }

    private static List<ch.admin.bit.jeap.processcontext.plugin.api.context.Message> createMessages(ProcessInstance processInstance) {
        return processInstance.getMessageReferences().stream()
                .map(ProcessContextFactory::createMessage)
                .toList();
    }

    private static Set<ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData> toMessageData(Set<MessageReferenceMessageDataDTO> messageReferenceMessageDataDTOS) {
        return messageReferenceMessageDataDTOS.stream()
                .map(data -> new ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData(data.getMessageDataKey(), data.getMessageDataValue(), data.getMessageDataRole()))
                .collect(Collectors.toSet());
    }

}
