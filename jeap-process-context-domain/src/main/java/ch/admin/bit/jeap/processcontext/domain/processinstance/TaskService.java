package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskCardinality;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskLifecycle;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskType;
import ch.admin.bit.jeap.processcontext.plugin.api.condition.TaskInstantiationCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Stream;

import static ch.admin.bit.jeap.processcontext.domain.processinstance.api.MessageFactory.createMessage;
import static java.util.stream.Collectors.toSet;

@Component
@RequiredArgsConstructor
class TaskService {

    private final ProcessInstanceRepository processInstanceRepository;
    private final TaskInstanceRepository taskInstanceRepository;

    List<TaskInstance> planDomainEventTasks(ProcessInstance processInstance, MessageReferenceMessageDTO messageReferenceMessageDTO, Message message) {
        List<TaskInstance> taskInstances = processInstance.getProcessTemplate().getTaskTypes().stream()
                .filter(taskType -> message.getMessageName().equals(taskType.getPlannedByDomainEvent()))
                .flatMap(taskType -> planDomainEventTasks(processInstance, messageReferenceMessageDTO, message, taskType))
                .filter(Objects::nonNull)
                .toList();
        taskInstanceRepository.flush();
        return taskInstances;
    }

    private Stream<TaskInstance> planDomainEventTasks(ProcessInstance processInstance, MessageReferenceMessageDTO messageReferenceMessageDTO, Message message, TaskType taskType) {
        TaskInstantiationCondition instantiationCondition = taskType.getInstantiationCondition();
        if (instantiationCondition == null || instantiationCondition.instantiate(createMessage(messageReferenceMessageDTO))) {
            if (taskType.getCardinality() == TaskCardinality.SINGLE_INSTANCE) {
                return planSingleInstanceDomainEventTaskIfNotExists(processInstance, messageReferenceMessageDTO, taskType, message.getMessageId(), message.getMessageCreatedAt(), message.getId())
                        .stream();
            } else {
                return messageReferenceMessageDTO.getRelatedOriginTaskIds().stream()
                        .flatMap(originTaskId -> planDomainEventTask(processInstance, taskType, originTaskId, message.getMessageCreatedAt(), message.getId()).stream());
            }
        }
        return Stream.empty();
    }

    private Optional<TaskInstance> planSingleInstanceDomainEventTaskIfNotExists(ProcessInstance processInstance, MessageReferenceMessageDTO eventReference, TaskType taskType, String eventId, ZonedDateTime timestamp, UUID messageId) {
        Set<String> relatedOriginTaskIds = eventReference.getRelatedOriginTaskIds();
        if (relatedOriginTaskIds.size() > 1) {
            throw TaskPlanningException.singleInstanceTaskMultipleIds(taskType, relatedOriginTaskIds);
        }
        String taskId = relatedOriginTaskIds.isEmpty() ? eventId : relatedOriginTaskIds.iterator().next();
        return planDomainEventTask(processInstance, taskType, taskId, timestamp, messageId);
    }

    void completeObservationTasks(ProcessInstance processInstance, MessageReferenceMessageDTO messageReferenceMessageDTO, Message message) {
        processInstance.getProcessTemplate().getTaskTypes().stream()
                .filter(taskType -> matchingObservationTask(message, taskType))
                .forEach(taskType -> instantiateObservationTask(processInstance, messageReferenceMessageDTO, message, taskType));
        taskInstanceRepository.flush();
    }

    private void instantiateObservationTask(ProcessInstance processInstance, MessageReferenceMessageDTO messageReferenceMessageDTO, Message message, TaskType taskType) {
        TaskInstantiationCondition instantiationCondition = taskType.getInstantiationCondition();
        if (instantiationCondition == null || instantiationCondition.instantiate(createMessage(messageReferenceMessageDTO))) {
            addObservationTask(processInstance, taskType, message.getMessageId(), message.getMessageCreatedAt(), message.getId());
        }
    }

    private static boolean matchingObservationTask(Message message, TaskType taskType) {
        return (TaskLifecycle.OBSERVED == taskType.getLifecycle()) && (message.getMessageName().equals(taskType.getObservesDomainEvent()));
    }

    void evaluateCompletedTasks(ProcessInstance processInstance, MessageReferenceMessageDTO messageReference) {
        // Make sure planned tasks are visible to the repository before querying for tasks to complete
        processInstanceRepository.flush();

        Set<String> taskTypesCompletedByDomainEvent = processInstance.getProcessTemplate().getTaskTypes().stream()
                .filter(type -> messageReference.getMessageName().equals(type.getCompletedByDomainEvent()))
                .map(TaskType::getName)
                .collect(toSet());
        if (taskTypesCompletedByDomainEvent.isEmpty()) {
            return;
        }

        List<TaskInstance> openTaskInstances = taskInstanceRepository.getTaskInstancesInNonFinalStateOfTypes(
                processInstance.getProcessTemplate(), processInstance.getId(), taskTypesCompletedByDomainEvent);
        openTaskInstances.forEach(taskInstance -> taskInstance.evaluateIfCompleted(messageReference));

        // Flush latest task state changes to tbe database
        taskInstanceRepository.flush();
    }

    void evaluatePlannedTasksCompletedByExistingMessages(List<TaskInstance> plannedDomainEventTasks) {
        if (plannedDomainEventTasks.isEmpty()) {
            return;
        }

        // Check if any the new planned tasks are waiting for completion by a domain event that might have been received before
        Set<TaskWaitingToBeCompletedByMessage> tasksWaitingForCompletionByDomainEvent = new HashSet<>();
        for (TaskInstance plannedTaskInstance : plannedDomainEventTasks) {
            Optional<TaskWaitingToBeCompletedByMessage> waitingTasks = plannedTaskInstance.waitingToBeCompletedByDomainEvent();
            waitingTasks.ifPresent(tasksWaitingForCompletionByDomainEvent::add);
        }

        // Check if any of these tasks can be completed now because the event has been received before
        completePlannedTasksByExistingMessages(tasksWaitingForCompletionByDomainEvent);
        taskInstanceRepository.flush();
    }

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
