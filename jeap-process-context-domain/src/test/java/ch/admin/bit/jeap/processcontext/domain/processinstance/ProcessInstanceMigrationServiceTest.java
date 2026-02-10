package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskCardinality;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskLifecycle;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
class ProcessInstanceMigrationServiceTest {

    @Mock
    private TaskInstanceRepository taskInstanceRepository;

    @Mock
    private TaskService taskService;

    private ProcessInstanceMigrationService migrationService;

    @BeforeEach
    void setUp() {
        migrationService = new ProcessInstanceMigrationService(taskInstanceRepository, taskService);
    }

    @Test
    void applyTemplateMigrationIfChanged_templateNotChanged_returnsEmpty() {
        ProcessInstance processInstance = createProcessInstance("hash1", "hash1", List.of(
                staticTask("task1")));

        Optional<List<TaskInstance>> result = migrationService.applyTemplateMigrationIfChanged(processInstance);

        assertThat(result).isEmpty();
        verifyNoInteractions(taskInstanceRepository, taskService);
    }

    @Test
    void applyTemplateMigrationIfChanged_templateHashNull_returnsEmpty() {
        ProcessInstance processInstance = createProcessInstance(null, "hash1", List.of(
                staticTask("task1")));

        Optional<List<TaskInstance>> result = migrationService.applyTemplateMigrationIfChanged(processInstance);

        assertThat(result).isEmpty();
        verifyNoInteractions(taskInstanceRepository, taskService);
    }

    @Test
    void applyTemplateMigrationIfChanged_templateChanged_deletesRemovedTasks() {
        TaskType task1 = staticTask("task1");
        ProcessInstance processInstance = createProcessInstance("oldHash", "newHash", List.of(task1));

        when(taskInstanceRepository.findTaskTypeNamesByProcessInstanceId(processInstance.getId()))
                .thenReturn(Set.of("task1"));

        migrationService.applyTemplateMigrationIfChanged(processInstance);

        verify(taskInstanceRepository).deleteRemovedOpenTasks(processInstance.getId(), Set.of("task1"));
    }

    @Test
    void applyTemplateMigrationIfChanged_templateChanged_plansNewTaskTypes() {
        TaskType existingTask = staticTask("existing");
        TaskType newTask = staticTask("newTask");
        ProcessInstance processInstance = createProcessInstance("oldHash", "newHash", List.of(existingTask, newTask));

        when(taskInstanceRepository.findTaskTypeNamesByProcessInstanceId(processInstance.getId()))
                .thenReturn(Set.of("existing"));

        TaskInstance plannedTask = mock(TaskInstance.class);
        when(taskService.registerNewTaskInUnknownState(eq(processInstance), eq(newTask), any()))
                .thenReturn(Optional.of(plannedTask));

        Optional<List<TaskInstance>> result = migrationService.applyTemplateMigrationIfChanged(processInstance);

        assertThat(result).isPresent();
        assertThat(result.get()).containsExactly(plannedTask);
    }

    @Test
    void applyTemplateMigrationIfChanged_templateChanged_doesNotPlanDynamicTasks() {
        TaskType staticType = staticTask("staticTask");
        TaskType dynamicType = dynamicTask("dynamicTask");
        ProcessInstance processInstance = createProcessInstance("oldHash", "newHash", List.of(staticType, dynamicType));

        when(taskInstanceRepository.findTaskTypeNamesByProcessInstanceId(processInstance.getId()))
                .thenReturn(Set.of());

        TaskInstance plannedTask = mock(TaskInstance.class);
        when(taskService.registerNewTaskInUnknownState(eq(processInstance), eq(staticType), any()))
                .thenReturn(Optional.of(plannedTask));

        Optional<List<TaskInstance>> result = migrationService.applyTemplateMigrationIfChanged(processInstance);

        assertThat(result).isPresent();
        assertThat(result.get()).containsExactly(plannedTask);
        verify(taskService, never()).registerNewTaskInUnknownState(eq(processInstance), eq(dynamicType), any());
    }

    @Test
    void applyTemplateMigrationIfChanged_templateChanged_skipsAlreadyExistingTaskTypes() {
        TaskType task1 = staticTask("task1");
        TaskType task2 = staticTask("task2");
        ProcessInstance processInstance = createProcessInstance("oldHash", "newHash", List.of(task1, task2));

        when(taskInstanceRepository.findTaskTypeNamesByProcessInstanceId(processInstance.getId()))
                .thenReturn(Set.of("task1", "task2"));

        Optional<List<TaskInstance>> result = migrationService.applyTemplateMigrationIfChanged(processInstance);

        assertThat(result).isPresent();
        assertThat(result.get()).isEmpty();
        verify(taskService, never()).registerNewTaskInUnknownState(any(), any(), any());
    }

    @Test
    void applyTemplateMigrationIfChanged_templateChanged_updatesTemplateHash() {
        TaskType task1 = staticTask("task1");
        ProcessInstance processInstance = createProcessInstance("oldHash", "newHash", List.of(task1));

        when(taskInstanceRepository.findTaskTypeNamesByProcessInstanceId(processInstance.getId()))
                .thenReturn(Set.of("task1"));

        migrationService.applyTemplateMigrationIfChanged(processInstance);

        assertThat(processInstance.getProcessTemplateHash()).isEqualTo("newHash");
    }

    @Test
    void applyTemplateMigrationIfChanged_saveIfNewReturnsFalse_taskNotInResult() {
        TaskType newTask = staticTask("newTask");
        ProcessInstance processInstance = createProcessInstance("oldHash", "newHash", List.of(newTask));

        when(taskInstanceRepository.findTaskTypeNamesByProcessInstanceId(processInstance.getId()))
                .thenReturn(Set.of());
        when(taskService.registerNewTaskInUnknownState(eq(processInstance), eq(newTask), any()))
                .thenReturn(Optional.empty());

        Optional<List<TaskInstance>> result = migrationService.applyTemplateMigrationIfChanged(processInstance);

        assertThat(result).isPresent();
        assertThat(result.get()).isEmpty();
    }

    private ProcessInstance createProcessInstance(String storedHash, String templateHash, List<TaskType> taskTypes) {
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name("template")
                .templateHash(templateHash)
                .taskTypes(taskTypes)
                .build();
        ProcessContextRepositoryFacadeStub repositoryFacade = new ProcessContextRepositoryFacadeStub();
        ProcessInstance processInstance = ProcessInstance.createProcessInstance("origin-1", processTemplate, new ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextFactory(repositoryFacade));
        // Override the stored hash to simulate a previously stored different hash
        setField(processInstance, "processTemplateHash", storedHash);
        return processInstance;
    }

    private TaskType staticTask(String name) {
        return TaskType.builder()
                .name(name)
                .lifecycle(TaskLifecycle.STATIC)
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .build();
    }

    private TaskType dynamicTask(String name) {
        return TaskType.builder()
                .name(name)
                .lifecycle(TaskLifecycle.DYNAMIC)
                .cardinality(TaskCardinality.MULTI_INSTANCE)
                .plannedByDomainEvent("someEvent")
                .build();
    }
}
