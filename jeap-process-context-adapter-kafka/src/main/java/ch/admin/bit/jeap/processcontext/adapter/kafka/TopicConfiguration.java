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
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Configuration
@ConfigurationProperties(prefix = "jeap.processcontext.kafka.topic")
@Data
@Slf4j
public class TopicConfiguration {
    public static final String PROCESS_OUTDATED_TOPIC_NAME = "${jeap.processcontext.kafka.topic.process-outdated-internal}";
    private static final String EVENT_PROCESSING_FAILED_TOPIC_NAME = "${jeap.messaging.kafka.error-topic-name}";
    public static final String PROCESS_SNAPSHOT_CREATED_EVENT_TOPIC_NAME = "${jeap.processcontext.kafka.topic.process-snapshot-created}";

    /**
     * Name of the topic for the internal process outdated events
     */
    private String processOutdatedInternal;

    /**
     * Name of the topic where process snapshot created events will be published
     */
    private String processSnapshotCreated;

    @Configuration
    @Profile("!local")
    @RequiredArgsConstructor
    @Slf4j
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
                        topicConfiguration.getProcessOutdatedInternal(),
                        eventProcessingFailedTopicName));
                if (processTemplateRepository.hasProcessSnapshotsConfigured()) {
                    // The notification of a snapshot creation requires the following topic
                    topicNames.add(topicConfiguration.getProcessSnapshotCreated());
                }
                for (String topicName : topicNames) {
                    try {
                        adminClient.describeTopics(Set.of(topicName)).allTopicNames().get();
                    } catch (Exception e) {
                        log.error("Topic {} does not exist, please check your configuration or create the topic", topicName);
                        throw e;
                    }
                }
            }
        }
    }
}
