package ch.admin.bit.jeap.processcontext.plugin.api.context;

import ch.admin.bit.jeap.processcontext.plugin.api.context.test.ProcessContextStub;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessContextTest {

    @Test
    void getEventsByName() {
        ProcessContext processContext = createProcessContextWithEvents(
                createMessage("event", Set.of("2")),
                createMessage("event", Set.of("1")),
                createMessage("other", Set.of()));

        List<Message> messages = processContext.getMessagesByName("event");
        assertEquals(2, messages.size());
        assertEquals(processContext.getMessages().getFirst(), messages.getFirst());
        assertEquals(processContext.getMessages().get(1), messages.get(1));
    }

    @Test
    void getEventsByName_whenNoEventsArePresent_shouldReturnEmptyList() {
        ProcessContext processContext = createProcessContextWithEvents();

        List<Message> messages = processContext.getMessagesByName("name");
        assertTrue(messages.isEmpty());
    }

    private Message createMessage(String name, Set<String> originTaskIds) {
        return Message.builder()
                .name(name)
                .relatedOriginTaskIds(originTaskIds)
                .build();
    }

    private static ProcessContext createProcessContextWithEvents(Message... messages) {
        return createProcessContext(List.of(messages));
    }

    private static ProcessContext createProcessContext(List<Message> events) {
        return ProcessContextStub.builder()
                .originProcessId("id")
                .processName("name")
                .processState(ProcessState.STARTED)
                .messages(events)
                .build();
    }
}
