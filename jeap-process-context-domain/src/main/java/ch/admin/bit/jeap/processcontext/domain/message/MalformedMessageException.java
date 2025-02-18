package ch.admin.bit.jeap.processcontext.domain.message;

import ch.admin.bit.jeap.messaging.model.Message;

import static java.lang.String.format;

class MalformedMessageException extends RuntimeException {

    private MalformedMessageException(String message) {
        super(message);
    }

    static MalformedMessageException missingPayload(Message message) {
        return new MalformedMessageException(format("Missing payload in message %s with id %s",
                message.getType().getName(), message.getIdentity().getId()));
    }

    static MalformedMessageException missingProcessId(Message message) {
        return new MalformedMessageException(format("Missing processId in message %s with id %s",
                message.getType().getName(), message.getIdentity().getId()));
    }
}
