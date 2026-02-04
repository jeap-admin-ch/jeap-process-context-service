package ch.admin.bit.jeap.processcontext.domain.processinstance;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
class TaskService {

    private final ProcessInstanceRepository processInstanceRepository;

    public void completePlannedTasksByExistingMessages(Set<TaskWaitingToBeCompletedByMessage> plannedTasks) {
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
}
