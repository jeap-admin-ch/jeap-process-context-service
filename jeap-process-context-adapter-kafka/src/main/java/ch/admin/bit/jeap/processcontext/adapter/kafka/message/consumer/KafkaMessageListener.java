package ch.admin.bit.jeap.processcontext.adapter.kafka.message.consumer;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
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

    @Override
    public void onMessage(ConsumerRecord<AvroMessageKey, AvroMessage> record, Acknowledgment acknowledgment) {
        try {
            handleMessage(record);
        } catch (Exception ex) {
            throw KafkaEventListenerException.from(ex);
        }
        acknowledgment.acknowledge();
    }

    private void handleMessage(ConsumerRecord<AvroMessageKey, AvroMessage> record) {
        String receivedEventName = record.value().getType().getName();
        if (eventName.equals(receivedEventName)) {
            messageReceiver.messageReceived(record.value());
        } else {
            logIgnoredMessage(record);
        }
    }

    private void logIgnoredMessage(ConsumerRecord<AvroMessageKey, AvroMessage> record) {
        log.debug("Ignoring event {} on topic {} as there is no matching message reference in the template / correlation provider",
                record.value().getType().getName(), record.topic());
    }
}
