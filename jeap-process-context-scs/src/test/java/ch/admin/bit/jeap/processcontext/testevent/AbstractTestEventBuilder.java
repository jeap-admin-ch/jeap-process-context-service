package ch.admin.bit.jeap.processcontext.testevent;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEvent;
import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.messaging.model.MessageReferences;
import lombok.Getter;

import java.util.function.Supplier;

public abstract class AbstractTestEventBuilder<BuilderType extends AvroDomainEventBuilder<BuilderType, EventType>, EventType extends AvroDomainEvent>
        extends AvroDomainEventBuilder<BuilderType, EventType> {

    private final Supplier<MessageReferences> referencesSupplier;

    @Getter
    private final String eventName;
    @Getter
    private final String processId;

    protected AbstractTestEventBuilder(
            Supplier<EventType> constructor,
            Supplier<MessageReferences> referencesSupplier,
            String eventName,
            String processId) {
        super(constructor);
        this.referencesSupplier = referencesSupplier;
        this.eventName = eventName;
        this.processId = processId;
    }

    @Override
    protected final String getSpecifiedMessageTypeVersion() {
        return "1.0.0";
    }

    @Override
    protected final String getServiceName() {
        return "service";
    }

    @Override
    protected final String getSystemName() {
        return "system";
    }

    @Override
    @SuppressWarnings("unchecked")
    protected final BuilderType self() {
        return (BuilderType) this;
    }

    @Override
    public EventType build() {
        setReferences(referencesSupplier.get());
        setProcessId(processId);
        return super.build();
    }
}
