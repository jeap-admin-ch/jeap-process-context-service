package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.adapter.restapi.ReevaluationJobController;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobRepository;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTaskDispatcher;
import ch.admin.bit.jeap.processcontext.domain.maintenance.ReevaluationJobService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        "jeap.processcontext.maintenance.enabled=true",
        "jeap.processcontext.template.classpath-location-pattern=" +
                "classpath:/process/templates/command_triggers_process_instance_instantiation.json"
})
class MaintenanceEnabledContextIT extends ProcessInstanceMockS3ITBase {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void maintenanceEnabled_registersCompleteRuntimeSlice() {
        assertThat(applicationContext.getBeansOfType(MaintenanceJobRepository.class)).hasSize(1);
        assertThat(applicationContext.getBean(ReevaluationJobService.class)).isNotNull();
        assertThat(applicationContext.getBean(ReevaluationJobController.class)).isNotNull();
        assertThat(applicationContext.getBean(MaintenanceTaskDispatcher.class)).isNotNull();
        assertThat(applicationContext.containsBean("maintenanceJobJpaRepository")).isTrue();
        assertThat(applicationContext.containsBean("maintenanceTaskJpaRepository")).isTrue();
        assertThat(applicationContext.containsBean("outboxMaintenanceEventPublisher")).isTrue();
        assertThat(applicationContext.containsBean("maintenanceProcessContextOutdatedEventHandler")).isTrue();
    }
}
