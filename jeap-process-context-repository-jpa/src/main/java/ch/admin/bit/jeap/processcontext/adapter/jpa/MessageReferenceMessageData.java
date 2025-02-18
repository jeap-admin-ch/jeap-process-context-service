package ch.admin.bit.jeap.processcontext.adapter.jpa;


import java.util.UUID;

interface MessageReferenceMessageData {
    UUID getMessageReferenceId();

    String getMessageDataKey();

    String getMessageDataValue();

    String getMessageDataRole();
}
