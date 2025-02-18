package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskType;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static java.lang.String.format;

public class TaskPlanningException extends RuntimeException {

    private TaskPlanningException(String message) {
        super(message);
    }

    static TaskPlanningException createTaskAlreadyPlanned(String originProcessId, TaskType taskType, List<TaskInstance> existingInstances) {
        return new TaskPlanningException(format(
                "Task type %s has already been planned in process with ID %s, instances: %s",
                taskType,
                originProcessId,
                existingInstances));
    }

    static TaskPlanningException createWrongNumberOfTasks(TaskType taskType, Collection<String> originTaskIds, String originProcessId) {
        return new TaskPlanningException(format(
                "Cannot create tasks %s for task type %s in process with ID %s as this is not within the number of allowed tasks",
                originTaskIds,
                taskType,
                originProcessId));
    }

    static Supplier<TaskPlanningException> invalidTaskType(String taskTypeName, String originProcessId) {
        return () -> new TaskPlanningException(format(
                "There is no task type %s in process with ID %s ",
                taskTypeName,
                originProcessId));
    }

    public static TaskPlanningException singleInstanceTaskMultipleIds(TaskType taskType, Set<String> relatedOriginTaskIds) {
        String taskIds = String.join(",", relatedOriginTaskIds);
        return new TaskPlanningException(format("""
                Task type %s is declared single instance. \
                It must therefore not be planned with multiple related origin task ids (%s)\
                """, taskType, taskIds));
    }
}
