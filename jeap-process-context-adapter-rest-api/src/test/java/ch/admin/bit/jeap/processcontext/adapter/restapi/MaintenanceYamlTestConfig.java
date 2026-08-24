package ch.admin.bit.jeap.processcontext.adapter.restapi;

import ch.admin.bit.jeap.processcontext.adapter.restapi.config.MaintenanceYamlConverterCustomizer;
import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
class MaintenanceYamlTestConfig {

    @Bean
    ServerHttpMessageConvertersCustomizer yamlConverterCustomizer() {
        return new MaintenanceYamlConverterCustomizer(1_024);
    }
}
