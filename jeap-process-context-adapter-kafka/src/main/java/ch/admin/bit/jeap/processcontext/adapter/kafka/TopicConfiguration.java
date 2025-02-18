package ch.admin.bit.jeap.processcontext.adapter.kafka;

import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Configuration
@ConfigurationProperties(prefix = "jeap.processcontext.kafka.topic")
@Data
@Slf4j
public class TopicConfiguration {
    public static final String PROCESS_INSTANCE_CREATED_EVENT_TOPIC_NAME = "${jeap.processcontext.kafka.topic.process-instance-created}";
    public static final String PROCESS_INSTANCE_COMPLETED_EVENT_TOPIC_NAME = "${jeap.processcontext.kafka.topic.process-instance-completed}";
    public static final String PROCESS_MILESTONE_REACHED_EVENT_TOPIC_NAME = "${jeap.processcontext.kafka.topic.process-milestone-reached}";
    public static final String PROCESS_OUTDATED_TOPIC_NAME = "${jeap.processcontext.kafka.topic.process-outdated-internal}";
    public static final String PROCESS_STATE_CHANGED_TOPIC_NAME = "${jeap.processcontext.kafka.topic.process-changed-internal}";
    public static final String CREATE_PROCESS_INSTANCE_TOPIC_NAME = "${jeap.processcontext.kafka.topic.create-process-instance}";
    private static final String EVENT_PROCESSING_FAILED_TOPIC_NAME = "${jeap.messaging.kafka.error-topic-name}";
    public static final String PROCESS_SNAPSHOT_CREATED_EVENT_TOPIC_NAME = "${jeap.processcontext.kafka.topic.process-snapshot-created}";


    /**
     * Name of the topic for the internal process outdated events
     */
    private String processOutdatedInternal;

    /**
     * Name of topic for the internal instance changed events
     */
    private String processChangedInternal;

    /**
     * Name of topic where process instance created events will be published
     */
    private String processInstanceCreated;

    /**
     * Name of topic where process instance completed events will be published
     */
    private String processInstanceCompleted;

    /**
     * Name of topic where process milestone reached events will be published
     */
    private String processMilestoneReached;

    /**
     * Name of topic where create process instance commands will be published
     */
    private String createProcessInstance;

    /**
     * Name of the topic where process snapshot created events will be published
     */
    private String processSnapshotCreated;

    @Configuration
    @Profile("!local")
    @RequiredArgsConstructor
    @SuppressWarnings({"unused", "java:S3985"}) // Class is used as spring configuration even if not public
    private static class TopicConfigurationCloud {

        private final KafkaAdmin kafkaAdmin;
        private final TopicConfiguration topicConfiguration;
        private final ProcessTemplateRepository processTemplateRepository;

        @Value(EVENT_PROCESSING_FAILED_TOPIC_NAME)
        private String eventProcessingFailedTopicName;

        @PostConstruct
        @SuppressWarnings("findbugs:RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE") // redundant admin client null check
        public void checkIfTopicsExist() throws ExecutionException, InterruptedException {
            try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
                List<String> topicNames = new ArrayList<>(List.of(
                        topicConfiguration.getProcessChangedInternal(),
                        topicConfiguration.getProcessOutdatedInternal(),
                        topicConfiguration.getProcessInstanceCreated(),
                        topicConfiguration.getProcessInstanceCompleted(),
                        topicConfiguration.getProcessMilestoneReached(),
                        topicConfiguration.getCreateProcessInstance(),
                        eventProcessingFailedTopicName));
                if (processTemplateRepository.hasProcessSnapshotsConfigured()) {
                    // The notification of a snapshot creation requires the following topic
                    topicNames.add(topicConfiguration.getProcessSnapshotCreated());
                }
                adminClient.describeTopics(topicNames).allTopicNames().get();
            }
        }
    }

    @Configuration
    @Profile("local")
    @RequiredArgsConstructor
    @SuppressWarnings({"unused", "java:S3985"}) // Class is used as spring configuration even if not public
    private static class TopicConfigurationLocal {
        private final TopicConfiguration topicConfiguration;

        @Value(EVENT_PROCESSING_FAILED_TOPIC_NAME)
        private String eventProcessingFailedTopicName;

        @Bean
        public NewTopic processChangedInternalTopic() {
            return new NewTopic(topicConfiguration.getProcessChangedInternal(), 1, (short) 1);
        }

        @Bean
        public NewTopic processOutdatedInternalTopic() {
            return new NewTopic(topicConfiguration.getProcessOutdatedInternal(), 1, (short) 1);
        }

        @Bean
        public NewTopic processInstanceCreatedTopic() {
            return new NewTopic(topicConfiguration.getProcessInstanceCreated(), 1, (short) 1);
        }

        @Bean
        public NewTopic processInstanceCompletedTopic() {
            return new NewTopic(topicConfiguration.getProcessInstanceCompleted(), 1, (short) 1);
        }

        @Bean
        public NewTopic processMilestoneReachedTopic() {
            return new NewTopic(topicConfiguration.getProcessMilestoneReached(), 1, (short) 1);
        }

        @Bean
        public NewTopic eventProcessingFailedTopic() {
            return new NewTopic(eventProcessingFailedTopicName, 1, (short) 1);
        }

        @Bean
        public NewTopic createProcessInstance() {
            return new NewTopic(topicConfiguration.getCreateProcessInstance(), 1, (short) 1);
        }

        @Bean
        public NewTopic processSnapshotCreatedTopic() {
            return new NewTopic(topicConfiguration.getProcessSnapshotCreated(), 1, (short) 1);
        }
    }
}
