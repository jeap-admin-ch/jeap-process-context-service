package ch.admin.bit.jeap.processcontext.adapter.jpa.maintenance;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(value = "jeap.processcontext.maintenance.enabled", havingValue = "true")
@EnableJpaRepositories(basePackageClasses = MaintenanceJobJpaRepository.class)
class MaintenanceJpaConfig {
}
