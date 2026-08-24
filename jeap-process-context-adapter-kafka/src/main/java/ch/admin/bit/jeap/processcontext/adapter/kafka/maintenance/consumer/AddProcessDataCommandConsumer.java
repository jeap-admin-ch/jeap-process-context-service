package ch.admin.bit.jeap.processcontext.adapter.kafka.maintenance.consumer;

import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.processcontext.command.addprocessdata.AddProcessDataCommand;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceCommand;
import ch.admin.bit.jeap.processcontext.domain.maintenance.AddProcessDataCommandService;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceCommandRejectedException;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTargetNotFoundException;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTaskNotFoundException;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTaskResultService;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessDataValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "jeap.processcontext.maintenance", name = "enabled", havingValue = "true")
public class AddProcessDataCommandConsumer implements AcknowledgingMessageListener<AvroMessageKey, AddProcessDataCommand> {
    private final AddProcessDataCommandService commandService;
    private final MaintenanceTaskResultService resultService;

    @KafkaListener(groupId = "${spring.application.name}-add-process-data-command",
            topics = "${jeap.processcontext.kafka.topic.add-process-data-command}")
    @Override
    public void onMessage(ConsumerRecord<AvroMessageKey, AddProcessDataCommand> consumerRecord,
                          Acknowledgment acknowledgment) {
        AddProcessDataCommand command = consumerRecord.value();
        UUID taskId = command.getPayload().getTaskId();
        try {
            commandService.handle(toDomain(command));
        } catch (MaintenanceTargetNotFoundException e) {
            resultService.markNotFoundInNewTransaction(taskId);
            log.warn("Backfill target not found for job '{}' task '{}'.",
                    command.getPayload().getJobId(), taskId);
        } catch (MaintenanceCommandRejectedException e) {
            resultService.markFailedInNewTransaction(taskId, e);
            log.warn("Rejected add process data command for job '{}' task '{}'.",
                    command.getPayload().getJobId(), taskId);
        } catch (MaintenanceTaskNotFoundException e) {
            log.warn("Ignored add process data command for unknown maintenance task '{}' in job '{}'.",
                    taskId, command.getPayload().getJobId());
        } catch (RuntimeException e) {
            resultService.markFailedInNewTransaction(taskId, e);
            log.error("Add process data command failed for job '{}' task '{}' with exception type '{}'.",
                    command.getPayload().getJobId(), taskId, e.getClass().getSimpleName());
        }
        acknowledgment.acknowledge();
    }

    private static MaintenanceCommand toDomain(AddProcessDataCommand command) {
        var payload = command.getPayload();
        return new MaintenanceCommand(
                payload.getJobId(),
                payload.getTaskId(),
                payload.getProcessTemplateName(),
                payload.getProcessData().stream()
                        .map(value -> new ProcessDataValue(value.getKey(), value.getValue(), value.getRole()))
                        .toList(),
                command.getProcessId(),
                command.getIdentity().getIdempotenceId());
    }
}
