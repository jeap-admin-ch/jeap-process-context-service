package ch.admin.bit.jeap.processcontext.domain.processinstance;

import java.util.UUID;
import java.util.function.Supplier;

import static java.lang.String.format;

public class NotFoundException extends RuntimeException {

    private NotFoundException(String message) {
        super(message);
    }

    static Supplier<NotFoundException> templateNotFound(String processTemplateName, String originProcessId) {
        return () -> new NotFoundException(format(
                "Process template %s not found while instantiating process %s",
                processTemplateName,
                originProcessId));
    }

    static Supplier<NotFoundException> messageNotFound(UUID id, String originProcessId) {
        return () -> new NotFoundException(format(
                "The message with ID %s correlated with process %s could not be found.",
                id,
                originProcessId));
    }
}
