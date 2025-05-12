package ch.admin.bit.jeap.processcontext.adapter.kafka.message.consumer;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEvent;
import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.processcontext.domain.message.MessageReceiver;
import ch.admin.bit.jeap.processcontext.event.ProcessInstanceCompletedEventBuilder;
import ch.admin.bit.jeap.processcontext.plugin.api.message.MessageFilter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaMessageListenerTest {

    @Mock
    private MessageReceiver messageReceiver;
    @Mock
    private ConsumerRecord<AvroMessageKey, AvroMessage> consumerRecord;
    @Mock
    private Acknowledgment acknowledgment;

    private final AvroDomainEvent event = ProcessInstanceCompletedEventBuilder.create()
            .serviceName("service").processId("id").systemName("sys").idempotenceId("idempotenceId").build();

    @Test
    void onMessage_whenEventNameMatches_shouldReceiveEvent() {
        KafkaMessageListener messageListener = new KafkaMessageListener(event.getType().getName(), messageReceiver, null);
        doReturn(event).when(consumerRecord).value();

        messageListener.onMessage(consumerRecord, acknowledgment);

        verify(messageReceiver).messageReceived(event);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void onMessage_whenEventNameDoesNotMatch_shouldNotReceiveEvent() {
        KafkaMessageListener messageListener = new KafkaMessageListener("otherEvent", messageReceiver, null);
        doReturn(event).when(consumerRecord).value();

        messageListener.onMessage(consumerRecord, acknowledgment);

        verifyNoInteractions(messageReceiver);
        verify(acknowledgment).acknowledge();
    }

    @Test
    @SuppressWarnings("unchecked")
    void onMessage_whenFilteredFalse_shouldNotReceiveEvent() {
        MessageFilter<AvroMessage> messageFilter = mock(MessageFilter.class);
        KafkaMessageListener messageListener = new KafkaMessageListener(event.getType().getName(), messageReceiver, messageFilter);
        when(messageFilter.filter(any())).thenReturn(false);
        doReturn(event).when(consumerRecord).value();

        messageListener.onMessage(consumerRecord, acknowledgment);

        verifyNoInteractions(messageReceiver);
        verify(acknowledgment).acknowledge();
    }

    @Test
    @SuppressWarnings("unchecked")
    void onMessage_whenFilteredTrue_shouldReceiveEvent() {
        MessageFilter<AvroMessage> messageFilter = mock(MessageFilter.class);
        KafkaMessageListener messageListener = new KafkaMessageListener(event.getType().getName(), messageReceiver, messageFilter);
        when(messageFilter.filter(any())).thenReturn(true);
        doReturn(event).when(consumerRecord).value();

        messageListener.onMessage(consumerRecord, acknowledgment);

        verify(messageReceiver).messageReceived(event);
        verify(acknowledgment).acknowledge();
    }
}
