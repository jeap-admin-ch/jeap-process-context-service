package ch.admin.bit.jeap.processcontext.adapter.kafka.maintenance.consumer;

import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.avro.security.AvroClassSecurity;
import ch.admin.bit.jeap.processcontext.adapter.kafka.maintenance.producer.AddProcessDataCommandFactory;
import ch.admin.bit.jeap.processcontext.command.addprocessdata.AddProcessDataCommand;
import ch.admin.bit.jeap.processcontext.domain.maintenance.*;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessDataValue;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AddProcessDataCommandConsumerTest {
    private static final UUID JOB_ID = UUID.fromString("88dbb65f-9634-4685-bc86-17b72d715d3e");
    private static final UUID TASK_ID = UUID.fromString("019c8c72-6fd1-7f25-a9a1-3b3d51fbb321");

    @BeforeAll
    static void installAvroClassWhitelist() {
        AvroClassSecurity.installDefaultIfMissing();
    }

    private final AddProcessDataCommandService commandService = mock(AddProcessDataCommandService.class);
    private final MaintenanceTaskResultService resultService = mock(MaintenanceTaskResultService.class);
    private final AddProcessDataCommandConsumer consumer =
            new AddProcessDataCommandConsumer(commandService, resultService);
    private final Acknowledgment acknowledgment = mock(Acknowledgment.class);

    @Test
    void onMessage_successHandlesAndAcknowledges() {
        consumer.onMessage(consumerRecord(), acknowledgment);

        verify(commandService).handle(expectedCommand());
        verify(acknowledgment).acknowledge();
        verifyNoInteractions(resultService);
    }

    @Test
    void onMessage_commandFailurePersistsFailedResultAndAcknowledges() {
        IllegalStateException exception = new IllegalStateException("command failed");
        doThrow(exception).when(commandService).handle(any());

        consumer.onMessage(consumerRecord(), acknowledgment);

        InOrder persistenceBeforeAcknowledgment = inOrder(resultService, acknowledgment);
        persistenceBeforeAcknowledgment.verify(resultService).markFailedInNewTransaction(TASK_ID, exception);
        persistenceBeforeAcknowledgment.verify(acknowledgment).acknowledge();
    }

    @Test
    void onMessage_rejectedCommandPersistsFailedResultAndAcknowledges() {
        MaintenanceCommandRejectedException exception =
                new MaintenanceCommandRejectedException("Maintenance command jobId does not match the durable task");
        doThrow(exception).when(commandService).handle(any());

        consumer.onMessage(consumerRecord(), acknowledgment);

        InOrder persistenceBeforeAcknowledgment = inOrder(resultService, acknowledgment);
        persistenceBeforeAcknowledgment.verify(resultService).markFailedInNewTransaction(TASK_ID, exception);
        persistenceBeforeAcknowledgment.verify(acknowledgment).acknowledge();
    }

    @Test
    void onMessage_missingTaskAcknowledgesWithoutPersistingFailedResult() {
        doThrow(new MaintenanceTaskNotFoundException()).when(commandService).handle(any());

        consumer.onMessage(consumerRecord(), acknowledgment);

        verifyNoInteractions(resultService);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void onMessage_missingTargetPersistsNotFoundResultAndAcknowledges() {
        doThrow(new MaintenanceTargetNotFoundException()).when(commandService).handle(any());

        consumer.onMessage(consumerRecord(), acknowledgment);

        InOrder persistenceBeforeAcknowledgment = inOrder(resultService, acknowledgment);
        persistenceBeforeAcknowledgment.verify(resultService).markNotFoundInNewTransaction(TASK_ID);
        persistenceBeforeAcknowledgment.verify(acknowledgment).acknowledge();
    }

    @Test
    void onMessage_failedResultPersistencePropagatesWithoutAcknowledging() {
        IllegalStateException commandException = new IllegalStateException("command failed");
        IllegalStateException persistenceException = new IllegalStateException("database unavailable");
        doThrow(commandException).when(commandService).handle(any());
        doThrow(persistenceException).when(resultService).markFailedInNewTransaction(TASK_ID, commandException);

        assertThatThrownBy(() -> consumer.onMessage(consumerRecord(), acknowledgment))
                .isSameAs(persistenceException);

        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void kafkaListener_hasDedicatedConsumerGroup() throws NoSuchMethodException {
        KafkaListener listener = AddProcessDataCommandConsumer.class
                .getMethod("onMessage", ConsumerRecord.class, Acknowledgment.class)
                .getAnnotation(KafkaListener.class);

        assertThat(listener).isNotNull();
        assertThat(listener.groupId()).isEqualTo("${spring.application.name}-add-process-data-command");
    }

    private static ConsumerRecord<AvroMessageKey, AddProcessDataCommand> consumerRecord() {
        return new ConsumerRecord<>("add-process-data", 0, 0, null, command());
    }

    private static AddProcessDataCommand command() {
        MaintenanceTask task = task();
        MaintenanceJob job = new MaintenanceJob(JOB_ID, MaintenanceJobType.PROCESS_DATA_BACKFILL,
                "assessmentProcess", "a".repeat(64), MaintenanceJobState.OPEN, null, task.createdAt(), null,
                null, null, List.of(task));
        return new AddProcessDataCommandFactory("JEAP", "process-context-service").create(job, task);
    }

    private static MaintenanceCommand expectedCommand() {
        return MaintenanceCommand.from(new MaintenanceJob(JOB_ID, MaintenanceJobType.PROCESS_DATA_BACKFILL,
                "assessmentProcess", "a".repeat(64), MaintenanceJobState.OPEN, null, task().createdAt(), null,
                null, null, List.of(task())), task());
    }

    private static MaintenanceTask task() {
        return new MaintenanceTask(TASK_ID, MaintenanceTargetType.PROCESS, "assessment-4711", "assessment-4711",
                MaintenanceTaskState.COMMAND_QUEUED, Instant.parse("2026-08-06T08:03:12Z"), null, null, null,
                List.of(new ProcessDataValue("assessmentId", "a-123", null)));
    }
}
