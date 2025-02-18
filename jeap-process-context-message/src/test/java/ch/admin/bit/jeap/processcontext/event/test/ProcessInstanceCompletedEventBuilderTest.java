package ch.admin.bit.jeap.processcontext.event.test;

import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bit.jeap.processcontext.event.ProcessInstanceCompletedEventBuilder;
import ch.admin.bit.jeap.processcontext.event.process.instance.completed.ProcessInstanceCompletedEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProcessInstanceCompletedEventBuilderTest {

    private static final String SYSTEM_NAME = "test-system-name";
    private static final String SERVICE_NAME = "test-service-name";
    private static final String PROCESS_ID = "test-process-id";
    private static final String IDEMPOTENCE_ID = "test-idempotence-id";

    @Test
    void testBuild() {
        ProcessInstanceCompletedEvent event = ProcessInstanceCompletedEventBuilder.create().
                idempotenceId(IDEMPOTENCE_ID).
                systemName(SYSTEM_NAME).
                serviceName(SERVICE_NAME).
                processId(PROCESS_ID).
                build();

        assertNotNull(event.getIdentity().getEventId());
        assertEquals(IDEMPOTENCE_ID, event.getIdentity().getIdempotenceId());
        assertEquals(SYSTEM_NAME, event.getPublisher().getSystem());
        assertEquals(SERVICE_NAME, event.getPublisher().getService());
        assertEquals(PROCESS_ID, event.getProcessId());
        assertFalse(event.getOptionalPayload().isPresent());
    }

    @Test
    void testBuild_whenProcessIdMissing_thenThrowsAvroMessageBuilderException() {
        Throwable t = assertThrows(AvroMessageBuilderException.class, () ->
                ProcessInstanceCompletedEventBuilder.create().build());
        assertTrue(t.getMessage().contains("processId"));
    }

}
