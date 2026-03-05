package ch.admin.bit.jeap.processcontext.migration;

import ch.admin.bit.jeap.processcontext.domain.processinstance.migration.ProcessInstanceMigrationTriggerService;
import net.javacrumbs.shedlock.core.LockAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MigrationSchedulerTest {

    @Mock
    private ProcessInstanceMigrationTriggerService triggerService;

    @Captor
    private ArgumentCaptor<ZonedDateTime> createdAtAfterCaptor;

    @Captor
    private ArgumentCaptor<Integer> batchSizeCaptor;

    @BeforeEach
    void setUp() {
        LockAssert.TestHelper.makeAllAssertsPass(true);
    }

    @Test
    void execute_callsTriggerServiceWithCorrectParameters() {
        MigrationSchedulerConfigProperties config = new MigrationSchedulerConfigProperties();
        config.setMaxCreatedAtAgeDays(180);
        config.setBatchSize(10000);
        MigrationScheduler scheduler = new MigrationScheduler(triggerService, config);

        when(triggerService.triggerMigrationForModifiedTemplates(any(), anyInt())).thenReturn(0);

        scheduler.execute();

        verify(triggerService).triggerMigrationForModifiedTemplates(createdAtAfterCaptor.capture(), batchSizeCaptor.capture());

        ZonedDateTime now = ZonedDateTime.now();
        assertThat(createdAtAfterCaptor.getValue()).isBefore(now.minusDays(179));
        assertThat(createdAtAfterCaptor.getValue()).isAfter(now.minusDays(181));
        assertThat(batchSizeCaptor.getValue()).isEqualTo(10000);
    }
}
