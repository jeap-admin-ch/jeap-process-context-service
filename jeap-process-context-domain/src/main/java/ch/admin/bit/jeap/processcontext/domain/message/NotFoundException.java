package ch.admin.bit.jeap.processcontext.domain.message;

import java.util.function.Supplier;

import static java.lang.String.format;

public class NotFoundException extends RuntimeException {
    private NotFoundException(String message) {
        super(message);
    }

    static Supplier<NotFoundException> processNotFound(String originProcessId) {
        return () -> new NotFoundException(format("Process ID %s not found", originProcessId));
    }
}
