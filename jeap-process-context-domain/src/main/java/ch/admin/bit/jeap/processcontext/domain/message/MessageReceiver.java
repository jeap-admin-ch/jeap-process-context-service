package ch.admin.bit.jeap.processcontext.domain.message;

import ch.admin.bit.jeap.messaging.model.Message;

public interface MessageReceiver {

    void messageReceived(Message message);

}
