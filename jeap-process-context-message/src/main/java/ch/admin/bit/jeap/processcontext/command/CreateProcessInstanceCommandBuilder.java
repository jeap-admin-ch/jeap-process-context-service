package ch.admin.bit.jeap.processcontext.command;

import ch.admin.bit.jeap.command.avro.AvroCommandBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bit.jeap.processcontext.command.process.instance.create.CreateProcessInstanceCommand;
import ch.admin.bit.jeap.processcontext.command.process.instance.create.CreateProcessInstanceCommandPayload;
import ch.admin.bit.jeap.processcontext.command.process.instance.create.CreateProcessInstanceCommandReferences;
import ch.admin.bit.jeap.processcontext.command.process.instance.create.ProcessData;
import lombok.Getter;

import java.util.List;

public class CreateProcessInstanceCommandBuilder extends AvroCommandBuilder<CreateProcessInstanceCommandBuilder, CreateProcessInstanceCommand> {

    @Getter
    private String serviceName;
    @Getter
    private String systemName;
    @Getter
    private String processTemplateName;
    @Getter
    private String processId;
    @Getter
    private List<ProcessData> processData;

    private CreateProcessInstanceCommandBuilder() {
        super(CreateProcessInstanceCommand::new);
    }

    public static CreateProcessInstanceCommandBuilder create() {
        return new CreateProcessInstanceCommandBuilder();
    }

    @Override
    protected String getSpecifiedMessageTypeVersion() {
        return "1.0.0";
    }

    @Override
    protected CreateProcessInstanceCommandBuilder self() {
        return this;
    }

    public CreateProcessInstanceCommandBuilder systemName(String systemName) {
        this.systemName = systemName;
        return this;
    }

    public CreateProcessInstanceCommandBuilder serviceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    public CreateProcessInstanceCommandBuilder processTemplateName(String processTemplateName) {
        this.processTemplateName = processTemplateName;
        return this;
    }

    public CreateProcessInstanceCommandBuilder processData(List<ProcessData> processData) {
        this.processData = processData;
        return this;
    }

    public CreateProcessInstanceCommandBuilder processId(String processId) {
        this.processId = processId;
        return this;
    }

    @Override
    public CreateProcessInstanceCommand build() {
        if (this.processId == null) {
            throw AvroMessageBuilderException.propertyNull("processId");
        }
        setProcessId(this.processId);
        CreateProcessInstanceCommandPayload payload = CreateProcessInstanceCommandPayload.newBuilder()
                .setProcessTemplateName(processTemplateName)
                .setProcessData(processData)
                .build();
        setPayload(payload);

        CreateProcessInstanceCommandReferences references = CreateProcessInstanceCommandReferences.newBuilder().build();
        setReferences(references);
        return super.build();
    }

}
