package ch.admin.bit.jeap.processcontext.domain.housekeeping;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Configuration for the automatic housekeeping
 */
@Configuration
@ConfigurationProperties(prefix = "jeap.processcontext.housekeeping")
@Data
public class HouseKeepingConfigProperties {

    /**
     * How often to run the scheduler? Must be a cron expression. Default: Once a Day at 00:20
     * see {@link org.springframework.scheduling.support.CronExpression}
     */
    private String cronExpression = "0 20 0 * * *";

    /**
     * Minimal time to keep a lock at this job,
     * see {@link net.javacrumbs.shedlock.spring.annotation.SchedulerLock}
     */
    private Duration lockAtLeast = Duration.of(5, ChronoUnit.SECONDS);
    /**
     * Maximal time to keep a lock at this job,
     * see {@link net.javacrumbs.shedlock.spring.annotation.SchedulerLock}
     */
    private Duration lockAtMost = Duration.of(30, ChronoUnit.MINUTES);

    /**
     * Delete completed process instances older than this value [duration]. Default is 180 days
     */
    private Duration completedProcessInstancesMaxAge = Duration.of(180, ChronoUnit.DAYS);

    /**
     * Delete started process instances older than this value [duration]. Default is 365 days
     */
    private Duration startedProcessInstancesMaxAge = Duration.of(365, ChronoUnit.DAYS);

    /**
     * Delete events without correlation with a process instance older than this value [duration]. Default is 90 days
     */
    private Duration eventsMaxAge = Duration.of(90, ChronoUnit.DAYS);

    /**
     * Delete process updates without correlation to a process (handled=false, failed=false) older than this value [duration]. Default is 90 days
     */
    private Duration processUpdateMaxAge = Duration.of(90, ChronoUnit.DAYS);

    /**
     * Size for the queries [pages]. Default is 500
     */
    private int pageSize = 500;

    /**
     * Max. pages to housekeep in one run. This limits the amount of time one housekeeping run can max. spend
     * (the time to delete maxPages * pageSize elements of each kind).
     */
    private int maxPages = 100000;
}
