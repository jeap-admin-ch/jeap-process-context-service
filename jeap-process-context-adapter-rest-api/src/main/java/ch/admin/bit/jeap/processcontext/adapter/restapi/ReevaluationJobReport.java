package ch.admin.bit.jeap.processcontext.adapter.restapi;

import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJob;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTask;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReevaluationJobReport(
        @JsonProperty("job-id") UUID jobId,
        @JsonProperty("job-type") String jobType,
        @JsonProperty("process-template-name") String processTemplateName,
        @JsonProperty("job-state") String jobState,
        @JsonProperty("job-result") String jobResult,
        Instant started,
        Instant completed,
        @JsonProperty("started-by-name") String startedByName,
        @JsonProperty("started-by-ext-id") String startedByExtId,
        List<ProcessReport> processes) {

    static ReevaluationJobReport from(MaintenanceJob job) {
        return new ReevaluationJobReport(
                job.jobId(),
                value(job.jobType()),
                job.processTemplateName(),
                value(job.jobState()),
                value(job.jobResult()),
                job.startedAt(),
                job.completedAt(),
                job.startedByName(),
                job.startedByExtId(),
                job.tasks().stream().map(ProcessReport::from).toList());
    }

    private static String value(Enum<?> value) {
        return value == null ? null : value.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ProcessReport(
            @JsonProperty("task-id") UUID taskId,
            @JsonProperty("origin-process-id") String originProcessId,
            String state,
            ErrorReport error) {

        static ProcessReport from(MaintenanceTask task) {
            ErrorReport error = task.errorMessage() == null && task.errorTraceId() == null
                    ? null
                    : new ErrorReport(task.errorMessage(), task.errorTraceId());
            return new ProcessReport(task.taskId(), task.originProcessId(), value(task.taskState()), error);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ErrorReport(String message, @JsonProperty("trace-id") String traceId) {
    }
}
