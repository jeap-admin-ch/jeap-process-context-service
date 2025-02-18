package ch.admin.bit.jeap.processcontext.command.test;

import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bit.jeap.processcontext.command.CreateProcessInstanceCommandBuilder;
import ch.admin.bit.jeap.processcontext.command.process.instance.create.CreateProcessInstanceCommand;
import ch.admin.bit.jeap.processcontext.command.process.instance.create.ProcessData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CreateProcessInstanceCommandBuilderTest {

    private static final String SYSTEM_NAME = "test-system-name";
    private static final String SERVICE_NAME = "test-service-name";
    private static final String PROCESS_ID = "test-process-id";
    private static final String IDEMPOTENCE_ID = "test-idempotence-id";
    private static final String PROCESS_TEMPLATE_NAME = "test-process-template-name";

    @Test
    void testBuild_processDataNull() {
        CreateProcessInstanceCommand command = CreateProcessInstanceCommandBuilder.create().
                idempotenceId(IDEMPOTENCE_ID).
                systemName(SYSTEM_NAME).
                serviceName(SERVICE_NAME).
                processId(PROCESS_ID).
                processTemplateName(PROCESS_TEMPLATE_NAME).
                build();

        assertNotNull(command.getIdentity().getId());
        assertEquals(IDEMPOTENCE_ID, command.getIdentity().getIdempotenceId());
        assertEquals(SYSTEM_NAME, command.getPublisher().getSystem());
        assertEquals(SERVICE_NAME, command.getPublisher().getService());
        assertEquals(PROCESS_ID, command.getProcessId());
        assertEquals(PROCESS_TEMPLATE_NAME, command.getPayload().getProcessTemplateName());
        assertTrue(command.getOptionalPayload().isPresent());
        assertNull(command.getPayload().getProcessData());
    }

    @Test
    void testBuild_processDataList() {
        List<ProcessData> processData = new ArrayList<>();
        processData.add(new ProcessData("key1", "value1", "role1"));
        processData.add(new ProcessData("key2", "value2", null));
        CreateProcessInstanceCommand command = CreateProcessInstanceCommandBuilder.create().
                idempotenceId(IDEMPOTENCE_ID).
                systemName(SYSTEM_NAME).
                serviceName(SERVICE_NAME).
                processId(PROCESS_ID).
                processTemplateName(PROCESS_TEMPLATE_NAME).
                processData(processData).
                build();

        assertNotNull(command.getIdentity().getId());
        assertEquals(IDEMPOTENCE_ID, command.getIdentity().getIdempotenceId());
        assertEquals(SYSTEM_NAME, command.getPublisher().getSystem());
        assertEquals(SERVICE_NAME, command.getPublisher().getService());
        assertEquals(PROCESS_ID, command.getProcessId());
        assertEquals(PROCESS_TEMPLATE_NAME, command.getPayload().getProcessTemplateName());
        assertTrue(command.getOptionalPayload().isPresent());
        assertNotNull(command.getPayload().getProcessData());
        assertEquals(processData.get(0), command.getPayload().getProcessData().get(0));
        assertEquals(processData.get(1), command.getPayload().getProcessData().get(1));
    }

    @Test
    void testBuild_whenProcessIdMissing_thenThrowsAvroMessageBuilderException() {
        final CreateProcessInstanceCommandBuilder createProcessInstanceCommandBuilder = CreateProcessInstanceCommandBuilder.create();
        Throwable t = assertThrows(AvroMessageBuilderException.class, createProcessInstanceCommandBuilder::build);
        assertTrue(t.getMessage().contains("processId"));
    }

}
