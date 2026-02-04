package ch.admin.bit.jeap.processcontext.domain.port;

public interface InternalMessageProducer {

    void produceProcessContextOutdatedEventSynchronously(String originProcessId);

    void produceProcessContextOutdatedMigrationTriggerEventSynchronously(String originProcessId);
}
