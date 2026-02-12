package ch.admin.bit.jeap.processcontext.domain.processtemplate;

import static java.lang.String.format;

public class ProcessTemplateException extends RuntimeException {
    private ProcessTemplateException(String message) {
        super(message);
    }

    static ProcessTemplateException createEmptyProcessTemplate(String name) {
        return new ProcessTemplateException(format("Process template %s does not contain any tasks", name));
    }
}
