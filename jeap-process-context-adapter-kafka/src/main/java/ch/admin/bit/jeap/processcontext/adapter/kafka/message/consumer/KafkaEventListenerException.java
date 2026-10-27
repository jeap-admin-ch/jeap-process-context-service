package ch.admin.bit.jeap.processcontext.adapter.kafka.message.consumer;

import ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerException;
import lombok.Getter;

@Getter
public class KafkaEventListenerException extends MessageHandlerException {

    private KafkaEventListenerException(String errorCode, String description, Temporality temporality, Throwable cause) {
        super(temporality, cause, null, errorCode, description);
    }

    // The temporality is resolved by jEAP messaging; unresolved failures are handled as permanent
    public static KafkaEventListenerException from(Exception e) {
        return new KafkaEventListenerException("ERROR_CONSUMING_EVENT",
                "Error while consuming event",
                Temporality.UNKNOWN, e
        );
    }
}
