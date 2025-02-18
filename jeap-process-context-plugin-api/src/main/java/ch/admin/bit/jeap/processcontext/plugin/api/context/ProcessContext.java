package ch.admin.bit.jeap.processcontext.plugin.api.context;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.groupingBy;

@SuppressWarnings("removal")
public final class ProcessContext {

    @Getter
    private final String originProcessId;

    @Getter
    private final String processName;

    @Getter
    private final ProcessState processState;

    @Getter
    private final ProcessCompletion processCompletion;

    @Getter
    private final List<Task> tasks;

    @Getter
    private final List<Message> messages;

    private final Map<String, List<Task>> tasksByName;

    private final Map<String, List<Message>> messagesByName;

    @Builder
    private ProcessContext(@NonNull String originProcessId,
                           @NonNull String processName,
                           @NonNull ProcessState processState,
                           @NonNull List<Task> tasks,
                           @NonNull List<Message> messages,
                           ProcessCompletion processCompletion) {
        this.originProcessId = originProcessId;
        this.processName = processName;
        this.processState = processState;
        this.tasks = tasks;
        this.messages = messages;
        if ((processState == ProcessState.COMPLETED) && (processCompletion == null)) {
            throw new IllegalArgumentException("Process completion must be provided for a process in completed state.");
        }
        this.processCompletion = processCompletion;
        this.tasksByName = tasks.stream()
                .collect(groupingBy(task -> task.getType().getName()));
        this.messagesByName = messages.stream()
                .collect(groupingBy(Message::getName));
    }

    /**
     * @deprecated Replaced by {@link #getMessages()}
     */
    @Deprecated(since = "7.0.0", forRemoval = true)
    public List<Event> getEvents() {
        return messages.stream()
                .map(message -> new Event(message.getName(), message.getRelatedOriginTaskIds(), message.getMessageData()))
                .toList();
    }

    /**
     * @param name          the name of the task
     * @param expectedState the expected state
     * @return true if at least one task with this name exists, and all tasks of this type are in the expected state
     */
    public boolean isTasksInState(String name, TaskState expectedState) {
        List<Task> tasksByNameRetrieved = getTasksByName(name);
        return !tasksByNameRetrieved.isEmpty() &&
                tasksByNameRetrieved.stream().allMatch(task -> task.getState() == expectedState);
    }

    public List<Task> getTasksByName(String name) {
        return tasksByName.getOrDefault(name, Collections.emptyList());
    }

    /**
     * @deprecated Replaced by {@link #getMessagesByName(String)}
     */
    @Deprecated(since = "7.0.0", forRemoval = true)
    public List<Event> getEventsByName(String name) {
        return messagesByName.getOrDefault(name, List.of())
                .stream()
                .map(message -> new Event(message.getName(), message.getRelatedOriginTaskIds(), message.getMessageData()))
                .toList();
    }

    /**
     * @param taskId the task id
     * @return Task with matching task ID
     */
    public Optional<Task> getTaskById(String taskId) {
        return tasks.stream()
                .filter(task -> task.getId().equals(taskId))
                .findFirst();
    }

    public List<Message> getMessagesByName(String messageName) {
        return messagesByName.getOrDefault(messageName, List.of());
    }
}
