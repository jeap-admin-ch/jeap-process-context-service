package ch.admin.bit.jeap.processcontext.adapter.kafka.message.consumer;

import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.processcontext.adapter.kafka.message.filter.MessageFilterConfigurationException;
import ch.admin.bit.jeap.processcontext.adapter.kafka.message.filter.MessageFiltersConfiguration;
import ch.admin.bit.jeap.processcontext.domain.message.MessageReceiver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaMessageConsumerFactoryTest {

    @Mock
    private BeanFactory beanFactory;

    @Mock
    private KafkaProperties kafkaProperties;

    @Mock
    private MessageFiltersConfiguration messageFiltersConfiguration;

    @BeforeEach
    void setup(){
        when(kafkaProperties.getDefaultClusterName()).thenReturn(KafkaProperties.DEFAULT_CLUSTER);
    }

    @Test
    void startConsumer_withDefaultCluster() {
        var mockedContainerFactory = mock(ConcurrentKafkaListenerContainerFactory.class);
        var mockedContainer = mock(ConcurrentMessageListenerContainer.class);
        when(mockedContainerFactory.createContainer(anyString())).thenReturn(mockedContainer);
        when(mockedContainer.getContainerProperties()).thenReturn(mock(ContainerProperties.class));
        when(beanFactory.getBean("kafkaListenerContainerFactory")).thenReturn(mockedContainerFactory);

        KafkaMessageConsumerFactory factory = new KafkaMessageConsumerFactory("appName", kafkaProperties, beanFactory, messageFiltersConfiguration);

        assertDoesNotThrow(() -> factory.startConsumer("topicName", "messageName", null, mock(MessageReceiver.class)));
    }

    @Test
    void startConsumer_withDefinedCluster() {
        var mockedContainerFactory = mock(ConcurrentKafkaListenerContainerFactory.class);
        var mockedContainer = mock(ConcurrentMessageListenerContainer.class);
        when(mockedContainerFactory.createContainer(anyString())).thenReturn(mockedContainer);
        when(mockedContainer.getContainerProperties()).thenReturn(mock(ContainerProperties.class));
        when(beanFactory.getBean("clusterAKafkaListenerContainerFactory")).thenReturn(mockedContainerFactory);

        KafkaMessageConsumerFactory factory = new KafkaMessageConsumerFactory("appName", kafkaProperties, beanFactory, messageFiltersConfiguration);

        assertDoesNotThrow(() -> factory.startConsumer("topicName", "messageName", "clusterA", mock(MessageReceiver.class)));
    }

    @Test
    void startConsumer_withUndefinedCluster_throwsException() {
        KafkaMessageConsumerFactory factory = new KafkaMessageConsumerFactory("appName", kafkaProperties, beanFactory, messageFiltersConfiguration);
        when(beanFactory.getBean(anyString())).thenThrow(new NoSuchBeanDefinitionException("name"));

        assertThrows(IllegalStateException.class, () -> factory.startConsumer("topicName", "messageName", "clusterNotDefined", mock(MessageReceiver.class)));
    }

    @Test
    void startConsumer_withMessageFilterClassNotFound_throwsException() {
        Map<String, String> messageFilters = Map.of("messageName", "foo.bar");
        when(messageFiltersConfiguration.getFilters()).thenReturn(messageFilters);

        KafkaMessageConsumerFactory factory = new KafkaMessageConsumerFactory("appName", kafkaProperties, beanFactory, messageFiltersConfiguration);

        MessageFilterConfigurationException exception = assertThrows(MessageFilterConfigurationException.class, () -> factory.startConsumer("topicName", "messageName", null, mock(MessageReceiver.class)));
        assertThat(exception.getMessage()).isEqualTo("Error while creating MessageFilter instance of foo.bar");
        assertThat(exception.getCause()).isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void startConsumer_withDefaultClusterAndMessageFilter() {
        var mockedContainerFactory = mock(ConcurrentKafkaListenerContainerFactory.class);
        var mockedContainer = mock(ConcurrentMessageListenerContainer.class);
        when(mockedContainerFactory.createContainer(anyString())).thenReturn(mockedContainer);
        when(mockedContainer.getContainerProperties()).thenReturn(mock(ContainerProperties.class));
        when(beanFactory.getBean("kafkaListenerContainerFactory")).thenReturn(mockedContainerFactory);
        Map<String, String> messageFilters = Map.of("messageName", "ch.admin.bit.jeap.processcontext.adapter.kafka.message.filter.TestMessageFilter");
        when(messageFiltersConfiguration.getFilters()).thenReturn(messageFilters);

        KafkaMessageConsumerFactory factory = new KafkaMessageConsumerFactory("appName", kafkaProperties, beanFactory, messageFiltersConfiguration);

        assertDoesNotThrow(() -> factory.startConsumer("topicName", "messageName", null, mock(MessageReceiver.class)));
    }

    @Test
    void startConsumer_withMessageFilterDoesntImplementInterface_throwsException() {
        Map<String, String> messageFilters = Map.of("messageName", "ch.admin.bit.jeap.processcontext.adapter.kafka.message.filter.TestMessageFilterWithoutImplements");
        when(messageFiltersConfiguration.getFilters()).thenReturn(messageFilters);

        KafkaMessageConsumerFactory factory = new KafkaMessageConsumerFactory("appName", kafkaProperties, beanFactory, messageFiltersConfiguration);

        MessageFilterConfigurationException exception = assertThrows(MessageFilterConfigurationException.class, () -> factory.startConsumer("topicName", "messageName", null, mock(MessageReceiver.class)));
        assertThat(exception.getMessage()).isEqualTo("Error while creating MessageFilter instance of ch.admin.bit.jeap.processcontext.adapter.kafka.message.filter.TestMessageFilterWithoutImplements");

        assertThat(exception.getCause()).isInstanceOf(ClassCastException.class);
    }

}
