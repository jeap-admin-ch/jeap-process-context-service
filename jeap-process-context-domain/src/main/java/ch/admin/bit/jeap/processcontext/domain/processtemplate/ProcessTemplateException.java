package ch.admin.bit.jeap.processcontext.domain.processtemplate;

import java.util.function.Supplier;

import static java.lang.String.format;

public class ProcessTemplateException extends RuntimeException {
    private ProcessTemplateException(String message) {
        super(message);
    }

    static ProcessTemplateException createEmptyProcessTemplate(String name) {
        return new ProcessTemplateException(format("Process template %s does not contain any tasks", name));
    }

    public static Supplier<ProcessTemplateException> taskTypeNotFound(String taskTypeName, ProcessTemplate processTemplate) {
        return () -> new ProcessTemplateException(format("Task type %s does not exist in process template %s",
                taskTypeName,
                processTemplate));
    }
}
