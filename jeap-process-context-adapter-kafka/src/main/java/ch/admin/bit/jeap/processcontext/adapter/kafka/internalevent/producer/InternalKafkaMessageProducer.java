package ch.admin.bit.jeap.processcontext.adapter.kafka.internalevent.producer;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.processcontext.adapter.kafka.TopicConfiguration;
import ch.admin.bit.jeap.processcontext.domain.port.InternalMessageProducer;
import ch.admin.bit.jeap.processcontext.domain.port.MetricsListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

@Component
@Slf4j
class InternalKafkaMessageProducer implements InternalMessageProducer {
    private final KafkaTemplate<SpecificRecord, AvroMessage> internalKafkaTemplate;
    private final TopicConfiguration topicConfiguration;
    private final InternalMessageFactory internalMessageFactory;
    private final MetricsListener metricsListener;

    public InternalKafkaMessageProducer(KafkaTemplate<SpecificRecord, AvroMessage> internalKafkaTemplate,
                                        TopicConfiguration topicConfiguration,
                                        InternalMessageFactory internalMessageFactory,
                                        MetricsListener metricsListener) {
        this.internalKafkaTemplate = internalKafkaTemplate;
        this.topicConfiguration = topicConfiguration;
        this.internalMessageFactory = internalMessageFactory;
        this.metricsListener = metricsListener;
    }

    @Override
    public void produceProcessContextOutdatedEventSynchronously(String originProcessId) {
        log.debug("Producing process outdated message for process ID {}", originProcessId);
        String topicName = topicConfiguration.getProcessOutdatedInternal();
        sendSynchronously(topicName, originProcessId, internalMessageFactory::processContextOutdatedEvent);
    }

    @Override
    public void produceProcessContextStateChangedEventSynchronously(String originProcessId) {
        log.debug("Producing process state changed message for process ID {}", originProcessId);
        String topicName = topicConfiguration.getProcessChangedInternal();
        metricsListener.timed("jeap_pcs_produce_process_context_state_changed_event", Collections.emptyMap(),
                () -> sendSynchronously(topicName, originProcessId, internalMessageFactory::processContextStateChangedEvent));
    }

    private void sendSynchronously(String topicName, String originProcessId, Function<String, AvroMessage> messageFactory) {
        try {
            SpecificRecord key = internalMessageFactory.key(originProcessId);
            AvroMessage event = messageFactory.apply(originProcessId);
            internalKafkaTemplate
                    .send(topicName, key, event)
                    .get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Cannot send event", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Cannot send event", e);
        }
    }
}
