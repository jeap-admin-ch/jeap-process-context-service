package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.*;
import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextFactory;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = JpaAdapterConfig.class)
class TaskInstanceJpaRepositoryTest {

    @PersistenceContext
    EntityManager entityManager;

    @MockitoBean
    private ProcessTemplateRepository processTemplateRepository;

    @MockitoBean
    private ProcessContextFactory processContextFactory;

    @Autowired
    private TaskInstanceJpaRepository taskInstanceJpaRepository;

    @Autowired
    private TaskInstanceRepository taskInstanceRepository;

    @Autowired
    private ProcessInstanceJpaRepository processInstanceJpaRepository;

    @Test
    void findByProcessInstanceId_singleTask_returnsTask() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);
        createAndSaveTask(ProcessInstanceStubs.task, "origin-1", TaskState.PLANNED, savedProcessInstance);
        entityManager.flush();
        entityManager.clear();

        List<TaskInstance> result = taskInstanceJpaRepository.findByProcessInstanceId(savedProcessInstance.getId());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTaskTypeName()).isEqualTo(ProcessInstanceStubs.task);
    }

    @Test
    void findByProcessInstanceId_twoTasks_returnsBoth() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithTwoPlannedTaskInstances();
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);
        createAndSaveTask(ProcessInstanceStubs.task, "origin-1", TaskState.PLANNED, savedProcessInstance);
        createAndSaveTask(ProcessInstanceStubs.task2, "origin-2", TaskState.PLANNED, savedProcessInstance);
        entityManager.flush();
        entityManager.clear();

        List<TaskInstance> result = taskInstanceJpaRepository.findByProcessInstanceId(savedProcessInstance.getId());

        assertThat(result).hasSize(2);
        assertThat(result).extracting(TaskInstance::getTaskTypeName)
                .containsExactlyInAnyOrder(ProcessInstanceStubs.task, ProcessInstanceStubs.task2);
    }

    @Test
    void findByProcessInstanceId_noTasks_returnsEmptyList() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithoutTaskInstance();
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);
        entityManager.detach(savedProcessInstance);

        List<TaskInstance> result = taskInstanceJpaRepository.findByProcessInstanceId(savedProcessInstance.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void findByProcessInstanceId_nonExistentProcessInstanceId_returnsEmptyList() {
        List<TaskInstance> result = taskInstanceJpaRepository.findByProcessInstanceId(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    @Test
    void findByProcessInstanceId_multipleProcessInstances_returnsOnlyMatchingTasks() {
        ProcessInstance processInstance1 = ProcessInstanceStubs.createProcessWithTwoPlannedTaskInstances();
        ProcessInstance savedProcessInstance1 = processInstanceJpaRepository.saveAndFlush(processInstance1);
        createAndSaveTask(ProcessInstanceStubs.task, "origin-1", TaskState.PLANNED, savedProcessInstance1);
        createAndSaveTask(ProcessInstanceStubs.task2, "origin-2", TaskState.PLANNED, savedProcessInstance1);

        ProcessInstance processInstance2 = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        ProcessInstance savedProcessInstance2 = processInstanceJpaRepository.saveAndFlush(processInstance2);
        createAndSaveTask(ProcessInstanceStubs.task, "origin-3", TaskState.PLANNED, savedProcessInstance2);

        entityManager.flush();
        entityManager.clear();

        List<TaskInstance> result1 = taskInstanceJpaRepository.findByProcessInstanceId(savedProcessInstance1.getId());
        List<TaskInstance> result2 = taskInstanceJpaRepository.findByProcessInstanceId(savedProcessInstance2.getId());

        assertThat(result1).hasSize(2);
        assertThat(result2).hasSize(1);
    }

    @Test
    void findByProcessInstanceId_completedTasks_returnsTasksWithCorrectState() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithTwoPlannedTaskInstances();
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);
        createAndSaveTask(ProcessInstanceStubs.task, "origin-1", TaskState.COMPLETED, savedProcessInstance);
        createAndSaveTask(ProcessInstanceStubs.task2, "origin-2", TaskState.COMPLETED, savedProcessInstance);
        entityManager.flush();
        entityManager.clear();

        List<TaskInstance> result = taskInstanceJpaRepository.findByProcessInstanceId(savedProcessInstance.getId());

        assertThat(result)
                .hasSize(2)
                .allSatisfy(task ->
                        assertThat(task.getState().isFinalState()).isTrue()
                );
    }

    @Test
    void existsByProcessInstanceIdAndTaskTypeNameAndOriginTaskId_notFound_returnsFalse() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithoutTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance);

        assertThat(taskInstanceRepository.existsByProcessInstanceIdAndTaskTypeNameAndOriginTaskId(
                processInstance.getId(), "myTask", "origin-1")).isFalse();
    }

    @Test
    void existsByProcessInstanceIdAndTaskTypeNameAndOriginTaskId_found_returnsTrue() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithoutTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance);
        TaskInstance task = createTaskForSaveIfNew("myTask", "origin-1", processInstance);
        taskInstanceRepository.save(task);

        assertThat(taskInstanceRepository.existsByProcessInstanceIdAndTaskTypeNameAndOriginTaskId(
                processInstance.getId(), "myTask", "origin-1")).isTrue();
    }

    @Test
    void existsByProcessInstanceIdAndTaskTypeNameAndOriginTaskId_differentOriginTaskIds_distinguishes() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithoutTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance);
        TaskInstance task1 = createTaskForSaveIfNew("myTask", "origin-1", processInstance);
        TaskInstance task2 = createTaskForSaveIfNew("myTask", "origin-2", processInstance);
        taskInstanceRepository.save(task1);
        taskInstanceRepository.save(task2);

        assertThat(taskInstanceRepository.existsByProcessInstanceIdAndTaskTypeNameAndOriginTaskId(
                processInstance.getId(), "myTask", "origin-1")).isTrue();
        assertThat(taskInstanceRepository.existsByProcessInstanceIdAndTaskTypeNameAndOriginTaskId(
                processInstance.getId(), "myTask", "origin-3")).isFalse();
    }

    @Test
    void existsByProcessInstanceIdAndTaskTypeNameAndOriginTaskId_nullOriginTaskId_found() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithoutTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance);
        TaskInstance task = createTaskForSaveIfNew("myTask", null, processInstance);
        taskInstanceRepository.save(task);

        assertThat(taskInstanceRepository.existsByProcessInstanceIdAndTaskTypeNameAndOriginTaskId(
                processInstance.getId(), "myTask", null)).isTrue();
    }

    @Test
    void save_persistsNewTask() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithoutTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance);
        TaskInstance task = ProcessInstanceStubs.createTaskInstance("savedTask", 0, "origin-save");
        ReflectionTestUtils.setField(task, "processInstance", processInstance);

        taskInstanceRepository.save(task);
        entityManager.flush();
        entityManager.clear();

        List<TaskInstance> tasks = taskInstanceJpaRepository.findByProcessInstanceId(processInstance.getId());
        assertThat(tasks).hasSize(1);
        assertThat(tasks.getFirst().getTaskTypeName()).isEqualTo("savedTask");
        assertThat(tasks.getFirst().getOriginTaskId()).isEqualTo("origin-save");
    }

    @Test
    void deleteRemovedOpenTasks_deletesOpenTasksNotInRemainingSet() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithoutTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance);

        // Create tasks: keepTask is in remaining set, removeTask is not
        createAndSaveTask("keepTask", "origin-keep", TaskState.PLANNED, processInstance);
        createAndSaveTask("removeTask", "origin-remove", TaskState.PLANNED, processInstance);
        entityManager.flush();
        entityManager.clear();

        ZonedDateTime now = ZonedDateTime.now();
        taskInstanceJpaRepository.deleteRemovedOpenTasks(processInstance.getId(), Set.of("keepTask"), now);
        entityManager.flush();
        entityManager.clear();

        List<TaskInstance> tasks = taskInstanceJpaRepository.findByProcessInstanceId(processInstance.getId());
        TaskInstance kept = tasks.stream().filter(t -> t.getTaskTypeName().equals("keepTask")).findFirst().orElseThrow();
        TaskInstance removed = tasks.stream().filter(t -> t.getTaskTypeName().equals("removeTask")).findFirst().orElseThrow();
        assertThat(kept.getState()).isEqualTo(TaskState.PLANNED);
        assertThat(removed.getState()).isEqualTo(TaskState.DELETED);
    }

    @Test
    void deleteRemovedOpenTasks_doesNotAffectFinalStateTasks() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithoutTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance);

        // Tasks in final states should not be touched even if not in remaining set
        createAndSaveTask("completedTask", "origin-1", TaskState.COMPLETED, processInstance);
        createAndSaveTask("notRequiredTask", "origin-2", TaskState.NOT_REQUIRED, processInstance);
        createAndSaveTask("deletedTask", "origin-3", TaskState.DELETED, processInstance);
        entityManager.flush();
        entityManager.clear();

        taskInstanceJpaRepository.deleteRemovedOpenTasks(processInstance.getId(), Set.of("otherTask"), ZonedDateTime.now());
        entityManager.flush();
        entityManager.clear();

        List<TaskInstance> tasks = taskInstanceJpaRepository.findByProcessInstanceId(processInstance.getId());
        assertThat(tasks).extracting(TaskInstance::getState)
                .containsExactlyInAnyOrder(TaskState.COMPLETED, TaskState.NOT_REQUIRED, TaskState.DELETED);
    }

    @Test
    void deleteRemovedOpenTasks_deletesUnknownAndNotPlannedTasks() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithoutTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance);

        createAndSaveTask("unknownTask", "origin-1", TaskState.UNKNOWN, processInstance);
        createAndSaveTask("notPlannedTask", "origin-2", TaskState.NOT_PLANNED, processInstance);
        entityManager.flush();
        entityManager.clear();

        taskInstanceJpaRepository.deleteRemovedOpenTasks(processInstance.getId(), Set.of("otherTask"), ZonedDateTime.now());
        entityManager.flush();
        entityManager.clear();

        List<TaskInstance> tasks = taskInstanceJpaRepository.findByProcessInstanceId(processInstance.getId());
        assertThat(tasks)
                .isNotEmpty()
                .allSatisfy(t -> assertThat(t.getState()).isEqualTo(TaskState.DELETED));
    }

    @Test
    void deleteRemovedOpenTasks_doesNotAffectOtherProcessInstances() {
        ProcessInstance process1 = ProcessInstanceStubs.createProcessWithoutTaskInstance();
        processInstanceJpaRepository.saveAndFlush(process1);
        ProcessInstance process2 = ProcessInstanceStubs.createProcessWithoutTaskInstance();
        processInstanceJpaRepository.saveAndFlush(process2);

        createAndSaveTask("taskA", "origin-1", TaskState.PLANNED, process1);
        createAndSaveTask("taskA", "origin-2", TaskState.PLANNED, process2);
        entityManager.flush();
        entityManager.clear();

        taskInstanceJpaRepository.deleteRemovedOpenTasks(process1.getId(), Set.of("otherTask"), ZonedDateTime.now());
        entityManager.flush();
        entityManager.clear();

        // process1's task should be DELETED, process2's should remain PLANNED
        TaskInstance task1 = taskInstanceJpaRepository.findByProcessInstanceId(process1.getId()).getFirst();
        TaskInstance task2 = taskInstanceJpaRepository.findByProcessInstanceId(process2.getId()).getFirst();
        assertThat(task1.getState()).isEqualTo(TaskState.DELETED);
        assertThat(task2.getState()).isEqualTo(TaskState.PLANNED);
    }

    @Test
    void getTasksOfTypesInNonFinalState_returnsMatchingNonFinalTasks() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithoutTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance);

        createAndSaveTask("taskA", "origin-1", TaskState.PLANNED, processInstance);
        createAndSaveTask("taskB", "origin-2", TaskState.UNKNOWN, processInstance);
        createAndSaveTask("taskC", "origin-3", TaskState.PLANNED, processInstance);
        entityManager.flush();
        entityManager.clear();

        List<TaskInstance> result = taskInstanceJpaRepository.findByTaskTypeNameInAndStateNotFinal(processInstance.getId(), Set.of("taskA", "taskB"));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(TaskInstance::getTaskTypeName)
                .containsExactlyInAnyOrder("taskA", "taskB");
    }

    @Test
    void getTasksOfTypesInNonFinalState_excludesFinalStateTasks() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithoutTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance);

        createAndSaveTask("taskA", "origin-1", TaskState.COMPLETED, processInstance);
        createAndSaveTask("taskA", "origin-2", TaskState.NOT_REQUIRED, processInstance);
        createAndSaveTask("taskA", "origin-3", TaskState.DELETED, processInstance);
        createAndSaveTask("taskA", "origin-4", TaskState.PLANNED, processInstance);
        entityManager.flush();
        entityManager.clear();

        List<TaskInstance> result = taskInstanceJpaRepository.findByTaskTypeNameInAndStateNotFinal(processInstance.getId(), Set.of("taskA"));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getState()).isEqualTo(TaskState.PLANNED);
    }

    @Test
    void getTasksOfTypesInNonFinalState_noMatchingTypes_returnsEmpty() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithoutTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance);

        createAndSaveTask("taskA", "origin-1", TaskState.PLANNED, processInstance);
        entityManager.flush();
        entityManager.clear();

        List<TaskInstance> result = taskInstanceJpaRepository.findByTaskTypeNameInAndStateNotFinal(processInstance.getId(), Set.of("otherTask"));

        assertThat(result).isEmpty();
    }

    private TaskInstance createAndSaveTask(String name, String originTaskId, TaskState state, ProcessInstance processInstance) {
        TaskInstance task = ProcessInstanceStubs.createTaskInstance(name, 0, originTaskId);
        ReflectionTestUtils.setField(task, "processInstance", processInstance);
        ReflectionTestUtils.setField(task, "state", state);
        taskInstanceJpaRepository.save(task);
        return task;
    }

    private TaskInstance createTaskForSaveIfNew(String name, String originTaskId, ProcessInstance processInstance) {
        TaskInstance task = ProcessInstanceStubs.createTaskInstance(name, 0, originTaskId);
        ReflectionTestUtils.setField(task, "processInstance", processInstance);
        return task;
    }
}
