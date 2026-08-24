package ch.admin.bit.jeap.processcontext.adapter.kafka.maintenance.producer;

import ch.admin.bit.jeap.command.avro.AvroCommandBuilder;
import ch.admin.bit.jeap.processcontext.command.addprocessdata.AddProcessDataCommand;
import ch.admin.bit.jeap.processcontext.command.addprocessdata.AddProcessDataCommandPayload;
import ch.admin.bit.jeap.processcontext.command.addprocessdata.AddProcessDataCommandReferences;
import ch.admin.bit.jeap.processcontext.command.addprocessdata.AddProcessDataMessageKey;
import ch.admin.bit.jeap.processcontext.command.addprocessdata.ProcessDataValue;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceCommand;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJob;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTask;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AddProcessDataCommandFactory {
    private final String systemName;
    private final String serviceName;

    public AddProcessDataCommandFactory(@Value("${jeap.messaging.kafka.system-name}") String systemName,
                                        @Value("${jeap.messaging.kafka.service-name}") String serviceName) {
        this.systemName = systemName;
        this.serviceName = serviceName;
    }

    public AddProcessDataCommand create(MaintenanceJob job, MaintenanceTask task) {
        return new Builder(systemName, serviceName, MaintenanceCommand.from(job, task)).build();
    }

    public AddProcessDataMessageKey key(MaintenanceTask task) {
        return AddProcessDataMessageKey.newBuilder()
                .setOriginProcessId(task.originProcessId())
                .build();
    }

    private static class Builder extends AvroCommandBuilder<Builder, AddProcessDataCommand> {
        private final String systemName;
        private final String serviceName;
        private final MaintenanceCommand command;

        private Builder(String systemName, String serviceName, MaintenanceCommand command) {
            super(AddProcessDataCommand::new);
            this.systemName = systemName;
            this.serviceName = serviceName;
            this.command = command;
        }

        @Override
        public AddProcessDataCommand build() {
            idempotenceId = command.idempotenceId();
            setProcessId(command.processId());
            setReferences(AddProcessDataCommandReferences.newBuilder().build());
            setPayload(AddProcessDataCommandPayload.newBuilder()
                    .setJobId(command.jobId())
                    .setTaskId(command.taskId())
                    .setProcessTemplateName(command.processTemplateName())
                    .setProcessData(command.processData().stream()
                            .map(value -> ProcessDataValue.newBuilder()
                                    .setKey(value.key())
                                    .setValue(value.value())
                                    .setRole(value.role())
                                    .build())
                            .toList())
                    .build());
            return super.build();
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
        protected Builder self() {
            return this;
        }
    }
}
