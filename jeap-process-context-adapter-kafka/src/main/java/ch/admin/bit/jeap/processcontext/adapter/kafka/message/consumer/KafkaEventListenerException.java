package ch.admin.bit.jeap.processcontext.adapter.kafka.message.consumer;

import ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerException;
import ch.admin.bit.jeap.processcontext.domain.message.NotFoundException;
import lombok.Getter;

@Getter
public class KafkaEventListenerException extends MessageHandlerException {

    private KafkaEventListenerException(String errorCode, String description, Temporality temporality, Throwable cause) {
        super(temporality, cause, null, errorCode, description);
    }

    public static KafkaEventListenerException from(Exception e) {
        if (e instanceof NotFoundException) {
            return new KafkaEventListenerException("PROCESS_NOT_FOUND",
                    "Process not found",
                    Temporality.TEMPORARY, e
            );
        }
        return new KafkaEventListenerException("ERROR_CONSUMING_EVENT",
                "Error while consuming event",
                Temporality.PERMANENT, e
        );
    }
}
