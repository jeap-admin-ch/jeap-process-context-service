package ch.admin.bit.jeap.processcontext.adapter.kafka.message.consumer;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.spring.JeapKafkaBeanNames;
import ch.admin.bit.jeap.processcontext.adapter.kafka.message.ProcessContextKafkaConfiguration;
import ch.admin.bit.jeap.processcontext.adapter.kafka.message.filter.MessageFilterConfigurationException;
import ch.admin.bit.jeap.processcontext.domain.message.MessageReceiver;
import ch.admin.bit.jeap.processcontext.domain.port.MessageConsumerFactory;
import ch.admin.bit.jeap.processcontext.plugin.api.message.MessageFilter;
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

    private final ProcessContextKafkaConfiguration processContextKafkaConfiguration;

    public KafkaMessageConsumerFactory(@Value("${spring.application.name}") String appName, KafkaProperties kafkaProperties, BeanFactory beanFactory, ProcessContextKafkaConfiguration processContextKafkaConfiguration) {
        this.appName = appName;
        this.kafkaProperties = kafkaProperties;
        this.beanFactory = beanFactory;
        this.jeapKafkaBeanNames = new JeapKafkaBeanNames(kafkaProperties.getDefaultClusterName());
        this.processContextKafkaConfiguration = processContextKafkaConfiguration;
    }

    @Override
    public void startConsumer(String topicName, String messageName, String clusterName, MessageReceiver messageReceiver) {
        if (processContextKafkaConfiguration.isMessageConsumerPaused()) {
            log.info("Message consumer is paused via configuration, skipping consumer start for message '{}' on topic '{}'", messageName, topicName);
            return;
        }

        if (!StringUtils.hasText(clusterName)) {
            clusterName = kafkaProperties.getDefaultClusterName();
        }

        log.info("Starting message consumer for message '{}' on topic '{}' on cluster '{}'", messageName, topicName, clusterName);

        KafkaMessageListener listener = new KafkaMessageListener(messageName, messageReceiver, getMessageFilterInstanceForMessageName(messageName, processContextKafkaConfiguration));
        startConsumer(topicName, messageName, clusterName, listener);
    }

    private MessageFilter<AvroMessage> getMessageFilterInstanceForMessageName(String messageName, ProcessContextKafkaConfiguration processContextKafkaConfiguration) {
        String className = processContextKafkaConfiguration.getFilters().get(messageName);
        if (StringUtils.hasText(className)) {
            log.info("Found message filter for message type '{}': {}", messageName, className);
            return newMessageFilterInstance(className);
        }
        return null;
    }

    private <T extends MessageFilter<?>> T newMessageFilterInstance(String className) {
        try {
            @SuppressWarnings("unchecked")
            Class<T> messageFilterClass = (Class<T>) Class.forName(className);
            return messageFilterClass.getDeclaredConstructor().newInstance();
        } catch (ClassCastException | ReflectiveOperationException e) {
            throw new MessageFilterConfigurationException(className, e);
        }
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
