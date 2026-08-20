package ch.admin.bit.jeap.processcontext.domain.maintenance;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.relation.RelationService;
import ch.admin.bit.jeap.processcontext.domain.tx.Transactions;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jeap.processcontext.maintenance", name = "enabled", havingValue = "true")
public class MaintenanceTaskExecutionService {
    private final MaintenanceJobRepository repository;
    private final ProcessInstanceRepository processInstanceRepository;
    private final RelationService relationService;
    private final Transactions transactions;

    public void execute(UUID taskId, MaintenanceUpdateType updateType) {
        transactions.withinNewTransaction(() -> executeInTransaction(taskId, updateType));
    }

    private void executeInTransaction(UUID taskId, MaintenanceUpdateType updateType) {
        MaintenanceJob job = repository.findByTaskIdForUpdate(taskId)
                .orElseThrow(MaintenanceTaskNotFoundException::new);
        MaintenanceTask task = job.task(taskId);
        if (task.taskState().isTerminal()) {
            return;
        }
        if (task.taskState() != MaintenanceTaskState.EVENT_QUEUED || updateType != MaintenanceUpdateType.REEVALUATE_JOB) {
            throw new IllegalStateException("Maintenance task cannot be processed");
        }

        repository.update(job.transitionTask(taskId, MaintenanceTaskState.PROCESSING, null, Instant.now()));
        ProcessInstance processInstance = processInstanceRepository.findByOriginProcessId(task.originProcessId())
                .filter(instance -> job.processTemplateName().equals(instance.getProcessTemplate().getName()))
                .orElseThrow(MaintenanceTargetNotFoundException::new);
        relationService.reevaluateRelations(processInstance);
        repository.update(job.transitionTask(taskId, MaintenanceTaskState.SUCCEEDED, null, Instant.now()));
    }
}
