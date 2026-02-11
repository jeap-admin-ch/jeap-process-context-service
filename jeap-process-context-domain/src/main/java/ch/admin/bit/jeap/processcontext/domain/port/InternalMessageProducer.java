package ch.admin.bit.jeap.processcontext.domain.port;

import java.util.UUID;

public interface InternalMessageProducer {

    void produceProcessContextOutdatedCreateProcessEventSynchronously(String originProcessId, UUID messageId,
                                                                      String messageName, String idempotenceId, String templateName);

    void produceProcessContextOutdatedEventSynchronously(String originProcessId, UUID messageId, String messageName, String idempotenceId);

    void produceProcessContextOutdatedMigrationTriggerEventSynchronously(String originProcessId, String idempotenceId);
}
