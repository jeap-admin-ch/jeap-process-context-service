package ch.admin.bit.jeap.processcontext.adapter.jpa;


import java.util.UUID;

interface MessageReferenceRelatedTaskId {
    UUID getMessageReferenceId();
    String getRelatedOriginTaskId();
}
