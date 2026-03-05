package ch.admin.bit.jeap.processcontext.adapter.kafka.internalevent.producer;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.processcontext.adapter.kafka.KafkaProducerException;
import ch.admin.bit.jeap.processcontext.adapter.kafka.TopicConfiguration;
import ch.admin.bit.jeap.processcontext.domain.port.InternalMessageProducer;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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
    public void produceProcessContextOutdatedCreateProcessEventSynchronously(String originProcessId, UUID messageId,
                                                                             String messageName, String idempotenceId, String templateName) {
        AvroMessage message = internalMessageFactory
                .processContextOutdatedCreateProcessEvent(originProcessId, messageId, messageName, idempotenceId, templateName);
        produceMessage(originProcessId, message);
    }

    @Override
    public void produceProcessContextOutdatedEventSynchronously(String originProcessId, UUID messageId, String messageName, String idempotenceId) {
        AvroMessage message = internalMessageFactory.processContextOutdatedEvent(originProcessId, messageId, messageName, idempotenceId);
        produceMessage(originProcessId, message);
    }

    private void produceMessage(String originProcessId, AvroMessage message) {
        log.debug("Producing process outdated message for process ID {}", originProcessId);
        String topicName = topicConfiguration.getProcessOutdatedInternal();
        sendSynchronously(topicName, originProcessId, message);
    }

    private void sendSynchronously(String topicName, String originProcessId, AvroMessage message) {
        try {
            SpecificRecord key = internalMessageFactory.key(originProcessId);
            internalKafkaTemplate
                    .send(topicName, key, message)
                    .get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaProducerException("Cannot send event", e);
        } catch (ExecutionException e) {
            throw new KafkaProducerException("Cannot send event", e);
        }
    }

    @Override
    public void produceProcessContextOutdatedMigrationTriggerEvents(List<String> originProcessIds, String idempotenceId) {
        String topicName = topicConfiguration.getProcessOutdatedInternal();
        List<CompletableFuture<SendResult<SpecificRecord, AvroMessage>>> futures = new ArrayList<>(originProcessIds.size());
        for (String originProcessId : originProcessIds) {
            var future = sendAsync(idempotenceId, originProcessId, topicName);
            futures.add(future);
        }
        try {
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaProducerException("Cannot send migration trigger events", e);
        } catch (ExecutionException e) {
            throw new KafkaProducerException("Cannot send migration trigger events", e);
        }
    }

    private CompletableFuture<SendResult<SpecificRecord, AvroMessage>> sendAsync(String idempotenceId, String originProcessId, String topicName) {
        log.debug("Producing migration trigger message for process ID {}", originProcessId);
        var message = internalMessageFactory.processContextOutdatedMigrationTriggerEvent(originProcessId, idempotenceId);
        var key = internalMessageFactory.key(originProcessId);
        return internalKafkaTemplate.send(topicName, key, message);
    }

}
