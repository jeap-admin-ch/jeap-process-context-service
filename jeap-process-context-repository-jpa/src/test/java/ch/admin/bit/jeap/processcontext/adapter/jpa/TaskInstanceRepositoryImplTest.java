package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.TaskInstance;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskInstanceRepositoryImplTest {

    @Mock
    private TaskInstanceJpaRepository taskInstanceJpaRepository;
    @Mock
    private TaskInstance taskInstance1;
    @Mock
    private TaskInstance taskInstance2;
    @Mock
    private ProcessTemplate processTemplate;

    private TaskInstanceRepositoryImpl taskInstanceRepository;

    @BeforeEach
    void setUp() {
        taskInstanceRepository = new TaskInstanceRepositoryImpl(taskInstanceJpaRepository);
    }

    @Test
    void findByProcessInstanceId_delegatesToJpaRepository() {
        UUID processInstanceId = UUID.randomUUID();
        when(taskInstanceJpaRepository.findByProcessInstanceId(processInstanceId))
                .thenReturn(List.of(taskInstance1));

        List<TaskInstance> result = taskInstanceRepository.findByProcessInstanceId(processTemplate, processInstanceId);

        assertThat(result).containsExactly(taskInstance1);
        verify(taskInstanceJpaRepository).findByProcessInstanceId(processInstanceId);
    }

    @Test
    void findByProcessInstanceId_noResults_returnsEmptyList() {
        UUID processInstanceId = UUID.randomUUID();
        when(taskInstanceJpaRepository.findByProcessInstanceId(processInstanceId))
                .thenReturn(List.of());

        List<TaskInstance> result = taskInstanceRepository.findByProcessInstanceId(processTemplate, processInstanceId);

        assertThat(result).isEmpty();
    }

    @Test
    void findByProcessInstanceId_multipleResults_returnsAll() {
        UUID processInstanceId = UUID.randomUUID();
        when(taskInstanceJpaRepository.findByProcessInstanceId(processInstanceId))
                .thenReturn(List.of(taskInstance1, taskInstance2));

        List<TaskInstance> result = taskInstanceRepository.findByProcessInstanceId(processTemplate, processInstanceId);

        assertThat(result).containsExactly(taskInstance1, taskInstance2);
    }

    @Test
    void save_delegatesToJpaRepository() {
        when(taskInstanceJpaRepository.save(taskInstance1)).thenReturn(taskInstance1);

        assertThat(taskInstanceRepository.save(taskInstance1)).isSameAs(taskInstance1);
        verify(taskInstanceJpaRepository).save(taskInstance1);
    }

    @Test
    void existsByProcessInstanceIdAndTaskTypeNameAndOriginTaskId_delegatesToJpaRepository() {
        UUID processInstanceId = UUID.randomUUID();
        when(taskInstanceJpaRepository.existsByProcessInstance_IdAndTaskTypeNameAndOriginTaskId(processInstanceId, "myTask", "origin-1"))
                .thenReturn(true);

        assertThat(taskInstanceRepository.existsByProcessInstanceIdAndTaskTypeNameAndOriginTaskId(processInstanceId, "myTask", "origin-1"))
                .isTrue();
    }
}
