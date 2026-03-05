package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.TaskInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.TaskInstanceRepository;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@Timed(value = "jeap_pcs_repository_taskinstance")
public class TaskInstanceRepositoryImpl implements TaskInstanceRepository {

    private final TaskInstanceJpaRepository taskInstanceJpaRepository;

    @Override
    public List<TaskInstance> findByProcessInstanceId(ProcessTemplate processTemplate, UUID processInstanceId) {
        List<TaskInstance> taskInstances = taskInstanceJpaRepository.findByProcessInstanceId(processInstanceId);
        taskInstances.forEach(task -> task.setTaskTypeFromTemplate(processTemplate));
        return taskInstances;
    }

    @Override
    public List<TaskInstance> getTaskInstancesInNonFinalStateOfTypes(ProcessTemplate processTemplate, UUID processInstanceId, Set<String> taskTypeFilter) {
        List<TaskInstance> taskInstances = taskInstanceJpaRepository.findByTaskTypeNameInAndStateNotFinal(processInstanceId, taskTypeFilter);
        taskInstances.forEach(task -> task.setTaskTypeFromTemplate(processTemplate));
        return taskInstances;
    }

    @Override
    public Set<String> findTaskTypeNamesByProcessInstanceId(UUID processInstanceId) {
        return taskInstanceJpaRepository.findTaskTypeNamesByProcessInstanceId(processInstanceId);
    }

    @Override
    public void deleteRemovedOpenTasks(UUID processInstanceId, Set<String> remainingTaskTypeNames) {
        taskInstanceJpaRepository.deleteRemovedOpenTasks(processInstanceId, remainingTaskTypeNames, ZonedDateTime.now());
    }

    @Override
    public void saveAll(List<TaskInstance> taskInstances) {
        taskInstanceJpaRepository.saveAll(taskInstances);
    }

    @Override
    public boolean existsByProcessInstanceIdAndTaskTypeNameAndOriginTaskId(UUID processInstanceId, String taskTypeName, String originTaskId) {
        return taskInstanceJpaRepository.existsByProcessInstance_IdAndTaskTypeNameAndOriginTaskId(processInstanceId, taskTypeName, originTaskId);
    }

    @Override
    public TaskInstance save(TaskInstance taskInstance) {
        TaskInstance managed = taskInstanceJpaRepository.save(taskInstance);
        managed.setTaskType(taskInstance.getTaskType());
        return managed;
    }

    @Override
    public void flush() {
        taskInstanceJpaRepository.flush();
    }
}
