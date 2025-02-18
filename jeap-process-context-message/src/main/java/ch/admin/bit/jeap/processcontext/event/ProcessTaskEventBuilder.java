package ch.admin.bit.jeap.processcontext.event;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEvent;
import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bit.jeap.messaging.model.MessagePayload;
import ch.admin.bit.jeap.messaging.model.MessageReferences;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * This helper class for building task events has been deprecated because task events themselves are deprecated. These
 * events are no longer needed, as tasks can now be planned and completed using 'normal' domain events instead.
 */
@Deprecated(forRemoval = true, since = "5.11.0")
@Slf4j
@SuppressWarnings("findbugs:SS_SHOULD_BE_STATIC")
abstract class ProcessTaskEventBuilder<BuilderType extends AvroDomainEventBuilder<?, ?>, EventType extends AvroDomainEvent> extends AvroDomainEventBuilder<BuilderType, EventType> {
    private final Supplier<MessageReferences> refSupplier;
    @Getter
    private String serviceName;
    @Getter
    private String systemName;
    @Getter
    private String processId;
    @Getter
    private List<String> taskIds = new ArrayList<>();

    protected ProcessTaskEventBuilder(Supplier<EventType> builderSupplier, Supplier<MessageReferences> refSupplier) {
        super(builderSupplier);
        this.refSupplier = refSupplier;
    }

    @Override
    protected String getGeneratedOrSpecifiedVersion(AvroMessage message) {
        return "1.0.0";
    }

    public BuilderType systemName(String systemName) {
        this.systemName = systemName;
        return self();
    }

    public BuilderType serviceName(String serviceName) {
        this.serviceName = serviceName;
        return self();
    }

    public BuilderType processId(String processId) {
        this.processId = processId;
        return self();
    }

    public BuilderType taskIds(List<String> taskIds) {
        this.taskIds = new ArrayList<>(taskIds);
        return self();
    }

    @Override
    public final EventType build() {
        if (this.processId == null) {
            throw AvroMessageBuilderException.propertyNull("processId");
        }

        setPayload(buildPayload());
        setReferences(refSupplier.get());
        setProcessId(this.processId);
        return super.build();
    }

    protected abstract MessagePayload buildPayload();
}
