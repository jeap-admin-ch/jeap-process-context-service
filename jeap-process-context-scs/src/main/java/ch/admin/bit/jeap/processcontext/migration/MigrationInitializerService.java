package ch.admin.bit.jeap.processcontext.migration;

import ch.admin.bit.jeap.processcontext.domain.processinstance.migration.ProcessInstanceMigrationService;
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

    @Value("${jeap.processcontext.template.migration.max-modified-at-age-days:180}")
    int maxModifiedAtAgeDays;

    private final ProcessInstanceMigrationService processInstanceMigrationService;

    @PostConstruct
    public void triggerMigrationTasks() {
        log.info("Setting initial template hash value");
        processInstanceMigrationService.initializeProcessTemplateHashes();
        log.info("Triggering process instance migrations if templates have been modified");
        ZonedDateTime lastModifiedAfter = ZonedDateTime.now().minusDays(maxModifiedAtAgeDays);
        processInstanceMigrationService.triggerMigrationForModifiedTemplates(lastModifiedAfter);
    }
}
