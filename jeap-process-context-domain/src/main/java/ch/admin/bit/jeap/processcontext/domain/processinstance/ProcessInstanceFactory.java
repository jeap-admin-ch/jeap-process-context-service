package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.port.MetricsListener;
import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextFactory;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
class ProcessInstanceFactory {

    record ProcessInstanceCreationResult(ProcessInstance processInstance, boolean isNewlyCreated) {
    }

    private final ProcessInstanceRepository processInstanceRepository;
    private final ProcessTemplateRepository processTemplateRepository;
    private final ProcessContextFactory processContextFactory;
    private final MetricsListener metricsListener;
    private final TaskInstanceRepository taskInstanceRepository;

    Optional<ProcessInstance> createProcessInstance(String originProcessId, String createProcessByTemplateName) {
        if (createProcessByTemplateName != null) {
            ProcessInstance createdInstance = createFromTemplate(originProcessId, createProcessByTemplateName);
            return Optional.of(createdInstance);
        }

        return Optional.empty();
    }

    private ProcessInstance createFromTemplate(String originProcessId, String processTemplateName) {
        ProcessTemplate processTemplate = processTemplateRepository.findByName(processTemplateName)
                .orElseThrow(NotFoundException.templateNotFound(processTemplateName, originProcessId));
        ProcessInstance processInstance = ProcessInstance.createProcessInstance(originProcessId, processTemplate, processContextFactory);
        ProcessInstance managedEntity = processInstanceRepository.save(processInstance);
        managedEntity.onAfterLoadFromPersistentState(processTemplate, processContextFactory);
        planInitialTasks(managedEntity);

        log.info("Creating process {} with origin process ID {} from template {}", processInstance.getId(), originProcessId, processTemplateName);
        metricsListener.processInstanceCreated(processTemplateName);

        // Count completion in case the process instance is completed immediately after creation
        if (processInstance.getState() == ProcessState.COMPLETED) {
            metricsListener.processCompleted(processInstance.getProcessTemplate());
        }

        return managedEntity;
    }

    /**
     * Start the process instance by planning initial tasks as defined in the process template. Creates task instances
     * for all task types that are defined to be planned at process start. As this method invokes
     * {@link ProcessInstance#updateState()} internally, the process instance must be persisted first in case task or
     * process completion conditions require access to persisted data.
     */
    private void planInitialTasks(ProcessInstance processInstance) {
        List<TaskInstance> taskInstances = processInstance.getProcessTemplate().getTaskTypes().stream()
                .filter(TaskType::isPlannedAtProcessStart)
                .map(type -> TaskInstance.createInitialTaskInstance(type, processInstance, ZonedDateTime.now()))
                .toList();
        taskInstanceRepository.saveAll(taskInstances);
        processInstanceRepository.flush();
        processInstance.updateState();
    }
}
