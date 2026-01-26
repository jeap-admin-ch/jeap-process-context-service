package ch.admin.bit.jeap.processcontext.domain.port;

public interface ProcessInstanceEventProducer {

    void produceProcessSnapshotCreatedEventSynchronously(String originProcessId, int snapshotVersion);
}
