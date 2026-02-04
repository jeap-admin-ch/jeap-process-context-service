package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskCardinality;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskType;

import java.util.Optional;

/**
 * Represents a task instance that is waiting to be completed by a specific message, either by message type
 * for single instance tasks, or by message type and originTaskId for multi instance tasks.
 */
record TaskWaitingToBeCompletedByMessage(TaskInstance taskInstance, String messageType, String originTaskId) {

    boolean isSingleInstance() {
        return originTaskId == null;
    }

    public static Optional<TaskWaitingToBeCompletedByMessage> of(TaskInstance taskInstance, TaskType type, String originTaskId) {
        String messageType = type.getCompletedByDomainEvent();

        // Not completed by message?
        if (messageType == null) {
            return Optional.empty();
        }

        // Completed by any domain event of the expected message type (single instance task),
        // or by an event with a specific originTaskId (multi instance task)?
        String matchingOriginTaskId = TaskCardinality.SINGLE_INSTANCE == type.getCardinality() ? null : originTaskId;

        return Optional.of(new TaskWaitingToBeCompletedByMessage(taskInstance, messageType, matchingOriginTaskId));
    }
}
