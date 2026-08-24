package ch.admin.bit.jeap.processcontext.adapter.kafka.internalevent.producer;

import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.TransactionalOutbox;
import ch.admin.bit.jeap.processcontext.adapter.kafka.TopicConfiguration;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceEventPublisher;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJob;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTask;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jeap.processcontext.maintenance", name = "enabled", havingValue = "true")
class OutboxMaintenanceEventPublisher implements MaintenanceEventPublisher {
    private final TransactionalOutbox transactionalOutbox;
    private final TopicConfiguration topicConfiguration;
    private final InternalMessageFactory messageFactory;

    @Override
    public void publish(MaintenanceJob job, MaintenanceTask task) {
        transactionalOutbox.sendMessage(
                messageFactory.processContextOutdatedMaintenanceEvent(job, task),
                messageFactory.key(messageFactory.maintenanceProcessId(task)),
                topicConfiguration.getProcessOutdatedInternal());
    }
}
