package ch.admin.bit.jeap.processcontext.adapter.restapi;

import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTask;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RelationPublicationTaskReport(
        @JsonProperty("task-id") UUID taskId,
        @JsonProperty("relation-id") UUID relationId,
        String state,
        MaintenanceTaskReport.ErrorReport error) {

    static RelationPublicationTaskReport from(MaintenanceTask task) {
        return new RelationPublicationTaskReport(
                task.taskId(),
                task.relationId(),
                MaintenanceTaskReport.value(task.taskState()),
                MaintenanceTaskReport.error(task));
    }
}
