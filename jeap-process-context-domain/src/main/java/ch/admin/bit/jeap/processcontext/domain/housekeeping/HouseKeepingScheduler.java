package ch.admin.bit.jeap.processcontext.domain.housekeeping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class HouseKeepingScheduler {

    private final HouseKeepingService houseKeepingService;

    @Scheduled(cron = "#{@houseKeepingConfigProperties.cronExpression}")
    @SchedulerLock(name = "HouseKeepingScheduler_execute", lockAtLeastFor = "#{@houseKeepingConfigProperties.lockAtLeast.toString()}", lockAtMostFor = "#{@houseKeepingConfigProperties.lockAtMost.toString()}")
    public void execute() {
        LockAssert.assertLocked();
        log.info("Housekeeping started...");
        houseKeepingService.cleanup();
        log.info("Housekeeping ended");
    }

}
