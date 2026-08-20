package ch.admin.bit.jeap.processcontext.domain.maintenance;

import ch.admin.bit.jeap.processcontext.domain.tx.Transactions;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jeap.processcontext.maintenance", name = "enabled", havingValue = "true")
public class MaintenanceTaskDispatcher {
    private final MaintenanceJobRepository repository;
    private final MaintenanceEventPublisher eventPublisher;
    private final Transactions transactions;
    private final MaintenanceProperties properties;

    @Scheduled(fixedDelayString = "${jeap.processcontext.maintenance.dispatcher.fixed-delay-ms:1000}")
    public void dispatchCreatedTasks() {
        repository.findTaskIdsByState(MaintenanceTaskState.CREATED, properties.getDispatcher().getBatchSize())
                .forEach(this::dispatch);
    }

    private void dispatch(UUID taskId) {
        transactions.withinNewTransaction(() -> repository.findByTaskIdForUpdate(taskId).ifPresent(job -> {
            MaintenanceTask task = job.task(taskId);
            if (task.taskState() != MaintenanceTaskState.CREATED) {
                return;
            }
            eventPublisher.publish(job, task);
            repository.update(job.transitionTask(taskId, MaintenanceTaskState.EVENT_QUEUED, null, Instant.now()));
        }));
    }
}
