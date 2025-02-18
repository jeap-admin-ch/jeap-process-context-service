package ch.admin.bit.jeap.processcontext.event;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bit.jeap.processcontext.event.process.milestone.reached.ProcessMilestoneReachedEvent;
import ch.admin.bit.jeap.processcontext.event.process.milestone.reached.ProcessMilestoneReachedPayload;
import ch.admin.bit.jeap.processcontext.event.process.milestone.reached.ProcessMilestoneReachedReferences;
import lombok.Getter;

public class ProcessMilestoneReachedEventBuilder extends AvroDomainEventBuilder<ProcessMilestoneReachedEventBuilder, ProcessMilestoneReachedEvent> {

    @Getter
    private String serviceName;
    @Getter
    private String systemName;
    @Getter
    private String processId;

    private String milestoneName;

    private ProcessMilestoneReachedEventBuilder() {
        super(ProcessMilestoneReachedEvent::new);
    }

    public static ProcessMilestoneReachedEventBuilder create() {
        return new ProcessMilestoneReachedEventBuilder();
    }

    @Override
    protected String getSpecifiedMessageTypeVersion() {
        return "1.1.0";
    }

    @Override
    protected ProcessMilestoneReachedEventBuilder self() {
        return this;
    }

    public ProcessMilestoneReachedEventBuilder systemName(String systemName) {
        this.systemName = systemName;
        return this;
    }

    public ProcessMilestoneReachedEventBuilder serviceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    public ProcessMilestoneReachedEventBuilder processId(String processId) {
        this.processId = processId;
        return this;
    }

    public ProcessMilestoneReachedEventBuilder milestoneName(String milestoneName) {
        this.milestoneName = milestoneName;
        return this;
    }

    @Override
    public ProcessMilestoneReachedEvent build() {
        if (this.processId == null) {
            throw AvroMessageBuilderException.propertyNull("processId");
        }
        if (this.milestoneName == null) {
            throw AvroMessageBuilderException.propertyNull("milestoneName");
        }
        setProcessId(this.processId);
        ProcessMilestoneReachedReferences references = ProcessMilestoneReachedReferences.newBuilder().build();
        setReferences(references);
        ProcessMilestoneReachedPayload payload = ProcessMilestoneReachedPayload.newBuilder()
                .setMilestoneName(milestoneName)
                .build();
        setPayload(payload);
        return super.build();
    }

}
