package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class TaskInstanceTest {

    @Test
    void createTaskInstanceWithOriginTaskId_optional() {
        ProcessInstance process = ProcessInstanceStubs.createProcessWithSingleAndDynamicTasks();
        TaskType dynamicTaskType = process.getProcessTemplate().getTaskTypeByName(ProcessInstanceStubs.dynamicTaskName).orElseThrow();
        final String originTaskId = "origin-task-id";
        final ZonedDateTime timestamp = ZonedDateTime.now();
        final UUID messageId = UUID.randomUUID();

        TaskInstance taskInstance = TaskInstance.createTaskInstanceWithOriginTaskId(dynamicTaskType, process, originTaskId, timestamp, messageId);

        assertEquals(TaskState.PLANNED, taskInstance.getState());
        assertEquals(process, taskInstance.getProcessInstance());
        assertEquals("origin-task-id", taskInstance.getOriginTaskId());
        assertEquals(messageId, taskInstance.getPlannedBy());
        assertEquals(timestamp, taskInstance.getPlannedAt());
        assertNull(taskInstance.getCompletedAt());
    }

    @Test
    void createInitialTask_mandatory() {
        ProcessInstance process = ProcessInstanceStubs.createProcessWithSingleAndDynamicTasks();
        TaskType mandatoryTaskType = process.getProcessTemplate().getTaskTypeByName(ProcessInstanceStubs.singleTaskName).orElseThrow();
        final ZonedDateTime timestamp = ZonedDateTime.now();

        TaskInstance taskInstance = TaskInstance.createInitialTaskInstance(mandatoryTaskType, process, timestamp);

        assertEquals(TaskState.PLANNED, taskInstance.getState());
        assertEquals(process, taskInstance.getProcessInstance());
        assertNull(taskInstance.getOriginTaskId());
        assertThat(taskInstance.getPlannedAt()).isEqualTo(timestamp);
        assertNull(taskInstance.getCompletedAt());
    }

    @Test
    void setTaskTypeFromTemplate() throws Exception {
        ProcessInstance process = ProcessInstanceStubs.createProcessWithSingleDynamicTask();
        ProcessTemplate processTemplate = process.getProcessTemplate();
        TaskType taskType = processTemplate.getTaskTypeByName(ProcessInstanceStubs.singleTaskName).orElseThrow();
        TaskInstance taskInstance = TaskInstance.createInitialTaskInstance(taskType, process, ZonedDateTime.now());
        setTaskTypeToOptionalEmpty(taskInstance);

        taskInstance.setTaskTypeFromTemplate(processTemplate);

        assertSame(taskType, taskInstance.requireTaskType());
    }

    @Test
    void notRequired() {
        ProcessInstance process = ProcessInstanceStubs.createProcessWithSingleAndDynamicTasks();
        TaskType taskType = process.getProcessTemplate().getTaskTypeByName(ProcessInstanceStubs.singleTaskName).orElseThrow();
        TaskInstance taskInstance = TaskInstance.createInitialTaskInstance(taskType, process, ZonedDateTime.now());

        taskInstance.notRequired();

        assertEquals(TaskState.NOT_REQUIRED, taskInstance.getState());
    }

    /**
     * Simulate empty task type after loading the domain object from persistent state
     */
    private static void setTaskTypeToOptionalEmpty(TaskInstance taskInstance) throws Exception {
        Field taskTypeField = taskInstance.getClass().getDeclaredField("taskType");
        taskTypeField.setAccessible(true);
        taskTypeField.set(taskInstance, Optional.empty());
        assertNotNull(taskInstance.getTaskType());
        assertTrue(taskInstance.getTaskType().isEmpty());
    }
}
