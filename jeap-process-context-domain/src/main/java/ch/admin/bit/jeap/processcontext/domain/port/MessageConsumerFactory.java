package ch.admin.bit.jeap.processcontext.domain.port;

import ch.admin.bit.jeap.processcontext.domain.message.MessageReceiver;

public interface MessageConsumerFactory {

    void startConsumer(String topic, String messageName, String clusterName, MessageReceiver messageReceiver);
}
