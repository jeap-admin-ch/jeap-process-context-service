package ch.admin.bit.jeap.processcontext.adapter.kafka;

import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaAdmin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@SuppressWarnings("resource")
@ExtendWith(MockitoExtension.class)
class TopicConfigurationCloudTest {

    private static final String PROCESS_OUTDATED_TOPIC = "process-outdated-internal";
    private static final String EVENT_PROCESSING_FAILED_TOPIC = "event-processing-failed";
    private static final String PROCESS_SNAPSHOT_CREATED_TOPIC = "process-snapshot-created";

    @Mock
    private KafkaAdmin kafkaAdmin;

    @Mock
    private TopicConfiguration topicConfiguration;

    @Mock
    private ProcessTemplateRepository processTemplateRepository;

    @Mock
    private AdminClient adminClient;

    @Mock
    private DescribeTopicsResult describeTopicsResult;

    @Mock
    private KafkaFuture<Map<String, TopicDescription>> allTopicNamesFuture;

    private Object topicConfigurationCloud;

    @BeforeEach
    void setUp() throws Exception {
        Class<?> innerClass = Class.forName("ch.admin.bit.jeap.processcontext.adapter.kafka.TopicConfiguration$TopicConfigurationCloud");
        Constructor<?> constructor = innerClass.getDeclaredConstructor(KafkaAdmin.class, TopicConfiguration.class, ProcessTemplateRepository.class);
        constructor.setAccessible(true);
        topicConfigurationCloud = constructor.newInstance(kafkaAdmin, topicConfiguration, processTemplateRepository);

        Field eventProcessingFailedTopicNameField = innerClass.getDeclaredField("eventProcessingFailedTopicName");
        eventProcessingFailedTopicNameField.setAccessible(true);
        eventProcessingFailedTopicNameField.set(topicConfigurationCloud, EVENT_PROCESSING_FAILED_TOPIC);
    }

    @Test
    void checkIfTopicsExist_whenAllTopicsExist_shouldSucceed() throws Exception {
        when(topicConfiguration.getProcessOutdatedInternal()).thenReturn(PROCESS_OUTDATED_TOPIC);
        when(processTemplateRepository.hasProcessSnapshotsConfigured()).thenReturn(false);
        when(kafkaAdmin.getConfigurationProperties()).thenReturn(Map.of());
        when(describeTopicsResult.allTopicNames()).thenReturn(allTopicNamesFuture);
        when(allTopicNamesFuture.get()).thenReturn(Map.of());

        try (MockedStatic<AdminClient> adminClientMockedStatic = mockStatic(AdminClient.class)) {
            adminClientMockedStatic.when(() -> AdminClient.create(anyMap())).thenReturn(adminClient);
            when(adminClient.describeTopics(anyCollection())).thenReturn(describeTopicsResult);

            assertDoesNotThrow(this::invokeCheckIfTopicsExist);

            verify(adminClient, times(2)).describeTopics(anyCollection());
        }
    }

    @Test
    void checkIfTopicsExist_whenProcessSnapshotsConfigured_shouldCheckSnapshotTopic() throws Exception {
        when(topicConfiguration.getProcessOutdatedInternal()).thenReturn(PROCESS_OUTDATED_TOPIC);
        when(topicConfiguration.getProcessSnapshotCreated()).thenReturn(PROCESS_SNAPSHOT_CREATED_TOPIC);
        when(processTemplateRepository.hasProcessSnapshotsConfigured()).thenReturn(true);
        when(kafkaAdmin.getConfigurationProperties()).thenReturn(Map.of());
        when(describeTopicsResult.allTopicNames()).thenReturn(allTopicNamesFuture);
        when(allTopicNamesFuture.get()).thenReturn(Map.of());

        try (MockedStatic<AdminClient> adminClientMockedStatic = mockStatic(AdminClient.class)) {
            adminClientMockedStatic.when(() -> AdminClient.create(anyMap())).thenReturn(adminClient);
            when(adminClient.describeTopics(anyCollection())).thenReturn(describeTopicsResult);

            assertDoesNotThrow(this::invokeCheckIfTopicsExist);

            verify(adminClient, times(3)).describeTopics(anyCollection());
            verify(topicConfiguration).getProcessSnapshotCreated();
        }
    }

    @Test
    void checkIfTopicsExist_whenTopicDoesNotExist_shouldThrowException() throws Exception {
        when(topicConfiguration.getProcessOutdatedInternal()).thenReturn(PROCESS_OUTDATED_TOPIC);
        when(processTemplateRepository.hasProcessSnapshotsConfigured()).thenReturn(false);
        when(kafkaAdmin.getConfigurationProperties()).thenReturn(Map.of());
        when(describeTopicsResult.allTopicNames()).thenReturn(allTopicNamesFuture);
        when(allTopicNamesFuture.get()).thenThrow(new ExecutionException("Topic does not exist", new RuntimeException()));

        try (MockedStatic<AdminClient> adminClientMockedStatic = mockStatic(AdminClient.class)) {
            adminClientMockedStatic.when(() -> AdminClient.create(anyMap())).thenReturn(adminClient);
            when(adminClient.describeTopics(anyCollection())).thenReturn(describeTopicsResult);

            assertThatThrownBy(this::invokeCheckIfTopicsExist)
                    .isInstanceOf(ExecutionException.class)
                    .hasMessageContaining("Topic does not exist");
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void checkIfTopicsExist_whenSnapshotTopicDoesNotExist_shouldThrowException() throws Exception {
        when(topicConfiguration.getProcessOutdatedInternal()).thenReturn(PROCESS_OUTDATED_TOPIC);
        when(topicConfiguration.getProcessSnapshotCreated()).thenReturn(PROCESS_SNAPSHOT_CREATED_TOPIC);
        when(processTemplateRepository.hasProcessSnapshotsConfigured()).thenReturn(true);
        when(kafkaAdmin.getConfigurationProperties()).thenReturn(Map.of());

        KafkaFuture<Map<String, TopicDescription>> successFuture = mock(KafkaFuture.class);
        when(successFuture.get()).thenReturn(Map.of());

        KafkaFuture<Map<String, TopicDescription>> failureFuture = mock(KafkaFuture.class);
        when(failureFuture.get()).thenThrow(new ExecutionException("Snapshot topic does not exist", new RuntimeException()));

        DescribeTopicsResult successResult = mock(DescribeTopicsResult.class);
        when(successResult.allTopicNames()).thenReturn(successFuture);

        DescribeTopicsResult failureResult = mock(DescribeTopicsResult.class);
        when(failureResult.allTopicNames()).thenReturn(failureFuture);

        try (MockedStatic<AdminClient> adminClientMockedStatic = mockStatic(AdminClient.class)) {
            adminClientMockedStatic.when(() -> AdminClient.create(anyMap())).thenReturn(adminClient);
            when(adminClient.describeTopics(anyCollection()))
                    .thenReturn(successResult)
                    .thenReturn(successResult)
                    .thenReturn(failureResult);

            assertThatThrownBy(this::invokeCheckIfTopicsExist)
                    .isInstanceOf(ExecutionException.class)
                    .hasMessageContaining("Snapshot topic does not exist");
        }
    }

    private void invokeCheckIfTopicsExist() throws Exception {
        Method method = topicConfigurationCloud.getClass().getDeclaredMethod("checkIfTopicsExist");
        method.setAccessible(true);
        try {
            method.invoke(topicConfigurationCloud);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw e;
        }
    }
}
