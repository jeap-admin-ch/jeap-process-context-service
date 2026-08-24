package ch.admin.bit.jeap.processcontext.adapter.restapi;

import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTask;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Locale;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MaintenanceTaskReport(
        @JsonProperty("task-id") UUID taskId,
        @JsonProperty("origin-process-id") String originProcessId,
        String state,
        ErrorReport error) {

    static MaintenanceTaskReport from(MaintenanceTask task) {
        ErrorReport error = task.errorMessage() == null && task.errorTraceId() == null
                ? null
                : new ErrorReport(task.errorMessage(), task.errorTraceId());
        return new MaintenanceTaskReport(task.taskId(), task.originProcessId(), value(task.taskState()), error);
    }

    static String value(Enum<?> value) {
        return value == null ? null : value.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorReport(String message, @JsonProperty("trace-id") String traceId) {
    }
}
