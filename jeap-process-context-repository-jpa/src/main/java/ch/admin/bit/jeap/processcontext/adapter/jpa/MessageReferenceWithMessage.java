package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.processinstance.MessageReference;

/**
 * DTO for query results containing a MessageReference with its associated Message and process template name.
 */
record MessageReferenceWithMessage(
        MessageReference messageReference,
        Message message,
        String processTemplateName
) {
}
