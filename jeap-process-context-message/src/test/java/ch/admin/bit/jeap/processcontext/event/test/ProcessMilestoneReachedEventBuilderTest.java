package ch.admin.bit.jeap.processcontext.event.test;

import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bit.jeap.processcontext.event.ProcessMilestoneReachedEventBuilder;
import ch.admin.bit.jeap.processcontext.event.process.milestone.reached.ProcessMilestoneReachedEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProcessMilestoneReachedEventBuilderTest {

    private static final String SYSTEM_NAME = "test-system-name";
    private static final String SERVICE_NAME = "test-service-name";
    private static final String PROCESS_ID = "test-process-id";
    private static final String IDEMPOTENCE_ID = "test-idempotence-id";
    private static final String MILESTONE_NAME = "test-milestone-name";

    @Test
    void testBuild() {
        ProcessMilestoneReachedEvent event = ProcessMilestoneReachedEventBuilder.create().
                idempotenceId(IDEMPOTENCE_ID).
                systemName(SYSTEM_NAME).
                serviceName(SERVICE_NAME).
                processId(PROCESS_ID).
                milestoneName(MILESTONE_NAME).
                build();

        assertNotNull(event.getIdentity().getEventId());
        assertEquals(IDEMPOTENCE_ID, event.getIdentity().getIdempotenceId());
        assertEquals(SYSTEM_NAME, event.getPublisher().getSystem());
        assertEquals(SERVICE_NAME, event.getPublisher().getService());
        assertEquals(PROCESS_ID, event.getProcessId());
        assertEquals(MILESTONE_NAME, event.getPayload().getMilestoneName());
    }

    @Test
    void testBuild_whenProcessIdMissing_thenThrowsAvroMessageBuilderException() {
        Throwable t = assertThrows(AvroMessageBuilderException.class, () ->
                ProcessMilestoneReachedEventBuilder.create().build());
        assertTrue(t.getMessage().contains("processId"));
    }

    @Test
    void testBuild_whenMilestoneNameMissing_thenThrowsAvroMessageBuilderException() {
        Throwable t = assertThrows(AvroMessageBuilderException.class, () ->
                ProcessMilestoneReachedEventBuilder.create()
                        .processId(PROCESS_ID)
                        .build());
        assertTrue(t.getMessage().contains("milestone"), t::getMessage);
    }
}
