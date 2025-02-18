package ch.admin.bit.jeap.processcontext.migration;

import ch.admin.bit.jeap.processcontext.domain.processinstance.migration.ProcessInstanceMigrationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MigrationInitializerServiceTest {

    @Mock
    private ProcessInstanceMigrationService processInstanceMigrationService;

    @Captor
    ArgumentCaptor<ZonedDateTime> lastModifiedAfter;

    @Test
    void triggerMigrationTasks() {
        MigrationInitializerService migrationInitializerService = new MigrationInitializerService(processInstanceMigrationService);
        migrationInitializerService.maxModifiedAtAgeDays = 180;
        migrationInitializerService.triggerMigrationTasks();

        verify(processInstanceMigrationService)
                .triggerMigrationForModifiedTemplates(lastModifiedAfter.capture());

        ZonedDateTime now = ZonedDateTime.now();
        assertTrue(lastModifiedAfter.getValue().isBefore(now.minusDays(180 - 1)));
        assertTrue(lastModifiedAfter.getValue().isAfter(now.minusDays(180 + 1)));
    }
}
