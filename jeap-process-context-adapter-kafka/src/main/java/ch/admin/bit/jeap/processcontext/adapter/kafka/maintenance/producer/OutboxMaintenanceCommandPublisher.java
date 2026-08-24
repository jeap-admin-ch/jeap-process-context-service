package ch.admin.bit.jeap.processcontext.adapter.kafka.maintenance.producer;

import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.TransactionalOutbox;
import ch.admin.bit.jeap.processcontext.adapter.kafka.TopicConfiguration;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceCommandPublisher;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJob;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTask;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jeap.processcontext.maintenance", name = "enabled", havingValue = "true")
class OutboxMaintenanceCommandPublisher implements MaintenanceCommandPublisher {
    private final TransactionalOutbox transactionalOutbox;
    private final TopicConfiguration topicConfiguration;
    private final AddProcessDataCommandFactory commandFactory;

    @Override
    public void publish(MaintenanceJob job, MaintenanceTask task) {
        transactionalOutbox.sendMessage(
                commandFactory.create(job, task),
                commandFactory.key(task),
                topicConfiguration.getAddProcessDataCommand());
    }
}
