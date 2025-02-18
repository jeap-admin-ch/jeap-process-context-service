package ch.admin.bit.jeap.processcontext.adapter.jpa;

import java.util.function.Supplier;

import static java.lang.String.format;

class ProcessTemplateNotFoundException extends RuntimeException {
    private ProcessTemplateNotFoundException(String message) {
        super(message);
    }

    static Supplier<ProcessTemplateNotFoundException> templateNotFound(String originProcessId, String templateName) {
        return () -> new ProcessTemplateNotFoundException(format("Template %s not found for process with origin process ID %s",
                templateName,
                originProcessId));
    }
}
