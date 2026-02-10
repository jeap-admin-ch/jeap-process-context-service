package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.*;

import static java.util.stream.Collectors.toCollection;

@RequiredArgsConstructor
@Component
@Slf4j
class ProcessInstanceMigrationService {

    private final TaskInstanceRepository taskInstanceRepository;
    private final TaskService taskService;

    /**
     * @return If the process template has changed since the last time it was applied to this process instance, apply
     * necessary migrations to existing tasks and return the list of newly planned tasks. If the template has not
     * changed, return an empty optional.
     */
    public Optional<List<TaskInstance>> applyTemplateMigrationIfChanged(ProcessInstance processInstance) {
        if (isTemplateChanged(processInstance)) {
            log.info("Applying template migrations to process {}", processInstance.getOriginProcessId());

            deleteTaskInstancesForDeletedTaskTypes(processInstance);
            List<TaskInstance> plannedTasks = planTaskInstancesForNewTaskTypes(processInstance);
            taskInstanceRepository.flush();

            processInstance.updateTemplateHash();

            return Optional.of(plannedTasks);
        }
        return Optional.empty();
    }

    private boolean isTemplateChanged(ProcessInstance processInstance) {
        String processTemplateHash = processInstance.getProcessTemplateHash();
        return processTemplateHash != null &&
                !processInstance.getProcessTemplate().getTemplateHash().equals(processTemplateHash);
    }

    private void deleteTaskInstancesForDeletedTaskTypes(ProcessInstance processInstance) {
        Set<String> existingTaskNames = processInstance.getProcessTemplate().getTaskNames();
        taskInstanceRepository.deleteRemovedOpenTasks(processInstance.getId(), existingTaskNames);
    }

    private List<TaskInstance> planTaskInstancesForNewTaskTypes(ProcessInstance processInstance) {
        ProcessTemplate processTemplate = processInstance.getProcessTemplate();

        Set<String> typeNamesPlannedAtStartNotYetInstantiated = getTaskTypeNamesPlannedAtStartNotYetInstantiated(processInstance, processTemplate);

        // Plan missing task types in UNKNOWN state. It is unknown if these tasks have already been completed or not.
        // There might for example be a new event correlated to the process that did not yet exist in the previous
        // template version.
        List<TaskInstance> plannedTasks = new ArrayList<>();
        for (String plannedAtStartType : typeNamesPlannedAtStartNotYetInstantiated) {
            TaskType taskType = processTemplate.getTaskTypeByName(plannedAtStartType)
                    .orElseThrow(TaskPlanningException.invalidTaskType(plannedAtStartType, processInstance.getOriginProcessId()));
            Optional<TaskInstance> plannedTask = taskService.registerNewTaskInUnknownState(processInstance, taskType, ZonedDateTime.now());
            plannedTask.ifPresent(plannedTasks::add);
        }
        return plannedTasks;
    }

    private Set<String> getTaskTypeNamesPlannedAtStartNotYetInstantiated(ProcessInstance processInstance, ProcessTemplate processTemplate) {
        // Find all task types that are planned at process start
        Set<String> typeNamesPlannedAtStart = processTemplate.getTaskTypes().stream()
                .filter(TaskType::isPlannedAtProcessStart)
                .map(TaskType::getName)
                .collect(toCollection(HashSet::new));

        // Filter out existing task types in the persistent instance
        Set<String> existingTaskTypes = taskInstanceRepository.findTaskTypeNamesByProcessInstanceId(processInstance.getId());
        typeNamesPlannedAtStart.removeAll(existingTaskTypes);
        return typeNamesPlannedAtStart;
    }
}
