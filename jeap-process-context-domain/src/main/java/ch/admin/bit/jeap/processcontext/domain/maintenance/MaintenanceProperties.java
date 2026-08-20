package ch.admin.bit.jeap.processcontext.domain.maintenance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "jeap.processcontext.maintenance")
@Validated
@Data
public class MaintenanceProperties {
    private boolean enabled;

    @Valid
    private Limits limits = new Limits();

    @Data
    public static class Limits {
        @Min(1)
        private int maxTasksPerJob = 10_000;

        @Min(1)
        @Max(2_000)
        private int maxFieldLength = 2_000;
    }
}
