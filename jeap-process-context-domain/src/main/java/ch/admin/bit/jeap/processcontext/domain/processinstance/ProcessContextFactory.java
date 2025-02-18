package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessCompletion;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessCompletionConclusion;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessState;
import ch.admin.bit.jeap.processcontext.plugin.api.context.TaskState;
import ch.admin.bit.jeap.processcontext.plugin.api.context.*;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@UtilityClass
class ProcessContextFactory {

    ProcessContext createProcessContext(ProcessInstance processInstance) {
        return ProcessContext.builder()
                .originProcessId(processInstance.getOriginProcessId())
                .processName(processInstance.getProcessTemplateName())
                .processState(ProcessState.valueOf(processInstance.getState().name()))
                .tasks(createTasks(processInstance.getTasks().stream().filter(t -> t.getTaskType().isPresent()).toList()))
                .messages(createMessages(processInstance))
                .processCompletion(createProcessCompletion(processInstance))
                .build();
    }

    public static ch.admin.bit.jeap.processcontext.plugin.api.context.Message createMessage(MessageReferenceMessageDTO messageReferenceMessageDTO) {
        return ch.admin.bit.jeap.processcontext.plugin.api.context.Message.builder()
                .name(messageReferenceMessageDTO.getMessageName())
                .relatedOriginTaskIds(messageReferenceMessageDTO.getRelatedOriginTaskIds())
                .messageData(toMessageData(messageReferenceMessageDTO.getMessageData()))
                .build();
    }

    private List<Task> createTasks(List<TaskInstance> tasks) {
        return tasks.stream()
                .map(ProcessContextFactory::createTask)
                .collect(toList());
    }

    private Task createTask(TaskInstance task) {
        TaskType taskType = TaskType.builder()
                .name(task.requireTaskType().getName())
                .lifecycle(TaskLifecycle.valueOf(task.requireTaskType().getLifecycle().name()))
                .cardinality(TaskCardinality.valueOf(task.requireTaskType().getCardinality().name()))
                .build();

        return Task.builder()
                .id(task.getId().toString())
                .originTaskId(task.getOriginTaskId())
                .type(taskType)
                .state(TaskState.valueOf(task.getState().name()))
                .build();
    }

    private List<ch.admin.bit.jeap.processcontext.plugin.api.context.Message> createMessages(ProcessInstance processInstance) {
        return processInstance.getMessageReferences().stream()
                .map(ProcessContextFactory::createMessage)
                .collect(toList());
    }

    private Set<ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData> toMessageData(Set<MessageReferenceMessageDataDTO> messageReferenceMessageDataDTOS) {
        return messageReferenceMessageDataDTOS.stream()
                .map(data -> new ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData(data.getMessageDataKey(), data.getMessageDataValue(), data.getMessageDataRole()))
                .collect(Collectors.toSet());
    }

    private ProcessCompletion createProcessCompletion(ProcessInstance processInstance) {
        return processInstance.getProcessCompletion().map( c -> ProcessCompletion.builder()
                        .conclusion(ProcessCompletionConclusion.valueOf(c.getConclusion().name()))
                        .completedAt(c.getCompletedAt())
                        .name(c.getName())
                        .build())
                .orElse(null);
    }

}
