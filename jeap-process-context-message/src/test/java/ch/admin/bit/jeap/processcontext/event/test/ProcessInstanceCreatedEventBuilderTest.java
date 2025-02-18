package ch.admin.bit.jeap.processcontext.event.test;

import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bit.jeap.processcontext.event.ProcessInstanceCreatedEventBuilder;
import ch.admin.bit.jeap.processcontext.event.process.instance.created.ProcessInstanceCreatedEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProcessInstanceCreatedEventBuilderTest {

    private static final String SYSTEM_NAME = "test-system-name";
    private static final String SERVICE_NAME = "test-service-name";
    private static final String PROCESS_ID = "test-process-id";
    private static final String IDEMPOTENCE_ID = "test-idempotence-id";
    private static final String PROCESS_NAME = "process-name";

    @Test
    void testBuild() {
        ProcessInstanceCreatedEvent event = ProcessInstanceCreatedEventBuilder.create().
                idempotenceId(IDEMPOTENCE_ID).
                systemName(SYSTEM_NAME).
                serviceName(SERVICE_NAME).
                processId(PROCESS_ID).
                processName(PROCESS_NAME).
                build();

        assertNotNull(event.getIdentity().getEventId());
        assertEquals(IDEMPOTENCE_ID, event.getIdentity().getIdempotenceId());
        assertEquals(SYSTEM_NAME, event.getPublisher().getSystem());
        assertEquals(SERVICE_NAME, event.getPublisher().getService());
        assertEquals(PROCESS_ID, event.getProcessId());
        assertEquals(PROCESS_NAME, event.getPayload().getProcessName());
    }

    @Test
    void testBuild_whenProcessIdMissing_thenThrowsAvroMessageBuilderException() {
        Throwable t = assertThrows(AvroMessageBuilderException.class, () ->
                ProcessInstanceCreatedEventBuilder.create().build());
        assertTrue(t.getMessage().contains("processId"));
    }

}
