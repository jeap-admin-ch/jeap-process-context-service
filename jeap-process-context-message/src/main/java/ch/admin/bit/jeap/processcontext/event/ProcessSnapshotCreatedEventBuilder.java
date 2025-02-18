package ch.admin.bit.jeap.processcontext.event;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bit.jeap.processcontext.event.process.snapshot.created.ProcessSnapshotCreatedEvent;
import ch.admin.bit.jeap.processcontext.event.process.snapshot.created.ProcessSnapshotCreatedReference;
import ch.admin.bit.jeap.processcontext.event.process.snapshot.created.ProcessSnapshotCreatedReferences;
import lombok.Getter;

public class ProcessSnapshotCreatedEventBuilder extends AvroDomainEventBuilder<ProcessSnapshotCreatedEventBuilder, ProcessSnapshotCreatedEvent> {

    private static final String PROCESS_ID = "processId";

    @Getter
    private String serviceName;
    @Getter
    private String systemName;
    @Getter
    private String processId;

    private Integer snapshotVersion;

    private ProcessSnapshotCreatedEventBuilder() {
        super(ProcessSnapshotCreatedEvent::new);
    }

    public static ProcessSnapshotCreatedEventBuilder create() {
        return new ProcessSnapshotCreatedEventBuilder();
    }

    @Override
    protected String getSpecifiedMessageTypeVersion() {
        return "1.1.0";
    }

    @Override
    protected ProcessSnapshotCreatedEventBuilder self() {
        return this;
    }

    public ProcessSnapshotCreatedEventBuilder systemName(String systemName) {
        this.systemName = systemName;
        return this;
    }

    public ProcessSnapshotCreatedEventBuilder serviceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    public ProcessSnapshotCreatedEventBuilder processId(String processId) {
        this.processId = processId;
        return this;
    }

    public ProcessSnapshotCreatedEventBuilder snapshotVersion(int snapshotVersion) {
        this.snapshotVersion = snapshotVersion;
        return this;
    }

    @Override
    public ProcessSnapshotCreatedEvent build() {
        if (this.processId == null) {
            throw AvroMessageBuilderException.propertyNull(PROCESS_ID);
        }
        if (this.snapshotVersion == null) {
            throw AvroMessageBuilderException.propertyNull("snapshotVersion");
        }
        setProcessId(this.processId);
        ProcessSnapshotCreatedReference reference = ProcessSnapshotCreatedReference.newBuilder()
                .setProcessId(this.processId)
                .setSnapshotVersion(this.snapshotVersion)
                .setType(PROCESS_ID)
                .build();
        setReferences(ProcessSnapshotCreatedReferences.newBuilder()
                .setReference(reference)
                .build());
        return super.build();
    }

}
