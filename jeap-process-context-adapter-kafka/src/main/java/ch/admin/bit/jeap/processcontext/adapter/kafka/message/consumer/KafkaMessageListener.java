package ch.admin.bit.jeap.processcontext.adapter.kafka.message.consumer;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.processcontext.plugin.api.message.MessageFilter;
import ch.admin.bit.jeap.processcontext.domain.message.MessageReceiver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;

@RequiredArgsConstructor
@Slf4j
class KafkaMessageListener implements AcknowledgingMessageListener<AvroMessageKey, AvroMessage> {

    private final String eventName;
    private final MessageReceiver messageReceiver;
    private final MessageFilter<AvroMessage> messageFilter;

    @Override
    public void onMessage(ConsumerRecord<AvroMessageKey, AvroMessage> record, Acknowledgment acknowledgment) {
        try {
            handleMessage(record.value(), record.topic());
        } catch (Exception ex) {
            throw KafkaEventListenerException.from(ex);
        }
        acknowledgment.acknowledge();
    }

    private void handleMessage(AvroMessage message, String topic) {
        String receivedEventName = message.getType().getName();

        if (!eventName.equals(receivedEventName)) {
            logIgnoredMessage(receivedEventName, topic);
            return;
        }

        if (messageFilter == null || messageFilter.filter(message)) {
            messageReceiver.messageReceived(message);
        } else {
            log.trace("Message '{}' filtered out by the configured message filter and will be ignored", receivedEventName);
        }
    }

    private void logIgnoredMessage(String eventName, String topic) {
        log.debug("Ignoring event '{}' on topic '{}' as there is no matching message reference in the template/correlation provider",
                eventName, topic);
    }
}
