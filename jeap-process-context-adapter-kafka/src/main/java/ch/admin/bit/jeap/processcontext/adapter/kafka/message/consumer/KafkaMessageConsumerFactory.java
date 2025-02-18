package ch.admin.bit.jeap.processcontext.adapter.kafka.message.consumer;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.spring.JeapKafkaBeanNames;
import ch.admin.bit.jeap.processcontext.domain.message.MessageReceiver;
import ch.admin.bit.jeap.processcontext.domain.port.MessageConsumerFactory;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
public class KafkaMessageConsumerFactory implements MessageConsumerFactory {

    private final String appName;

    private final KafkaProperties kafkaProperties;

    private final BeanFactory beanFactory;

    private final JeapKafkaBeanNames jeapKafkaBeanNames;

    private final List<ConcurrentMessageListenerContainer<?, ?>> containers = new CopyOnWriteArrayList<>();

    public KafkaMessageConsumerFactory(@Value("${spring.application.name}") String appName, KafkaProperties kafkaProperties, BeanFactory beanFactory) {
        this.appName = appName;
        this.kafkaProperties = kafkaProperties;
        this.beanFactory = beanFactory;
        this.jeapKafkaBeanNames = new JeapKafkaBeanNames(kafkaProperties.getDefaultClusterName());
    }

    @Override
    public void startConsumer(String topicName, String messageName, String clusterName, MessageReceiver messageReceiver) {
        if (!StringUtils.hasText(clusterName)) {
            clusterName = kafkaProperties.getDefaultClusterName();
        }

        log.info("Starting domain event listener for event '{}' on topic '{}' on cluster '{}'", messageName, topicName, clusterName);

        KafkaMessageListener listener = new KafkaMessageListener(messageName, messageReceiver);
        startConsumer(topicName, messageName, clusterName, listener);
    }

    private ConcurrentMessageListenerContainer<AvroMessageKey, AvroMessage> startConsumer(
            String topicName,
            String eventName,
            String clusterName,
            AcknowledgingMessageListener<AvroMessageKey, AvroMessage> messageListener) {

        ConcurrentMessageListenerContainer<AvroMessageKey, AvroMessage> container = getKafkaListenerContainerFactory(clusterName).createContainer(topicName);
        // Set a unique group ID per topic/event pair, in case the same topic is consumed by multiple listeners with
        // different events. Otherwise, they would share the topic offset and records would be distributed among listeners.
        container.getContainerProperties().setGroupId(appName + "_" + topicName + "_" + eventName);
        container.setupMessageListener(messageListener);
        container.start();

        containers.add(container);

        return container;
    }

    @SuppressWarnings("unchecked")
    private ConcurrentKafkaListenerContainerFactory<AvroMessageKey, AvroMessage> getKafkaListenerContainerFactory(String clusterName) {
        try {
            return (ConcurrentKafkaListenerContainerFactory<AvroMessageKey, AvroMessage>) beanFactory.getBean(jeapKafkaBeanNames.getListenerContainerFactoryBeanName(clusterName));
        } catch (NoSuchBeanDefinitionException exception) {
            log.error("No kafkaListenerContainerFactory found for cluster with name '{}'", clusterName);
            throw new IllegalStateException("No kafkaListenerContainerFactory found for cluster with name " + clusterName);
        }
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping all domain event listener containers...");
        containers.forEach(concurrentMessageListenerContainer -> concurrentMessageListenerContainer.stop(true));
    }
}
