package ch.admin.bit.jeap.processcontext.adapter.kafka.internalevent.producer;


import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.processcontext.internal.event.key.ProcessContextProcessIdKey;
import ch.admin.bit.jeap.processcontext.internal.event.outdated.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
class InternalMessageFactory {

    @Value("${jeap.messaging.kafka.system-name}")
    private String systemName;

    @Value("${jeap.messaging.kafka.service-name}")
    private String serviceName;

    ProcessContextOutdatedEvent processContextOutdatedCreateProcessEvent(String originProcessId, UUID messageId,
                                                                         String messageName, String idempotenceId, String templateName) {
        return new ProcessContextUpdatedEventBuilder(originProcessId, messageName, idempotenceId)
                .messageCreatingProcessReceived(messageId, templateName)
                .build();
    }

    ProcessContextOutdatedEvent processContextOutdatedEvent(String originProcessId, UUID messageId, String messageName, String idempotenceId) {
        return new ProcessContextUpdatedEventBuilder(originProcessId, messageName, idempotenceId)
                .messageReceived(messageId)
                .build();
    }

    ProcessContextOutdatedEvent processContextOutdatedMigrationTriggerEvent(String originProcessId, String idempotenceId) {
        return new ProcessContextUpdatedEventBuilder(originProcessId, "migration", idempotenceId)
                .triggerMigration()
                .build();
    }

    ProcessContextProcessIdKey key(String originProcessId) {
        return ProcessContextProcessIdKey.newBuilder()
                .setProcessId(originProcessId)
                .build();
    }

    private class ProcessContextUpdatedEventBuilder extends AvroDomainEventBuilder<ProcessContextUpdatedEventBuilder, ProcessContextOutdatedEvent> {

        /**
         * @param originProcessId Origin Process ID, mapped to the processId attribute of the event
         * @param idempotenceId   Idempotence ID
         */
        protected ProcessContextUpdatedEventBuilder(String originProcessId, String messageName, String idempotenceId) {
            super(ProcessContextOutdatedEvent::new);
            setProcessId(originProcessId);
            setReferences(ProcessContextOutdatedReferences.newBuilder().build());
            this.idempotenceId = createIdempotenceId(originProcessId, messageName, idempotenceId);
        }

        private static String createIdempotenceId(String originProcessId, String messageName, String idempotenceId) {
            return originProcessId + "-" + messageName + "-" + idempotenceId;
        }

        ProcessContextUpdatedEventBuilder triggerMigration() {
            setPayload(ProcessContextOutdatedPayload.newBuilder()
                    .setProcessUpdateType(ProcessUpdateType.MIGRATION_TRIGGERED)
                    .build());
            return this;
        }

        ProcessContextUpdatedEventBuilder messageReceived(UUID messageId) {
            setPayload(ProcessContextOutdatedPayload.newBuilder()
                    .setProcessUpdateType(ProcessUpdateType.MESSAGE_RECEIVED)
                    .setReceivedMessageBuilder(ReceivedMessage.newBuilder()
                            .setMessageId(messageId))
                    .build());
            return this;
        }

        public ProcessContextUpdatedEventBuilder messageCreatingProcessReceived(UUID messageId, String templateName) {
            setPayload(ProcessContextOutdatedPayload.newBuilder()
                    .setProcessUpdateType(ProcessUpdateType.PROCESS_CREATION_MESSAGE_RECEIVED)
                    .setReceivedProcessCreationMessageBuilder(ReceivedProcessCreationMessage.newBuilder()
                            .setMessageId(messageId)
                            .setTemplateName(templateName))
                    .build());
            return this;
        }

        @Override
        protected String getServiceName() {
            return serviceName;
        }

        @Override
        protected String getSystemName() {
            return systemName;
        }

        @Override
        protected ProcessContextUpdatedEventBuilder self() {
            return this;
        }
    }
}
