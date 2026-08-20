package ch.admin.bit.jeap.processcontext.domain.maintenance;

import ch.admin.bit.jeap.processcontext.domain.tx.Transactions;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jeap.processcontext.maintenance", name = "enabled", havingValue = "true")
public class MaintenanceTaskResultService {
    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;
    private static final Pattern SENSITIVE_MESSAGE = Pattern.compile(
            "(?i).*(authorization|credential|password|secret|token).*", Pattern.DOTALL);

    private final MaintenanceJobRepository repository;
    private final Transactions transactions;

    public void markNotFoundInNewTransaction(UUID taskId) {
        transitionInNewTransaction(taskId, MaintenanceTaskState.NOT_FOUND, "Maintenance target not found");
    }

    public void markFailedInNewTransaction(UUID taskId, RuntimeException exception) {
        transitionInNewTransaction(taskId, MaintenanceTaskState.FAILED, sanitizedError(exception));
    }

    private void transitionInNewTransaction(UUID taskId, MaintenanceTaskState state, String errorMessage) {
        transactions.withinNewTransaction(() -> {
            MaintenanceJob job = repository.findByTaskIdForUpdate(taskId)
                    .orElseThrow(MaintenanceTaskNotFoundException::new);
            if (!job.task(taskId).taskState().isTerminal()) {
                repository.update(job.transitionTask(taskId, state, errorMessage, MDC.get("traceId"), Instant.now()));
            }
        });
    }

    private static String sanitizedError(RuntimeException exception) {
        String detail = exception.getMessage();
        if (detail == null || detail.isBlank() || SENSITIVE_MESSAGE.matcher(detail).matches()) {
            detail = "Task processing failed";
        } else {
            detail = detail.replaceAll("[\\r\\n\\t]+", " ").replaceAll(" +", " ").trim();
        }
        String message = exception.getClass().getSimpleName() + ": " + detail;
        return message.length() <= MAX_ERROR_MESSAGE_LENGTH
                ? message
                : message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }
}
