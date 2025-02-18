package ch.admin.bit.jeap.processcontext.adapter.jpa;


import java.time.ZonedDateTime;
import java.util.UUID;

interface MessageReferenceMessage {
    UUID getMessageReferenceId();

    UUID getMessageId();

    String getMessageName();

    ZonedDateTime getMessageReceivedAt();

    String getTraceId();
}
