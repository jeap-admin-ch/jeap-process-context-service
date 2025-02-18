package ch.admin.bit.jeap.processcontext.event;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bit.jeap.processcontext.event.process.instance.completed.ProcessInstanceCompletedEvent;
import ch.admin.bit.jeap.processcontext.event.process.instance.completed.ProcessInstanceCompletedReferences;
import lombok.Getter;


public class ProcessInstanceCompletedEventBuilder extends AvroDomainEventBuilder<ProcessInstanceCompletedEventBuilder, ProcessInstanceCompletedEvent> {

    @Getter
    private String serviceName;
    @Getter
    private String systemName;
    @Getter
    private String processId;

    private ProcessInstanceCompletedEventBuilder() {
        super(ProcessInstanceCompletedEvent::new);
    }

    public static ProcessInstanceCompletedEventBuilder create() {
        return new ProcessInstanceCompletedEventBuilder();
    }

    @Override
    protected String getSpecifiedMessageTypeVersion() {
        return "1.1.0";
    }

    @Override
    protected ProcessInstanceCompletedEventBuilder self() {
        return this;
    }

    public ProcessInstanceCompletedEventBuilder systemName(String systemName) {
        this.systemName = systemName;
        return this;
    }

    public ProcessInstanceCompletedEventBuilder serviceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    public ProcessInstanceCompletedEventBuilder processId(String processId) {
        this.processId = processId;
        return this;
    }

    @Override
    public ProcessInstanceCompletedEvent build() {
        if (this.processId == null) {
            throw AvroMessageBuilderException.propertyNull("processId");
        }
        setProcessId(this.processId);
        ProcessInstanceCompletedReferences references = ProcessInstanceCompletedReferences.newBuilder().build();
        setReferences(references);
        return super.build();
    }

}
