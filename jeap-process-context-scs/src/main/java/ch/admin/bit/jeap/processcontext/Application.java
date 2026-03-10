package ch.admin.bit.jeap.processcontext;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.data.RepositoryMetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

// Exclude: Disable Spring Data repository metrics to avoid conflicting with @Timed annotations on repository interfaces
@SpringBootApplication(exclude = RepositoryMetricsAutoConfiguration.class)
@EnableAsync
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
