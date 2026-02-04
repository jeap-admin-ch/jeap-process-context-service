package ch.admin.bit.jeap.processcontext.adapter.jpa;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Projection for query results containing a process instance ID and its last message creation timestamp.
 */
interface ProcessInstanceLastMessageProjection {
    UUID getProcessInstanceId();

    ZonedDateTime getLastMessageCreatedAt();
}
