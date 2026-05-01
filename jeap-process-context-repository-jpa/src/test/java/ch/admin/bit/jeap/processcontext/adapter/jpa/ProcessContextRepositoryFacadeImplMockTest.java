package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.TaskState;
import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextFactory;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessContextRepositoryFacadeImplMockTest {

    @InjectMocks
    private ProcessContextRepositoryFacadeImpl facade;

    @Mock
    private ProcessInstanceJpaRepository processInstanceJpaRepository;

    @MockitoBean
    private MessageJpaRepository messageJpaRepository;

    @MockitoBean
    private MessageSearchJpaRepository messageSearchJpaRepository;

    @MockitoBean
    private ProcessTemplateRepository processTemplateRepository;

    @MockitoBean
    private ProcessContextFactory processContextFactory;


    @Test
    void areAllTasksInFinalState_singleCompletedTask_expectTrue() {
        when(processInstanceJpaRepository.getAllTasksStates(any())).thenReturn(List.of(TaskState.COMPLETED));

        boolean result = facade.areAllTasksInFinalState(UUID.randomUUID());

        assertThat(result)
                .isTrue();
    }

    @Test
    void areAllTasksInFinalState_twoPlannedTasks_expectFalse() {
        when(processInstanceJpaRepository.getAllTasksStates(any())).thenReturn(List.of(TaskState.PLANNED));

        boolean result = facade.areAllTasksInFinalState(UUID.randomUUID());

        assertThat(result)
                .isFalse();
    }

    @Test
    void areAllTasksInFinalState_twoTasksPlannedAndCompleted_expectFalse() {
        when(processInstanceJpaRepository.getAllTasksStates(any())).thenReturn(List.of(TaskState.COMPLETED, TaskState.PLANNED));

        boolean result = facade.areAllTasksInFinalState(UUID.randomUUID());

        assertThat(result)
                .isFalse();
    }

    @Test
    void areAllTasksInFinalState_twoFinalStateTasks_expectTrue() {
        when(processInstanceJpaRepository.getAllTasksStates(any())).thenReturn(List.of(TaskState.NOT_REQUIRED, TaskState.DELETED));

        boolean result = facade.areAllTasksInFinalState(UUID.randomUUID());

        assertThat(result)
                .isTrue();
    }

    @Test
    void areAllTasksInFinalState_noTasksExpectFalse() {
        when(processInstanceJpaRepository.getAllTasksStates(any())).thenReturn(List.of());

        boolean result = facade.areAllTasksInFinalState(UUID.randomUUID());

        assertThat(result)
                .isFalse();
    }
}
