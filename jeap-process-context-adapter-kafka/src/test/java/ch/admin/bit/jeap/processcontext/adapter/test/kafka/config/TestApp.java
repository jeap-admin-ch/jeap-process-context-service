package ch.admin.bit.jeap.processcontext.adapter.test.kafka.config;

import ch.admin.bit.jeap.messaging.annotations.JeapMessageConsumerContractsByTemplates;
import ch.admin.bit.jeap.processcontext.adapter.kafka.TopicConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableConfigurationProperties(TopicConfiguration.class)
@ComponentScan(value = {"ch.admin.bit.jeap.processcontext.adapter.kafka"})
@Import({
        MicrometerTestConfig.class
})
@JeapMessageConsumerContractsByTemplates
public class TestApp {
}
