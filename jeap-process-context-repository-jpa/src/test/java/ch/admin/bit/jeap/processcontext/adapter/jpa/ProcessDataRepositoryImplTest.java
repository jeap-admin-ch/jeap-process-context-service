package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessData;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessDataRepositoryImplTest {

    @Mock
    private ProcessDataJpaRepository processDataJpaRepository;

    @Mock
    private ProcessInstance processInstance;

    private ProcessDataRepositoryImpl processDataRepository;

    @BeforeEach
    void setUp() {
        processDataRepository = new ProcessDataRepositoryImpl(processDataJpaRepository);
    }

    @Test
    void findProcessData_withRole_usesKeyAndRoleQuery() {
        String key = "testKey";
        String role = "testRole";
        ProcessData processData = new ProcessData(key, "value", role);
        when(processDataJpaRepository.findByProcessInstanceAndKeyAndRole(processInstance, key, role))
                .thenReturn(List.of(processData));

        List<ProcessData> result = processDataRepository.findProcessData(processInstance, key, role);

        assertThat(result).containsExactly(processData);
        verify(processDataJpaRepository).findByProcessInstanceAndKeyAndRole(processInstance, key, role);
        verify(processDataJpaRepository, never()).findByProcessInstanceAndKey(any(), any());
    }

    @Test
    void findProcessData_withoutRole_usesKeyOnlyQuery() {
        String key = "testKey";
        ProcessData processData = new ProcessData(key, "value");
        when(processDataJpaRepository.findByProcessInstanceAndKey(processInstance, key))
                .thenReturn(List.of(processData));

        List<ProcessData> result = processDataRepository.findProcessData(processInstance, key, null);

        assertThat(result).containsExactly(processData);
        verify(processDataJpaRepository).findByProcessInstanceAndKey(processInstance, key);
        verify(processDataJpaRepository, never()).findByProcessInstanceAndKeyAndRole(any(), any(), any());
    }

    @Test
    void findProcessData_noMatches_returnsEmptyList() {
        String key = "testKey";
        when(processDataJpaRepository.findByProcessInstanceAndKey(processInstance, key))
                .thenReturn(List.of());

        List<ProcessData> result = processDataRepository.findProcessData(processInstance, key, null);

        assertThat(result).isEmpty();
    }

    @Test
    void findProcessData_multipleMatches_returnsAll() {
        String key = "testKey";
        ProcessData processData1 = new ProcessData(key, "value1");
        ProcessData processData2 = new ProcessData(key, "value2");
        when(processDataJpaRepository.findByProcessInstanceAndKey(processInstance, key))
                .thenReturn(List.of(processData1, processData2));

        List<ProcessData> result = processDataRepository.findProcessData(processInstance, key, null);

        assertThat(result).containsExactly(processData1, processData2);
    }
}
