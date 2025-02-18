package ch.admin.bit.jeap.processcontext.event;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bit.jeap.processcontext.event.process.instance.created.ProcessInstanceCreatedEvent;
import ch.admin.bit.jeap.processcontext.event.process.instance.created.ProcessInstanceCreatedPayload;
import ch.admin.bit.jeap.processcontext.event.process.instance.created.ProcessInstanceCreatedReferences;
import lombok.Getter;


public class ProcessInstanceCreatedEventBuilder extends AvroDomainEventBuilder<ProcessInstanceCreatedEventBuilder, ProcessInstanceCreatedEvent> {

    @Getter
    private String serviceName;
    @Getter
    private String systemName;
    @Getter
    private String processId;

    private String processName;

    private ProcessInstanceCreatedEventBuilder() {
        super(ProcessInstanceCreatedEvent::new);
    }

    public static ProcessInstanceCreatedEventBuilder create() {
        return new ProcessInstanceCreatedEventBuilder();
    }

    @Override
    protected String getSpecifiedMessageTypeVersion() {
        return "1.1.0";
    }

    @Override
    protected ProcessInstanceCreatedEventBuilder self() {
        return this;
    }

    public ProcessInstanceCreatedEventBuilder systemName(String systemName) {
        this.systemName = systemName;
        return this;
    }

    public ProcessInstanceCreatedEventBuilder serviceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    public ProcessInstanceCreatedEventBuilder processId(String processId) {
        this.processId = processId;
        return this;
    }

    public ProcessInstanceCreatedEventBuilder processName(String processName) {
        this.processName = processName;
        return this;
    }

    @Override
    public ProcessInstanceCreatedEvent build() {
        if (this.processId == null) {
            throw AvroMessageBuilderException.propertyNull("processId");
        }
        if (this.processName == null) {
            throw AvroMessageBuilderException.propertyNull("processName");
        }
        setProcessId(this.processId);
        ProcessInstanceCreatedReferences references = ProcessInstanceCreatedReferences.newBuilder().build();
        setReferences(references);
        ProcessInstanceCreatedPayload payload = new ProcessInstanceCreatedPayload(processName);
        setPayload(payload);
        return super.build();
    }

}
