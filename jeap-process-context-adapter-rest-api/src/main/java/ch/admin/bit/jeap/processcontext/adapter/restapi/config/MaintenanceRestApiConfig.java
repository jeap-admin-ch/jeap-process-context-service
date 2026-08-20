package ch.admin.bit.jeap.processcontext.adapter.restapi.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(value = "jeap.processcontext.maintenance.enabled", havingValue = "true")
class MaintenanceRestApiConfig {

    @Bean
    ServerHttpMessageConvertersCustomizer maintenanceYamlConverterCustomizer() {
        return new MaintenanceYamlConverterCustomizer();
    }
}
