package ch.admin.bit.jeap.processcontext.domain.processinstance;

import static java.lang.String.format;

public class TaskTypeException extends RuntimeException {

    private TaskTypeException(String message) {
        super(message);
    }

    static TaskTypeException createTaskTypeDeleted(String taskTypeName) {
        return new TaskTypeException(format("Task type %s doesn't exist anymore in template", taskTypeName));
    }

}
