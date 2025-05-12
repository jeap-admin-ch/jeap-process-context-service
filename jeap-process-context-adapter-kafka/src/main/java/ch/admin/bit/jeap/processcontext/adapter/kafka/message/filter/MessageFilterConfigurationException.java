package ch.admin.bit.jeap.processcontext.adapter.kafka.message.filter;

public class MessageFilterConfigurationException extends RuntimeException {

    public MessageFilterConfigurationException(String className, Throwable cause) {
        super("Error while creating MessageFilter instance of " + className, cause);
    }
}
