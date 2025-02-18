package ch.admin.bit.jeap.processcontext.adapter.kafka.internalevent.producer;


import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.processcontext.internal.event.key.ProcessContextProcessIdKey;
import ch.admin.bit.jeap.processcontext.internal.event.outdated.ProcessContextOutdatedEvent;
import ch.admin.bit.jeap.processcontext.internal.event.outdated.ProcessContextOutdatedReferences;
import ch.admin.bit.jeap.processcontext.internal.event.statechanged.ProcessContextStateChangedEvent;
import ch.admin.bit.jeap.processcontext.internal.event.statechanged.ProcessContextStateChangedReferences;
import com.fasterxml.uuid.Generators;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class InternalMessageFactory {

    @Value("${jeap.messaging.kafka.system-name}")
    private String systemName;

    @Value("${jeap.messaging.kafka.system-name}")
    private String serviceName;

    ProcessContextStateChangedEvent processContextStateChangedEvent(String originProcessId) {
        return new ProcessContextStateChangedEventBuilder(originProcessId)
                .build();
    }

    ProcessContextOutdatedEvent processContextOutdatedEvent(String originProcessId) {
        return new ProcessContextUpdatedEventBuilder(originProcessId)
                .build();
    }

    ProcessContextProcessIdKey key(String originProcessId) {
        return ProcessContextProcessIdKey.newBuilder()
                .setProcessId(originProcessId)
                .build();
    }

    private class ProcessContextUpdatedEventBuilder extends AvroDomainEventBuilder<ProcessContextUpdatedEventBuilder, ProcessContextOutdatedEvent> {

        protected ProcessContextUpdatedEventBuilder(String originProcessId) {
            super(ProcessContextOutdatedEvent::new);
            setProcessId(originProcessId);
            setReferences(ProcessContextOutdatedReferences.newBuilder().build());
            // An UUID is sufficient here, processing these events is naturally idempotent (actions are only taken if necessary due to the current process context state)
            idempotenceId = Generators.timeBasedEpochGenerator().generate().toString();
        }

        @Override
        protected String getSpecifiedMessageTypeVersion() {
            return "1.0.0";
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

    private class ProcessContextStateChangedEventBuilder extends AvroDomainEventBuilder<ProcessContextStateChangedEventBuilder, ProcessContextStateChangedEvent> {

        protected ProcessContextStateChangedEventBuilder(String originProcessId) {
            super(ProcessContextStateChangedEvent::new);
            setProcessId(originProcessId);
            setReferences(ProcessContextStateChangedReferences.newBuilder().build());
            // An UUID is sufficient here, processing these events is naturally idempotent (actions are only taken if necessary due to the current process context state)
            idempotenceId = Generators.timeBasedEpochGenerator().generate().toString();
        }

        @Override
        protected String getSpecifiedMessageTypeVersion() {
            return "1.0.0";
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
        protected ProcessContextStateChangedEventBuilder self() {
            return this;
        }
    }
}
