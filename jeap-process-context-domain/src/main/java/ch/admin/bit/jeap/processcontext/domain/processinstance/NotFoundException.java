package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskType;
import lombok.Getter;

import java.util.UUID;
import java.util.function.Supplier;

import static java.lang.String.format;

public class NotFoundException extends RuntimeException {
    @Getter
    private final boolean retryable;

    private NotFoundException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public static Supplier<NotFoundException> processNotFound(String originProcessId) {
        return () -> new NotFoundException(format("Process ID %s not found", originProcessId), true);
    }

    static NotFoundException createTaskTypeNotFound(TaskType taskType, String originProcessId) {
        return new NotFoundException(format("Task of type %s not found in process %s",
                taskType,
                originProcessId), false);
    }

    static Supplier<NotFoundException> templateNotFound(String processTemplateName, String originProcessId) {
        return () -> new NotFoundException(format(
                "Process template %s not found while instantiating process %s",
                processTemplateName,
                originProcessId), false);
    }

    static Supplier<NotFoundException> milestoneNotFound(String milestoneName, String processTemplateName, String originProcessId) {
        return () -> new NotFoundException(format(
                "Milestone %s not found in process template %s while loading process %s",
                milestoneName,
                processTemplateName,
                originProcessId), false);
    }

    static Supplier<NotFoundException> taskNotFoundInProcessContext(UUID id, String originProcessId) {
        return () -> new NotFoundException(format(
                "Task with ID %s not found in process context for process %s",
                id,
                originProcessId), true);
    }

    static Supplier<NotFoundException> messageNotFound(UUID id, String originProcessId) {
        return () -> new NotFoundException(format(
                "The message with ID %s correlated with process %s could not be found.",
                id,
                originProcessId), true);
    }
}
