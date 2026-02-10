package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class TaskService {

    private final ProcessInstanceRepository processInstanceRepository;
    private final TaskInstanceRepository taskInstanceRepository;

    void completePlannedTasksByExistingMessages(Set<TaskWaitingToBeCompletedByMessage> plannedTasks) {
        plannedTasks.forEach(this::completedIfMessageExists);
    }

    private void completedIfMessageExists(TaskWaitingToBeCompletedByMessage task) {
        if (task.isSingleInstance()) {
            Optional<MessageReference> messageReference =
                    processInstanceRepository.findLatestMessageReferenceByMessageType(task.taskInstance().getProcessInstance(), task.messageType());
            messageReference.ifPresent(msgRef ->
                    task.taskInstance().completeByMessage(msgRef.getMessageId(), msgRef.getCreatedAt()));
        } else {
            Optional<MessageReference> messageReference =
                    processInstanceRepository.findLatestMessageReferenceByMessageTypeAndOriginTaskId(
                            task.taskInstance().getProcessInstance(),
                            task.messageType(),
                            task.originTaskId());
            messageReference.ifPresent(msgRef ->
                    task.taskInstance().completeByMessage(msgRef.getMessageId(), msgRef.getCreatedAt()));
        }

    }

    Optional<TaskInstance> registerNewTaskInUnknownState(ProcessInstance processInstance, TaskType taskType, ZonedDateTime timestamp) {
        TaskInstance taskInstance = TaskInstance.createUnknownTaskInstance(taskType, processInstance, timestamp);
        return saveNewTaskInstance(taskInstance);
    }

    Optional<TaskInstance> planDomainEventTask(ProcessInstance processInstance, TaskType taskType, String originTaskId, ZonedDateTime timestamp, UUID messageId) {
        TaskInstance plannedTask = TaskInstance.createTaskInstanceWithOriginTaskId(taskType, processInstance, originTaskId, timestamp, messageId);
        return saveNewTaskInstance(plannedTask);
    }

    void addObservationTask(ProcessInstance processInstance, TaskType taskType, String messageId, ZonedDateTime timestamp, UUID messageUuid) {
        TaskInstance observationTaskInstance = TaskInstance.createTaskInstanceWithOriginTaskIdAndState(taskType, processInstance, messageId, TaskState.COMPLETED, timestamp, messageUuid);
        saveNewTaskInstance(observationTaskInstance);
    }

    private Optional<TaskInstance> saveNewTaskInstance(TaskInstance taskInstance) {
        boolean exists = taskInstanceRepository.existsByProcessInstanceIdAndTaskTypeNameAndOriginTaskId(
                taskInstance.getProcessInstance().getId(),
                taskInstance.getTaskTypeName(),
                taskInstance.getOriginTaskId());
        if (exists) {
            return Optional.empty();
        }
        return Optional.of(taskInstanceRepository.save(taskInstance));
    }
}
