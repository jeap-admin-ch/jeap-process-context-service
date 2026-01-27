package ch.admin.bit.jeap.processcontext.adapter.kafka.internalevent.producer;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.processcontext.adapter.kafka.KafkaProducerException;
import ch.admin.bit.jeap.processcontext.adapter.kafka.TopicConfiguration;
import ch.admin.bit.jeap.processcontext.domain.port.InternalMessageProducer;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.function.Function;

@Component
@Slf4j
class InternalKafkaMessageProducer implements InternalMessageProducer {
    private final KafkaTemplate<SpecificRecord, AvroMessage> internalKafkaTemplate;
    private final TopicConfiguration topicConfiguration;
    private final InternalMessageFactory internalMessageFactory;

    public InternalKafkaMessageProducer(KafkaTemplate<SpecificRecord, AvroMessage> internalKafkaTemplate,
                                        TopicConfiguration topicConfiguration,
                                        InternalMessageFactory internalMessageFactory) {
        this.internalKafkaTemplate = internalKafkaTemplate;
        this.topicConfiguration = topicConfiguration;
        this.internalMessageFactory = internalMessageFactory;
    }

    @Override
    public void produceProcessContextOutdatedEventSynchronously(String originProcessId) {
        log.debug("Producing process outdated message for process ID {}", originProcessId);
        String topicName = topicConfiguration.getProcessOutdatedInternal();
        sendSynchronously(topicName, originProcessId, internalMessageFactory::processContextOutdatedEvent);
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
            throw new KafkaProducerException("Cannot send event", e);
        } catch (ExecutionException e) {
            throw new KafkaProducerException("Cannot send event", e);
        }
    }
}
