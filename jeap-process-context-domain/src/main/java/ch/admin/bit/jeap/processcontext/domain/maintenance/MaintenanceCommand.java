package ch.admin.bit.jeap.processcontext.domain.maintenance;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessDataValue;

import java.util.List;
import java.util.UUID;

public record MaintenanceCommand(
        UUID jobId,
        UUID taskId,
        String processTemplateName,
        List<ProcessDataValue> processData,
        String processId,
        String idempotenceId) {

    public MaintenanceCommand {
        processData = ProcessDataValue.canonicalize(processData);
    }

    public static MaintenanceCommand from(MaintenanceJob job, MaintenanceTask task) {
        return new MaintenanceCommand(job.jobId(), task.taskId(), job.processTemplateName(), task.processData(),
                task.originProcessId(), idempotenceId(job.jobId(), task.taskId()));
    }

    public static String idempotenceId(UUID jobId, UUID taskId) {
        return jobId + "-" + taskId;
    }
}
