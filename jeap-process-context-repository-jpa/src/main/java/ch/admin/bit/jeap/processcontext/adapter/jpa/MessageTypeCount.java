package ch.admin.bit.jeap.processcontext.adapter.jpa;

/**
 * Projection interface for counting messages by type.
 */
interface MessageTypeCount {
    String getMessageName();

    Long getMessageCount();
}
