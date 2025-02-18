package ch.admin.bit.jeap.processcontext.plugin.api.context;

import com.fasterxml.uuid.Generators;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ProcessContextTest {

    @Test
    void isTasksInState() {
        ProcessContext processContext = createProcessContextWithTasks(
                createTask("name", TaskState.COMPLETED),
                createTask("name", TaskState.COMPLETED),
                createTask("other", TaskState.PLANNED));

        assertTrue(processContext.isTasksInState("name", TaskState.COMPLETED));
    }

    @Test
    void isTasksInState_whenNoTaskWithThisName_thenReturnFalse() {
        ProcessContext processContext = createProcessContextWithTasks();

        assertFalse(processContext.isTasksInState("name", TaskState.COMPLETED));
    }

    @Test
    void isTasksInState_whenNotTasksInExpectedState_thenReturnFalse() {
        ProcessContext processContext = createProcessContextWithTasks(
                createTask("name", TaskState.COMPLETED),
                createTask("name", TaskState.PLANNED));

        assertFalse(processContext.isTasksInState("name", TaskState.COMPLETED));
    }

    @Test
    void getEventsByName() {
        ProcessContext processContext = createProcessContextWithEvents(
                createMessage("event", Set.of("2")),
                createMessage("event", Set.of("1")),
                createMessage("other", Set.of()));

        List<Message> messages = processContext.getMessagesByName("event");
        assertEquals(2, messages.size());
        assertEquals(processContext.getMessages().get(0), messages.get(0));
        assertEquals(processContext.getMessages().get(1), messages.get(1));
    }

    @Test
    void getEventsByName_whenNoEventsArePresent_shouldReturnEmptyList() {
        ProcessContext processContext = createProcessContextWithEvents();

        List<Message> messages = processContext.getMessagesByName("name");
        assertTrue(messages.isEmpty());
    }

    private static Task createTask(String name, TaskState state) {
        TaskType taskType = TaskType.builder()
                .name(name)
                .lifecycle(TaskLifecycle.STATIC)
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .build();
        return Task.builder()
                .type(taskType)
                .state(state)
                .originTaskId("id")
                .id(Generators.timeBasedEpochGenerator().generate().toString())
                .build();
    }

    private Message createMessage(String name, Set<String> originTaskIds) {
        return Message.builder()
                .name(name)
                .relatedOriginTaskIds(originTaskIds)
                .build();
    }

    private static ProcessContext createProcessContextWithTasks(Task... tasks) {
        return createProcessContext(List.of(tasks), List.of());
    }

    private static ProcessContext createProcessContextWithEvents(Message... messages) {
        return createProcessContext(List.of(), List.of(messages));
    }

    private static ProcessContext createProcessContext(List<Task> tasks, List<Message> events) {
        return ProcessContext.builder()
                .originProcessId("id")
                .processName("name")
                .processState(ProcessState.STARTED)
                .tasks(tasks)
                .messages(events)
                .build();
    }
}
