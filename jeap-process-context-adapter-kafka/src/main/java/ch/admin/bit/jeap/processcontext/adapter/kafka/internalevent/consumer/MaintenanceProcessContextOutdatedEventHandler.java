package ch.admin.bit.jeap.processcontext.adapter.kafka.internalevent.consumer;

import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTargetNotFoundException;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTaskExecutionService;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTaskResultService;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceUpdateType;
import ch.admin.bit.jeap.processcontext.internal.event.outdated.ProcessContextOutdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "jeap.processcontext.maintenance", name = "enabled", havingValue = "true")
class MaintenanceProcessContextOutdatedEventHandler {
    private final MaintenanceTaskExecutionService executionService;
    private final MaintenanceTaskResultService resultService;

    void handle(ProcessContextOutdatedEvent event) {
        var maintenanceTask = event.getPayload().getMaintenanceJobTask();
        if (maintenanceTask == null) {
            throw new IllegalArgumentException("Maintenance event has no task envelope");
        }
        try {
            executionService.execute(maintenanceTask.getTaskId(),
                    MaintenanceUpdateType.valueOf(event.getPayload().getProcessUpdateType().name()));
        } catch (MaintenanceTargetNotFoundException e) {
            resultService.markNotFoundInNewTransaction(maintenanceTask.getTaskId());
            log.warn("Maintenance target not found for job '{}' task '{}'.",
                    maintenanceTask.getJobId(), maintenanceTask.getTaskId());
        } catch (RuntimeException e) {
            resultService.markFailedInNewTransaction(maintenanceTask.getTaskId(), e);
            log.error("Maintenance processing failed for job '{}' task '{}' with exception type '{}'.",
                    maintenanceTask.getJobId(), maintenanceTask.getTaskId(), e.getClass().getSimpleName());
        }
    }
}
