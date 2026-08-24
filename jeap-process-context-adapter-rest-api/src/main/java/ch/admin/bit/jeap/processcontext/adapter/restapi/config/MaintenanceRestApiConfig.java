package ch.admin.bit.jeap.processcontext.adapter.restapi.config;

import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(value = "jeap.processcontext.maintenance.enabled", havingValue = "true")
class MaintenanceRestApiConfig {

    @Bean
    ServerHttpMessageConvertersCustomizer maintenanceYamlConverterCustomizer(
            MaintenanceProperties maintenanceProperties) {
        return new MaintenanceYamlConverterCustomizer(maintenanceProperties.getLimits().getMaxRequestBytes());
    }
}
