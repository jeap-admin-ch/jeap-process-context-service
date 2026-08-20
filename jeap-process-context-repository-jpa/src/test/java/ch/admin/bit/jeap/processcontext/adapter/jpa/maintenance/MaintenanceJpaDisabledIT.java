package ch.admin.bit.jeap.processcontext.adapter.jpa.maintenance;

import ch.admin.bit.jeap.processcontext.adapter.jpa.JpaAdapterConfig;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextFactory;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = JpaAdapterConfig.class)
class MaintenanceJpaDisabledIT {

    @Autowired
    private ApplicationContext applicationContext;
    @MockitoBean
    private ProcessTemplateRepository processTemplateRepository;
    @MockitoBean
    private ProcessContextFactory processContextFactory;

    @Test
    void maintenanceRepositoriesAreNotRegistered() {
        assertThat(applicationContext.getBeansOfType(MaintenanceJobRepository.class)).isEmpty();
        assertThat(applicationContext.containsBean("maintenanceJobJpaRepository")).isFalse();
        assertThat(applicationContext.containsBean("maintenanceTaskJpaRepository")).isFalse();
    }
}
