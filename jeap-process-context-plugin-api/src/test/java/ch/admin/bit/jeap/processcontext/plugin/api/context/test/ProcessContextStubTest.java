package ch.admin.bit.jeap.processcontext.plugin.api.context.test;

import ch.admin.bit.jeap.processcontext.plugin.api.context.Message;
import ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ProcessContextStubTest {

    private static final String ORIGIN_PROCESS_ID = "origin-process-id";
    private static final String PROCESS_NAME = "test-process";

    @Test
    void getOriginProcessId_returnsOriginProcessId() {
        ProcessContextStub stub = ProcessContextStub.builder()
                .originProcessId(ORIGIN_PROCESS_ID)
                .processName(PROCESS_NAME)
                .build();

        assertEquals(ORIGIN_PROCESS_ID, stub.getOriginProcessId());
    }

    @Test
    void getProcessName_returnsProcessName() {
        ProcessContextStub stub = ProcessContextStub.builder()
                .originProcessId(ORIGIN_PROCESS_ID)
                .processName(PROCESS_NAME)
                .build();

        assertEquals(PROCESS_NAME, stub.getProcessName());
    }

    @Test
    void areAllTasksInFinalState_returnsTrue_whenAllTasksCompleted() {
        ProcessContextStub stub = ProcessContextStub.builder()
                .originProcessId(ORIGIN_PROCESS_ID)
                .processName(PROCESS_NAME)
                .allTasksCompleted(true)
                .build();

        assertTrue(stub.areAllTasksInFinalState());
    }

    @Test
    void areAllTasksInFinalState_returnsFalse_whenNotAllTasksCompleted() {
        ProcessContextStub stub = ProcessContextStub.builder()
                .originProcessId(ORIGIN_PROCESS_ID)
                .processName(PROCESS_NAME)
                .allTasksCompleted(false)
                .build();

        assertFalse(stub.areAllTasksInFinalState());
    }

    @Test
    void containsMessageOfType_returnsTrue_whenMessageExists() {
        Message message = Message.builder()
                .name("TestMessage")
                .messageData(Set.of())
                .build();
        ProcessContextStub stub = ProcessContextStub.builder()
                .originProcessId(ORIGIN_PROCESS_ID)
                .processName(PROCESS_NAME)
                .messages(List.of(message))
                .build();

        assertTrue(stub.containsMessageOfType("TestMessage"));
    }

    @Test
    void containsMessageOfType_returnsFalse_whenMessageDoesNotExist() {
        ProcessContextStub stub = ProcessContextStub.builder()
                .originProcessId(ORIGIN_PROCESS_ID)
                .processName(PROCESS_NAME)
                .messages(List.of())
                .build();

        assertFalse(stub.containsMessageOfType("TestMessage"));
    }

    @Test
    void containsMessageOfAnyType_returnsTrue_whenAnyMessageExists() {
        Message message = Message.builder()
                .name("Message1")
                .messageData(Set.of())
                .build();
        ProcessContextStub stub = ProcessContextStub.builder()
                .originProcessId(ORIGIN_PROCESS_ID)
                .processName(PROCESS_NAME)
                .messages(List.of(message))
                .build();

        assertTrue(stub.containsMessageOfAnyType(Set.of("Message1", "Message2")));
    }

    @Test
    void containsMessageOfAnyType_returnsFalse_whenNoMessageExists() {
        ProcessContextStub stub = ProcessContextStub.builder()
                .originProcessId(ORIGIN_PROCESS_ID)
                .processName(PROCESS_NAME)
                .messages(List.of())
                .build();

        assertFalse(stub.containsMessageOfAnyType(Set.of("Message1", "Message2")));
    }

    @Test
    void getMessageDataForMessageType_returnsMessageData_whenMessageExists() {
        Set<MessageData> messageData = Set.of(new MessageData("key", "value"));
        Message message = Message.builder()
                .name("TestMessage")
                .messageData(messageData)
                .build();
        ProcessContextStub stub = ProcessContextStub.builder()
                .originProcessId(ORIGIN_PROCESS_ID)
                .processName(PROCESS_NAME)
                .messages(List.of(message))
                .build();

        assertEquals(messageData, stub.getMessageDataForMessageType("TestMessage"));
    }

    @Test
    void getMessageDataForMessageType_returnsEmptySet_whenMessageDoesNotExist() {
        ProcessContextStub stub = ProcessContextStub.builder()
                .originProcessId(ORIGIN_PROCESS_ID)
                .processName(PROCESS_NAME)
                .messages(List.of())
                .build();

        assertEquals(Set.of(), stub.getMessageDataForMessageType("TestMessage"));
    }

    @Test
    void countMessagesByTypes_returnsCounts() {
        Message message1 = Message.builder().name("Type1").messageData(Set.of()).build();
        Message message2 = Message.builder().name("Type1").messageData(Set.of()).build();
        Message message3 = Message.builder().name("Type2").messageData(Set.of()).build();
        ProcessContextStub stub = ProcessContextStub.builder()
                .originProcessId(ORIGIN_PROCESS_ID)
                .processName(PROCESS_NAME)
                .messages(List.of(message1, message2, message3))
                .build();

        Map<String, Long> counts = stub.countMessagesByTypes(Set.of("Type1", "Type2", "Type3"));

        assertEquals(2L, counts.get("Type1"));
        assertEquals(1L, counts.get("Type2"));
        assertEquals(0L, counts.get("Type3"));

        long count = stub.countMessagesByType("Type1");
        assertEquals(2L, count);

        long countZero = stub.countMessagesByType("Type3");
        assertEquals(0L, countZero);
    }

    @Test
    void containsMessageByTypeWithMessageData_returnsTrue_whenMatchingMessageExists() {
        Message message = Message.builder()
                .name("TestMessage")
                .messageData(Set.of(new MessageData("status", "completed")))
                .build();
        ProcessContextStub stub = ProcessContextStub.builder()
                .originProcessId(ORIGIN_PROCESS_ID)
                .processName(PROCESS_NAME)
                .messages(List.of(message))
                .build();

        assertTrue(stub.containsMessageByTypeWithMessageData("TestMessage", "status", "completed"));
    }

    @Test
    void containsMessageByTypeWithMessageData_returnsFalse_whenNoMatchingMessageData() {
        Message message = Message.builder()
                .name("TestMessage")
                .messageData(Set.of(new MessageData("status", "pending")))
                .build();
        ProcessContextStub stub = ProcessContextStub.builder()
                .originProcessId(ORIGIN_PROCESS_ID)
                .processName(PROCESS_NAME)
                .messages(List.of(message))
                .build();

        assertFalse(stub.containsMessageByTypeWithMessageData("TestMessage", "status", "completed"));
    }

    @Test
    void containsMessageByTypeWithAnyMessageDataValue_returnsTrue_whenAnyValueMatches() {
        Message message = Message.builder()
                .name("TestMessage")
                .messageData(Set.of(new MessageData("status", "completed")))
                .build();
        ProcessContextStub stub = ProcessContextStub.builder()
                .originProcessId(ORIGIN_PROCESS_ID)
                .processName(PROCESS_NAME)
                .messages(List.of(message))
                .build();

        assertTrue(stub.containsMessageByTypeWithAnyMessageDataValue("TestMessage", "status", Set.of("pending", "completed")));
    }

    @Test
    void containsMessageByTypeWithAnyMessageDataValue_returnsFalse_whenNoValueMatches() {
        Message message = Message.builder()
                .name("TestMessage")
                .messageData(Set.of(new MessageData("status", "active")))
                .build();
        ProcessContextStub stub = ProcessContextStub.builder()
                .originProcessId(ORIGIN_PROCESS_ID)
                .processName(PROCESS_NAME)
                .messages(List.of(message))
                .build();

        assertFalse(stub.containsMessageByTypeWithAnyMessageDataValue("TestMessage", "status", Set.of("pending", "completed")));
    }

    @Test
    void containsMessageByTypeWithAnyMessageDataKeyValue_returnsTrue_whenAnyKeyValueMatches() {
        Message message = Message.builder()
                .name("TestMessage")
                .messageData(Set.of(new MessageData("key2", "value2")))
                .build();
        ProcessContextStub stub = ProcessContextStub.builder()
                .originProcessId(ORIGIN_PROCESS_ID)
                .processName(PROCESS_NAME)
                .messages(List.of(message))
                .build();

        Map<String, Set<String>> filter = Map.of(
                "key1", Set.of("value1"),
                "key2", Set.of("value2", "value3")
        );
        assertTrue(stub.containsMessageByTypeWithAnyMessageDataKeyValue("TestMessage", filter));
    }

    @Test
    void containsMessageByTypeWithAnyMessageDataKeyValue_returnsFalse_whenNoKeyValueMatches() {
        Message message = Message.builder()
                .name("TestMessage")
                .messageData(Set.of(new MessageData("key3", "value3")))
                .build();
        ProcessContextStub stub = ProcessContextStub.builder()
                .originProcessId(ORIGIN_PROCESS_ID)
                .processName(PROCESS_NAME)
                .messages(List.of(message))
                .build();

        Map<String, Set<String>> filter = Map.of(
                "key1", Set.of("value1"),
                "key2", Set.of("value2")
        );
        assertFalse(stub.containsMessageByTypeWithAnyMessageDataKeyValue("TestMessage", filter));
    }

    @Test
    void countMessagesByTypeWithMessageData_returnsCount() {
        Message message1 = Message.builder()
                .name("TestMessage")
                .messageData(Set.of(new MessageData("status", "completed")))
                .build();
        Message message2 = Message.builder()
                .name("TestMessage")
                .messageData(Set.of(new MessageData("status", "completed")))
                .build();
        Message message3 = Message.builder()
                .name("TestMessage")
                .messageData(Set.of(new MessageData("status", "pending")))
                .build();
        ProcessContextStub stub = ProcessContextStub.builder()
                .originProcessId(ORIGIN_PROCESS_ID)
                .processName(PROCESS_NAME)
                .messages(List.of(message1, message2, message3))
                .build();

        assertEquals(2L, stub.countMessagesByTypeWithMessageData("TestMessage", "status", "completed"));
    }

    @Test
    void countMessagesByTypeWithAnyMessageData_returnsCount() {
        Message message1 = Message.builder()
                .name("TestMessage")
                .messageData(Set.of(new MessageData("key1", "value1")))
                .build();
        Message message2 = Message.builder()
                .name("TestMessage")
                .messageData(Set.of(new MessageData("key2", "value2")))
                .build();
        Message message3 = Message.builder()
                .name("TestMessage")
                .messageData(Set.of(new MessageData("key3", "value3")))
                .build();
        ProcessContextStub stub = ProcessContextStub.builder()
                .originProcessId(ORIGIN_PROCESS_ID)
                .processName(PROCESS_NAME)
                .messages(List.of(message1, message2, message3))
                .build();

        Map<String, String> filter = Map.of("key1", "value1", "key2", "value2");
        assertEquals(2L, stub.countMessagesByTypeWithAnyMessageData("TestMessage", filter));
    }

    @Test
    void builderWithNullMessages_defaultsToEmptyList() {
        ProcessContextStub stub = ProcessContextStub.builder()
                .originProcessId(ORIGIN_PROCESS_ID)
                .processName(PROCESS_NAME)
                .messages(null)
                .build();

        assertFalse(stub.containsMessageOfType("any"));
    }
}
