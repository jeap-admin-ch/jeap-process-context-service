package ch.admin.bit.jeap.test.processcontext.persistence;

import ch.admin.bit.jeap.processcontext.StubMetricsListener;
import ch.admin.bit.jeap.processcontext.adapter.jpa.JpaAdapterConfig;
import ch.admin.bit.jeap.processcontext.domain.port.MetricsListener;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import(JpaAdapterConfig.class)
@ComponentScan(basePackages={
        "ch.admin.bit.jeap.processcontext.domain",
        "ch.admin.bit.jeap.processcontext.plugin.api",
        "ch.admin.bit.jeap.processcontext.repository.template.json",
        "ch.admin.bit.jeap.processcontext.adapter.objectstorage",})
public class DomainWithPersistenceConfig {

        @Bean
        public MetricsListener stubMetricsListener() {
            return new StubMetricsListener();
        }
}
