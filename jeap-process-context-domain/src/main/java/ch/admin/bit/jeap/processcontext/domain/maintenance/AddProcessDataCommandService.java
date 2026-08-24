package ch.admin.bit.jeap.processcontext.domain.maintenance;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessDataService;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceRepository;
import ch.admin.bit.jeap.processcontext.domain.tx.Transactions;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jeap.processcontext.maintenance", name = "enabled", havingValue = "true")
public class AddProcessDataCommandService {
    private final MaintenanceJobRepository repository;
    private final ProcessInstanceRepository processInstanceRepository;
    private final ProcessDataService processDataService;
    private final MaintenanceEventPublisher eventPublisher;
    private final Transactions transactions;

    public void handle(MaintenanceCommand command) {
        transactions.withinNewTransaction(() -> handleInTransaction(command));
    }

    private void handleInTransaction(MaintenanceCommand command) {
        MaintenanceJob job = repository.findByTaskIdForUpdate(command.taskId())
                .orElseThrow(MaintenanceTaskNotFoundException::new);
        MaintenanceTask task = job.task(command.taskId());
        validate(command, job, task);

        if (task.taskState() == MaintenanceTaskState.EVENT_QUEUED || task.taskState().isTerminal()) {
            return;
        }
        if (task.taskState() != MaintenanceTaskState.COMMAND_QUEUED) {
            throw new IllegalStateException("Maintenance task must be in COMMAND_QUEUED state");
        }

        ProcessInstance processInstance = processInstanceRepository.findByOriginProcessId(task.originProcessId())
                .orElseThrow(MaintenanceTargetNotFoundException::new);
        if (!job.processTemplateName().equals(processInstance.getProcessTemplate().getName())) {
            throw new IllegalStateException("Process template '%s' does not match backfill job template '%s'"
                    .formatted(processInstance.getProcessTemplate().getName(), job.processTemplateName()));
        }
        processDataService.addProcessData(processInstance, task.processData());
        eventPublisher.publish(job, task);
        repository.updateTask(job,
                task.transitionTo(MaintenanceTaskState.EVENT_QUEUED, null, null, Instant.now()));
    }

    private static void validate(MaintenanceCommand command, MaintenanceJob job, MaintenanceTask task) {
        if (job.jobType() != MaintenanceJobType.PROCESS_DATA_BACKFILL) {
            throw new MaintenanceCommandRejectedException(
                    "Maintenance command requires a PROCESS_DATA_BACKFILL job");
        }
        requireEqual("jobId", job.jobId(), command.jobId());
        requireEqual("processTemplateName", job.processTemplateName(), command.processTemplateName());
        requireEqual("processData", task.processData(), command.processData());
        requireEqual("processId", task.originProcessId(), command.processId());
        requireEqual("idempotenceId", MaintenanceCommand.idempotenceId(job.jobId(), task.taskId()),
                command.idempotenceId());
    }

    private static void requireEqual(String field, Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new MaintenanceCommandRejectedException(
                    "Maintenance command " + field + " does not match the durable task");
        }
    }
}
