package ch.admin.bit.jeap.processcontext.adapter.restapi;

import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJob;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static ch.admin.bit.jeap.processcontext.adapter.restapi.MaintenanceTaskReport.value;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RelationPublicationJobReport(
        @JsonProperty("job-id") UUID jobId,
        @JsonProperty("job-type") String jobType,
        @JsonProperty("job-state") String jobState,
        @JsonProperty("job-result") String jobResult,
        Instant started,
        Instant completed,
        @JsonProperty("started-by-name") String startedByName,
        @JsonProperty("started-by-ext-id") String startedByExtId,
        List<RelationPublicationTaskReport> relations) {

    static RelationPublicationJobReport from(MaintenanceJob job) {
        return new RelationPublicationJobReport(
                job.jobId(),
                value(job.jobType()),
                value(job.jobState()),
                value(job.jobResult()),
                job.startedAt(),
                job.completedAt(),
                job.startedByName(),
                job.startedByExtId(),
                job.tasks().stream().map(RelationPublicationTaskReport::from).toList());
    }
}
