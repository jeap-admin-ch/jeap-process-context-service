package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TaskUtilsTest {

    @Test
    void taskTypesWithoutTaskInstance_empty() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleAndDynamicTaskInstances();
        List<TaskInstance> tasks = processInstance.getTasks();

        Set<TaskType> taskTypes = TaskUtils.taskTypesWithoutTaskInstance(tasks, processInstance.getProcessTemplate());

        assertEquals(Set.of(), taskTypes);
    }

    @Test
    void taskTypesWithoutTaskInstance_nonEmpty() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleAndDynamicInstanceTasksBothNotYetInstantiated();

        List<TaskType> taskTypes = new ArrayList<>(TaskUtils.taskTypesWithoutTaskInstance(List.of(), processInstance.getProcessTemplate()));

        assertEquals(processInstance.getProcessTemplate().getTaskTypes(), taskTypes);
    }

}
