package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.plugin.api.condition.MilestoneCondition;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessState;
import ch.admin.bit.jeap.processcontext.plugin.api.context.TaskState;
import ch.admin.bit.jeap.processcontext.plugin.api.context.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MilestoneTest {

    private static final ProcessContext processContext = ProcessContext.builder()
            .originProcessId("id")
            .processName("name")
            .processState(ProcessState.STARTED)
            .tasks(List.of())
            .messages(List.of())
            .build();
    private static final ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleDynamicTaskInstance();

    @Mock
    private MilestoneCondition milestoneCondition;

    @Test
    void evaluateIfReached_shouldSetReachedToTrueWhenConditionIsMet() {
        Milestone milestone = Milestone.createNew("name", milestoneCondition, processInstance);
        doReturn(true).when(milestoneCondition).isMilestoneReached(processContext);

        milestone.evaluateIfReached(processContext);

        assertTrue(milestone.isReached());
    }

    @Test
    void evaluateIfReached_shouldNotEvaluateConditionIfAlreadyReached() {
        Milestone milestone = Milestone.createNew("name", milestoneCondition, processInstance);
        doReturn(true).when(milestoneCondition).isMilestoneReached(processContext);

        milestone.evaluateIfReached(processContext);
        assertTrue(milestone.isReached());

        milestone.evaluateIfReached(processContext);
        verify(milestoneCondition, times(1)).isMilestoneReached(processContext);
    }

    @Test
    void setMilestoneConditionFromTemplate() throws Exception {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithTwoTasksAndThreeMilestoneConditions();
        ProcessContext processContext = ProcessContext.builder()
                .originProcessId("id")
                .processName("name")
                .processState(ProcessState.STARTED)
                .messages(List.of())
                .tasks(List.of(Task.builder()
                        .type(TaskType.builder()
                                .name(ProcessInstanceStubs.task1)
                                .lifecycle(TaskLifecycle.STATIC)
                                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                                .build())
                        .state(TaskState.COMPLETED)
                        .id("id")
                        .build()))
                .build();
        Milestone milestone = Milestone.createNew(ProcessInstanceStubs.milestone1, milestoneCondition, processInstance);
        setMilestoneConditionToNull(milestone);

        milestone.setMilestoneConditionFromTemplate(processInstance.getProcessTemplate());

        milestone.evaluateIfReached(processContext);
        assertTrue(milestone.isReached());
    }

    @Test
    void setMilestoneConditionFromTemplate_shouldThrowIfAlreadySet() {
        Milestone milestone = Milestone.createNew("name", milestoneCondition, processInstance);

        assertThrows(IllegalStateException.class, () ->
                milestone.setMilestoneConditionFromTemplate(processInstance.getProcessTemplate()));
    }

    @Test
    void setMilestoneConditionFromTemplate_shouldNotThrowIfMilestoneNotFound() throws Exception {
        Milestone milestone = Milestone.createNew("notfound", milestoneCondition, processInstance);
        setMilestoneConditionToNull(milestone);

        milestone.setMilestoneConditionFromTemplate(processInstance.getProcessTemplate());

        assertThat(ReflectionTestUtils.getField(milestone, "condition")).isNull();

    }

    @Test
    void newMilestone_milestoneIsUnknown(){
        Milestone milestone = Milestone.createNewUnknown("test", null, processInstance);
        assertThat(milestone.getState()).isEqualTo(MilestoneState.UNKNOWN);
    }


    /**
     * Simulate empty condition after loading the domain object from persistent state
     */
    private static void setMilestoneConditionToNull(Milestone milestone) throws Exception {
        Field taskType = milestone.getClass().getDeclaredField("condition");
        taskType.setAccessible(true);
        taskType.set(milestone, null);
    }
}
