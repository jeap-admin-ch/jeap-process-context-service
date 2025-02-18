package ch.admin.bit.jeap.processcontext.domain.port;

public interface ProcessInstanceEventProducer {

    void produceProcessInstanceCreatedEventSynchronously(String originProcessId, String processName);

    void produceProcessInstanceCompletedEventSynchronously(String originProcessId);

    void produceProcessMilestoneReachedEventSynchronously(String originProcessId, String milestoneName);

    void produceProcessSnapshotCreatedEventSynchronously(String originProcessId, int snapshotVersion);
}
