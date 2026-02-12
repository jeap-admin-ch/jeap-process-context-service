package ch.admin.bit.jeap.processcontext.domain.processinstance;

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

    static Supplier<NotFoundException> templateNotFound(String processTemplateName, String originProcessId) {
        return () -> new NotFoundException(format(
                "Process template %s not found while instantiating process %s",
                processTemplateName,
                originProcessId), false);
    }

    static Supplier<NotFoundException> messageNotFound(UUID id, String originProcessId) {
        return () -> new NotFoundException(format(
                "The message with ID %s correlated with process %s could not be found.",
                id,
                originProcessId), true);
    }
}
