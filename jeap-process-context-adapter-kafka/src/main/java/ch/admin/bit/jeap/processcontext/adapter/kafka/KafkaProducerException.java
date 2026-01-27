package ch.admin.bit.jeap.processcontext.adapter.kafka;

public class KafkaProducerException extends RuntimeException {
    public KafkaProducerException(String message, Throwable cause) {
        super(message, cause);
    }
}
