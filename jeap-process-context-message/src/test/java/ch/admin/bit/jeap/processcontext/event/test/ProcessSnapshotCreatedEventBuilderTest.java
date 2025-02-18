package ch.admin.bit.jeap.processcontext.event.test;

import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bit.jeap.processcontext.event.ProcessSnapshotCreatedEventBuilder;
import ch.admin.bit.jeap.processcontext.event.process.snapshot.created.ProcessSnapshotCreatedEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProcessSnapshotCreatedEventBuilderTest {

    private static final String SYSTEM_NAME = "test-system-name";
    private static final String SERVICE_NAME = "test-service-name";
    private static final String PROCESS_ID = "test-process-id";
    private static final String IDEMPOTENCE_ID = "test-idempotence-id";
    private static final int SNAPSHOT_VERSION = 2;

    @Test
    void testBuild() {
        ProcessSnapshotCreatedEvent event = ProcessSnapshotCreatedEventBuilder.create().
                idempotenceId(IDEMPOTENCE_ID).
                systemName(SYSTEM_NAME).
                serviceName(SERVICE_NAME).
                processId(PROCESS_ID).
                snapshotVersion(SNAPSHOT_VERSION).
                build();

        assertNotNull(event.getIdentity().getEventId());
        assertEquals(IDEMPOTENCE_ID, event.getIdentity().getIdempotenceId());
        assertEquals(SYSTEM_NAME, event.getPublisher().getSystem());
        assertEquals(SERVICE_NAME, event.getPublisher().getService());
        assertEquals(PROCESS_ID, event.getProcessId());
        assertEquals(SNAPSHOT_VERSION, event.getReferences().getReference().getSnapshotVersion());
        assertEquals(PROCESS_ID, event.getReferences().getReference().getProcessId());
        assertEquals("processId", event.getReferences().getReference().getType());
    }

    @Test
    void testBuild_whenProcessIdMissing_thenThrowsAvroMessageBuilderException() {
        Throwable t = assertThrows(AvroMessageBuilderException.class, () ->
                ProcessSnapshotCreatedEventBuilder.create().build());
        assertTrue(t.getMessage().contains("processId"));
    }

    @Test
    void testBuild_whenSnapshotVersionMissing_thenThrowsAvroMessageBuilderException() {
        Throwable t = assertThrows(AvroMessageBuilderException.class, () ->
                ProcessSnapshotCreatedEventBuilder.create()
                        .processId(PROCESS_ID)
                        .build());
        assertTrue(t.getMessage().contains("snapshot"), t::getMessage);
    }
}
