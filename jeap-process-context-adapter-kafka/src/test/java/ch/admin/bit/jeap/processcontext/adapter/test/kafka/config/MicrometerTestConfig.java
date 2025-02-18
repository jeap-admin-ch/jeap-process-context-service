package ch.admin.bit.jeap.processcontext.adapter.test.kafka.config;

import ch.admin.bit.jeap.processcontext.domain.port.MetricsListener;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MicrometerTestConfig {

    @Bean
    public MeterRegistry meterRegistry(){
        return new SimpleMeterRegistry();
    }

    @Bean
    public MetricsListener metricsListener() {
        return new StubMetricsListener();
    }
}
