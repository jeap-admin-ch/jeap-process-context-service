package ch.admin.bit.jeap.processcontext.migration;

import ch.admin.bit.jeap.processcontext.domain.processinstance.migration.ProcessInstanceMigrationTriggerService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
class MigrationInitializerService {

    @Value("${jeap.processcontext.template.migration.max-created-at-age-days:180}")
    int maxCreatedAtAgeDays;

    private final ProcessInstanceMigrationTriggerService processInstanceMigrationTriggerService;

    @PostConstruct
    public void triggerMigrationTasks() {
        log.info("Triggering process instance migrations if templates have been modified");
        ZonedDateTime createdAtAfter = ZonedDateTime.now().minusDays(maxCreatedAtAgeDays);
        processInstanceMigrationTriggerService.triggerMigrationForModifiedTemplates(createdAtAfter);
    }
}
