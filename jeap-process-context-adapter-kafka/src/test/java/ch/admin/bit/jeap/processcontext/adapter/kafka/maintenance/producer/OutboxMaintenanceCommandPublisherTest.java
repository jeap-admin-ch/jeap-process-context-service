package ch.admin.bit.jeap.processcontext.adapter.kafka.maintenance.producer;

import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.TransactionalOutbox;
import ch.admin.bit.jeap.processcontext.adapter.kafka.TopicConfiguration;
import ch.admin.bit.jeap.processcontext.command.addprocessdata.AddProcessDataCommand;
import ch.admin.bit.jeap.processcontext.command.addprocessdata.AddProcessDataMessageKey;
import ch.admin.bit.jeap.processcontext.domain.maintenance.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

class OutboxMaintenanceCommandPublisherTest {
    private static final UUID JOB_ID = UUID.fromString("88dbb65f-9634-4685-bc86-17b72d715d3e");
    private static final UUID TASK_ID = UUID.fromString("019c8c72-6fd1-7f25-a9a1-3b3d51fbb321");

    @Test
    void publish_queuesCommandWithOriginProcessIdKeyOnConfiguredTopic() {
        TransactionalOutbox outbox = mock(TransactionalOutbox.class);
        TopicConfiguration topics = new TopicConfiguration();
        topics.setAddProcessDataCommand("add-process-data");
        AddProcessDataCommandFactory factory = mock(AddProcessDataCommandFactory.class);
        MaintenanceTask task = task();
        MaintenanceJob job = job(task);
        AddProcessDataCommand command = mock(AddProcessDataCommand.class);
        AddProcessDataMessageKey key = new AddProcessDataMessageKey("assessment-4711");
        when(factory.create(job, task)).thenReturn(command);
        when(factory.key(task)).thenReturn(key);

        new OutboxMaintenanceCommandPublisher(outbox, topics, factory).publish(job, task);

        verify(outbox).sendMessage(command, key, "add-process-data");
    }

    private static MaintenanceJob job(MaintenanceTask task) {
        return new MaintenanceJob(JOB_ID, MaintenanceJobType.PROCESS_DATA_BACKFILL, "assessmentProcess",
                "a".repeat(64), MaintenanceJobState.OPEN, null, task.createdAt(), null, null, null, List.of(task));
    }

    private static MaintenanceTask task() {
        Instant now = Instant.parse("2026-08-06T08:03:12Z");
        return new MaintenanceTask(TASK_ID, MaintenanceTargetType.PROCESS, "assessment-4711", "assessment-4711",
                MaintenanceTaskState.COMMAND_QUEUED, now, null, null, null);
    }
}
