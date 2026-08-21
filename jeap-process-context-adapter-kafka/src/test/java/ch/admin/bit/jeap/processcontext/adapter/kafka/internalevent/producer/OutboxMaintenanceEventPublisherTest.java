package ch.admin.bit.jeap.processcontext.adapter.kafka.internalevent.producer;

import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.TransactionalOutbox;
import ch.admin.bit.jeap.processcontext.adapter.kafka.TopicConfiguration;
import ch.admin.bit.jeap.processcontext.domain.maintenance.*;
import ch.admin.bit.jeap.processcontext.internal.event.key.ProcessContextProcessIdKey;
import ch.admin.bit.jeap.processcontext.internal.event.outdated.ProcessContextOutdatedEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

class OutboxMaintenanceEventPublisherTest {
    private static final UUID JOB_ID = UUID.fromString("88dbb65f-9634-4685-bc86-17b72d715d3e");
    private static final UUID TASK_ID = UUID.fromString("019c8c72-6fd1-7f25-a9a1-3b3d51fbb321");

    @Test
    void publish_queuesEventWithOriginProcessIdKey() {
        TransactionalOutbox outbox = mock(TransactionalOutbox.class);
        TopicConfiguration topics = new TopicConfiguration();
        topics.setProcessOutdatedInternal("process-outdated");
        InternalMessageFactory factory = mock(InternalMessageFactory.class);
        MaintenanceTask task = task();
        MaintenanceJob job = job(task);
        ProcessContextOutdatedEvent event = mock(ProcessContextOutdatedEvent.class);
        ProcessContextProcessIdKey key = ProcessContextProcessIdKey.newBuilder().setProcessId("assessment-4711").build();
        when(factory.processContextOutdatedMaintenanceEvent(job, task)).thenReturn(event);
        when(factory.key("assessment-4711")).thenReturn(key);

        new OutboxMaintenanceEventPublisher(outbox, topics, factory).publish(job, task);

        verify(outbox).sendMessage(event, key, "process-outdated");
    }

    private static MaintenanceJob job(MaintenanceTask task) {
        return new MaintenanceJob(JOB_ID, MaintenanceJobType.RELATION_REEVALUATION, "assessmentProcess",
                "a".repeat(64), MaintenanceJobState.OPEN, null, task.createdAt(), null, null, null, List.of(task));
    }

    private static MaintenanceTask task() {
        Instant now = Instant.parse("2026-08-06T08:03:12Z");
        return new MaintenanceTask(TASK_ID, MaintenanceTargetType.PROCESS, "assessment-4711", "assessment-4711",
                MaintenanceTaskState.EVENT_QUEUED, now, null, null, null);
    }
}
