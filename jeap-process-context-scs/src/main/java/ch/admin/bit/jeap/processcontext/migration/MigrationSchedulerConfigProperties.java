package ch.admin.bit.jeap.processcontext.migration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Configuration
@ConfigurationProperties(prefix = "jeap.processcontext.template.migration")
@Data
public class MigrationSchedulerConfigProperties {

    /**
     * How often to run the migration scheduler. Must be a cron expression. Default: At :10 past every hour.
     */
    private String cronExpression = "0 10 * * * *";

    /**
     * Minimal time to keep a lock at this job.
     */
    private Duration lockAtLeast = Duration.of(1, ChronoUnit.MINUTES);

    /**
     * Maximal time to keep a lock at this job.
     */
    private Duration lockAtMost = Duration.of(60, ChronoUnit.MINUTES);

    /**
     * Max number of process instances to migrate per scheduled run.
     */
    private int batchSize = 500;

    /**
     * Only migrate uncompleted process instances created within this number of days. Default is 180 days.
     */
    private int maxCreatedAtAgeDays = 180;
}
