package ch.admin.bit.jeap.processcontext.migration;

import ch.admin.bit.jeap.processcontext.domain.processinstance.migration.ProcessInstanceMigrationTriggerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
class MigrationScheduler {

    private final ProcessInstanceMigrationTriggerService triggerService;
    private final MigrationSchedulerConfigProperties configProperties;

    @Scheduled(cron = "#{@migrationSchedulerConfigProperties.cronExpression}")
    @SchedulerLock(name = "MigrationScheduler_execute", lockAtLeastFor = "#{@migrationSchedulerConfigProperties.lockAtLeast.toString()}", lockAtMostFor = "#{@migrationSchedulerConfigProperties.lockAtMost.toString()}")
    public void execute() {
        LockAssert.assertLocked();
        ZonedDateTime createdAtAfter = ZonedDateTime.now().minusDays(configProperties.getMaxCreatedAtAgeDays());
        log.debug("Migration scheduler started with batchSize={}, maxCreatedAtAgeDays={}", configProperties.getBatchSize(), configProperties.getMaxCreatedAtAgeDays());
        int totalMigrated = triggerService.triggerMigrationForModifiedTemplates(createdAtAfter, configProperties.getBatchSize());
        log.debug("Migration scheduler finished, triggered {} migration events", totalMigrated);
    }
}
