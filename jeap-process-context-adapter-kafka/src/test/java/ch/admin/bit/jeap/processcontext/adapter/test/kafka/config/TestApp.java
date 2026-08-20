package ch.admin.bit.jeap.processcontext.adapter.test.kafka.config;

import ch.admin.bit.jeap.messaging.annotations.JeapMessageConsumerContractsByTemplates;
import ch.admin.bit.jeap.processcontext.adapter.kafka.TopicConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        FlywayAutoConfiguration.class
}, excludeName = {
        "ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.OutboxConfig",
        "ch.admin.bit.jeap.messaging.transactionaloutbox.transaction.OutboxTransactionConfig",
        "ch.admin.bit.jeap.messaging.transactionaloutbox.jpa.OutboxJpaConfig",
        "ch.admin.bit.jeap.messaging.transactionaloutbox.messaging.OutboxMessagingConfig",
        "ch.admin.bit.jeap.messaging.transactionaloutbox.scheduling.OutboxSchedulingConfig",
        "ch.admin.bit.jeap.messaging.transactionaloutbox.metrics.OutboxMetricsConfig",
        "ch.admin.bit.jeap.messaging.transactionaloutbox.config.TransactionalOutboxConfigurationProperties"
})
@EnableConfigurationProperties(TopicConfiguration.class)
@ComponentScan(value = {"ch.admin.bit.jeap.processcontext.adapter.kafka"})
@Import({
        MicrometerTestConfig.class
})
@JeapMessageConsumerContractsByTemplates
public class TestApp {
}
