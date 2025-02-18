package ch.admin.bit.jeap.processcontext.testevent;

import ch.admin.bit.jeap.command.avro.AvroCommand;
import ch.admin.bit.jeap.command.avro.AvroCommandBuilder;
import ch.admin.bit.jeap.messaging.model.MessageReferences;
import com.fasterxml.uuid.Generators;
import lombok.Getter;

import java.util.function.Supplier;

public class AbstractTestCommandBuilder<BuilderType extends AvroCommandBuilder, CommandType extends AvroCommand> extends AvroCommandBuilder<BuilderType, CommandType> {

    private final Supplier<MessageReferences> referencesSupplier;

    @Getter
    private final String eventName;
    @Getter
    private final String processId;

    protected AbstractTestCommandBuilder(
            Supplier<CommandType> constructor,
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
    public CommandType build() {
        idempotenceId(Generators.timeBasedEpochGenerator().generate().toString());
        setReferences(referencesSupplier.get());
        setProcessId(processId);
        return super.build();
    }
}
