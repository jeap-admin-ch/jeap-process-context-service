package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface TaskInstanceRepository {

    List<TaskInstance> findByProcessInstanceId(ProcessTemplate processTemplate, UUID processInstanceId);

    Set<String> findTaskTypeNamesByProcessInstanceId(UUID processInstanceId);

    /**
     * Finds all tasks of the given types that are in a non-final state (i.e. not COMPLETED, NOT_REQUIRED or DELETED).
     *
     * @param taskTypeFilter Set of task type names to filter by (not empty)
     * @return All {@link TaskInstance}s matching the given task types, being in a non-final state
     */
    List<TaskInstance> getTaskInstancesInNonFinalStateOfTypes(ProcessTemplate processTemplate, UUID processInstanceId, Set<String> taskTypeFilter);

    /**
     * Deletes all open tasks for the given process instance that are not contained in the given set of remaining task type names.
     */
    void deleteRemovedOpenTasks(UUID processInstanceId, Set<String> remainingTaskTypeNames);

    void saveAll(List<TaskInstance> taskInstances);

    boolean existsByProcessInstanceIdAndTaskTypeNameAndOriginTaskId(UUID processInstanceId, String taskTypeName, String originTaskId);

    TaskInstance save(TaskInstance taskInstance);

    void flush();
}
