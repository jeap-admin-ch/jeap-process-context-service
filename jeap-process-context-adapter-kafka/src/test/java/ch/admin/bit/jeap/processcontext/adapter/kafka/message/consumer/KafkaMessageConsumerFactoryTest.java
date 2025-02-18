package ch.admin.bit.jeap.processcontext.adapter.kafka.message.consumer;

import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaMessageConsumerFactoryTest {

    @Mock
    private BeanFactory beanFactory;

    @Mock
    private KafkaProperties kafkaProperties;

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

        KafkaMessageConsumerFactory factory = new KafkaMessageConsumerFactory("appName", kafkaProperties, beanFactory);

        assertDoesNotThrow(() -> factory.startConsumer("topicName", "messageName", null, mock(MessageReceiver.class)));
    }

    @Test
    void startConsumer_withDefinedCluster() {
        var mockedContainerFactory = mock(ConcurrentKafkaListenerContainerFactory.class);
        var mockedContainer = mock(ConcurrentMessageListenerContainer.class);
        when(mockedContainerFactory.createContainer(anyString())).thenReturn(mockedContainer);
        when(mockedContainer.getContainerProperties()).thenReturn(mock(ContainerProperties.class));
        when(beanFactory.getBean("clusterAKafkaListenerContainerFactory")).thenReturn(mockedContainerFactory);

        KafkaMessageConsumerFactory factory = new KafkaMessageConsumerFactory("appName", kafkaProperties, beanFactory);

        assertDoesNotThrow(() -> factory.startConsumer("topicName", "messageName", "clusterA", mock(MessageReceiver.class)));
    }

    @Test
    void startConsumer_withUndefinedCluster_throwsException() {
        KafkaMessageConsumerFactory factory = new KafkaMessageConsumerFactory("appName", kafkaProperties, beanFactory);
        when(beanFactory.getBean(anyString())).thenThrow(new NoSuchBeanDefinitionException("name"));

        assertThrows(IllegalStateException.class, () -> factory.startConsumer("topicName", "messageName", "clusterNotDefined", mock(MessageReceiver.class)));
    }

}
