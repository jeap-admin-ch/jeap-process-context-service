package ch.admin.bit.jeap.processcontext.domain.processinstance.api;

import ch.admin.bit.jeap.processcontext.plugin.api.message.MessageData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessContextImplTest {

    private static final UUID PROCESS_INSTANCE_ID = UUID.randomUUID();
    private static final String ORIGIN_PROCESS_ID = "origin-process-id";
    private static final String PROCESS_NAME = "test-process";

    @Mock
    private ProcessContextRepositoryFacade repositoryFacade;

    private ProcessContextImpl processContext;

    @BeforeEach
    void setUp() {
        processContext = ProcessContextImpl.builder()
                .processInstanceId(PROCESS_INSTANCE_ID)
                .originProcessId(ORIGIN_PROCESS_ID)
                .processTemplate(PROCESS_NAME)
                .repositoryFacade(repositoryFacade)
                .build();
    }

    @Test
    void getOriginProcessId_returnsOriginProcessId() {
        assertEquals(ORIGIN_PROCESS_ID, processContext.getOriginProcessId());
    }

    @Test
    void getProcessName_returnsProcessName() {
        assertEquals(PROCESS_NAME, processContext.getProcessName());
    }

    @Test
    void areAllTasksInFinalState_delegatesToFacade() {
        when(repositoryFacade.areAllTasksInFinalState(PROCESS_INSTANCE_ID)).thenReturn(true);

        assertTrue(processContext.areAllTasksInFinalState());

        verify(repositoryFacade).areAllTasksInFinalState(PROCESS_INSTANCE_ID);
    }

    @Test
    void areAllTasksInFinalState_returnsFalseWhenFacadeReturnsFalse() {
        when(repositoryFacade.areAllTasksInFinalState(PROCESS_INSTANCE_ID)).thenReturn(false);

        assertFalse(processContext.areAllTasksInFinalState());
    }

    @Test
    void containsMessageOfType_delegatesToFacade() {
        String messageType = "TestMessage";
        when(repositoryFacade.containsMessageOfType(PROCESS_INSTANCE_ID, messageType)).thenReturn(true);

        assertTrue(processContext.containsMessageOfType(messageType));

        verify(repositoryFacade).containsMessageOfType(PROCESS_INSTANCE_ID, messageType);
    }

    @Test
    void containsMessageOfAnyType_delegatesToFacade() {
        Set<String> messageTypes = Set.of("Message1", "Message2");
        when(repositoryFacade.containsMessageOfAnyType(PROCESS_INSTANCE_ID, messageTypes)).thenReturn(true);

        assertTrue(processContext.containsMessageOfAnyType(messageTypes));

        verify(repositoryFacade).containsMessageOfAnyType(PROCESS_INSTANCE_ID, messageTypes);
    }

    @Test
    void getMessageDataForMessageType_delegatesToFacade() {
        String messageType = "TestMessage";
        Set<MessageData> expectedData = Set.of(new MessageData("key", "value"));
        when(repositoryFacade.getMessageDataForMessageType(PROCESS_INSTANCE_ID, messageType, PROCESS_NAME)).thenReturn(expectedData);

        Set<MessageData> result = processContext.getMessageDataForMessageType(messageType);

        assertEquals(expectedData, result);
        verify(repositoryFacade).getMessageDataForMessageType(PROCESS_INSTANCE_ID, messageType, PROCESS_NAME);
    }

    @Test
    void countMessagesByType_delegatesToFacade() {
        when(repositoryFacade.countMessagesByType(PROCESS_INSTANCE_ID, "messageType"))
                .thenReturn(1L);

        long result = processContext.countMessagesByType("messageType");

        assertEquals(1L, result);
        verify(repositoryFacade).countMessagesByType(PROCESS_INSTANCE_ID, "messageType");
    }

    @Test
    void countMessagesByTypes_delegatesToFacade() {
        Set<String> messageTypes = Set.of("Message1", "Message2");
        Map<String, Long> expectedCounts = Map.of("Message1", 5L, "Message2", 3L);
        when(repositoryFacade.countMessagesByTypes(PROCESS_INSTANCE_ID, messageTypes)).thenReturn(expectedCounts);

        Map<String, Long> result = processContext.countMessagesByTypes(messageTypes);

        assertEquals(expectedCounts, result);
        verify(repositoryFacade).countMessagesByTypes(PROCESS_INSTANCE_ID, messageTypes);
    }

    @Test
    void countMessagesByTypeWithMessageData_delegatesToFacade() {
        String messageType = "TestMessage";
        String key = "status";
        String value = "completed";
        when(repositoryFacade.countMessagesByTypeWithMessageData(PROCESS_INSTANCE_ID, messageType, key, value, PROCESS_NAME)).thenReturn(7L);

        long result = processContext.countMessagesByTypeWithMessageData(messageType, key, value);

        assertEquals(7L, result);
        verify(repositoryFacade).countMessagesByTypeWithMessageData(PROCESS_INSTANCE_ID, messageType, key, value, PROCESS_NAME);
    }

    @Test
    void countMessagesByTypeWithAnyMessageData_delegatesToFacade() {
        String messageType = "TestMessage";
        Map<String, String> messageDataFilter = Map.of("key1", "value1", "key2", "value2");
        when(repositoryFacade.countMessagesByTypeWithAnyMessageData(PROCESS_INSTANCE_ID, messageType, messageDataFilter, PROCESS_NAME)).thenReturn(4L);

        long result = processContext.countMessagesByTypeWithAnyMessageData(messageType, messageDataFilter);

        assertEquals(4L, result);
        verify(repositoryFacade).countMessagesByTypeWithAnyMessageData(PROCESS_INSTANCE_ID, messageType, messageDataFilter, PROCESS_NAME);
    }

    @Test
    void containsMessageByTypeWithMessageData_delegatesToFacade() {
        String messageType = "TestMessage";
        String key = "status";
        String value = "completed";
        when(repositoryFacade.containsMessageByTypeWithMessageData(PROCESS_INSTANCE_ID, messageType, key, value, PROCESS_NAME)).thenReturn(true);

        assertTrue(processContext.containsMessageByTypeWithMessageData(messageType, key, value));

        verify(repositoryFacade).containsMessageByTypeWithMessageData(PROCESS_INSTANCE_ID, messageType, key, value, PROCESS_NAME);
    }

    @Test
    void containsMessageByTypeWithAnyMessageDataValue_delegatesToFacade() {
        String messageType = "TestMessage";
        String key = "status";
        Set<String> values = Set.of("completed", "failed");
        when(repositoryFacade.containsMessageByTypeWithAnyMessageDataValue(PROCESS_INSTANCE_ID, messageType, key, values, PROCESS_NAME)).thenReturn(true);

        assertTrue(processContext.containsMessageByTypeWithAnyMessageDataValue(messageType, key, values));

        verify(repositoryFacade).containsMessageByTypeWithAnyMessageDataValue(PROCESS_INSTANCE_ID, messageType, key, values, PROCESS_NAME);
    }

    @Test
    void containsMessageByTypeWithAnyMessageDataKeyValue_delegatesToFacade() {
        String messageType = "TestMessage";
        Map<String, Set<String>> messageDataFilter = Map.of("key1", Set.of("value1", "value2"), "key2", Set.of("value3"));
        when(repositoryFacade.containsMessageByTypeWithAnyMessageDataKeyValue(PROCESS_INSTANCE_ID, messageType, messageDataFilter, PROCESS_NAME)).thenReturn(true);

        assertTrue(processContext.containsMessageByTypeWithAnyMessageDataKeyValue(messageType, messageDataFilter));

        verify(repositoryFacade).containsMessageByTypeWithAnyMessageDataKeyValue(PROCESS_INSTANCE_ID, messageType, messageDataFilter, PROCESS_NAME);
    }
}
