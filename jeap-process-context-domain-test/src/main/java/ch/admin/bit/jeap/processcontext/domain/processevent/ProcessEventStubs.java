package ch.admin.bit.jeap.processcontext.domain.processevent;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProcessEventStubs {

    public static ProcessEvent createProcessStarted(String originProcessId) {
        return ProcessEvent.createProcessStarted(originProcessId);
    }

    public static ProcessEvent createProcessCompleted(String originProcessId) {
        return ProcessEvent.createProcessCompleted(originProcessId);
    }

    public static ProcessEvent createMilestoneReached(String originProcessId, String milestoneName) {
        return ProcessEvent.createMilestoneReached(originProcessId, milestoneName);
    }
}
