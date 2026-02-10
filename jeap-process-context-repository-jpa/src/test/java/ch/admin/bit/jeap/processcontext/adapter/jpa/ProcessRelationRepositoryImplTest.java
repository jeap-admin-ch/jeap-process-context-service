package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessRelation;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessRelationRoleType;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessRelationRoleVisibility;
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
class ProcessRelationRepositoryImplTest {

    @Mock
    private ProcessRelationJpaRepository processRelationJpaRepository;

    @Mock
    private ProcessRelation processRelation;

    private ProcessRelationRepositoryImpl processRelationRepository;

    @BeforeEach
    void setUp() {
        processRelationRepository = new ProcessRelationRepositoryImpl(processRelationJpaRepository);
    }

    @Test
    void findAllByRelatedProcessId_delegatesToJpaRepository() {
        String processId = "test-process-id";
        when(processRelationJpaRepository.findAllByRelatedProcessId(processId))
                .thenReturn(List.of(processRelation));

        List<ProcessRelation> result = processRelationRepository.findAllByRelatedProcessId(processId);

        assertThat(result).containsExactly(processRelation);
        verify(processRelationJpaRepository).findAllByRelatedProcessId(processId);
    }

    @Test
    void findAllByProcessInstanceId_delegatesToJpaRepository() {
        UUID processInstanceId = UUID.randomUUID();
        when(processRelationJpaRepository.findAllByProcessInstanceId(processInstanceId))
                .thenReturn(List.of(processRelation));

        List<ProcessRelation> result = processRelationRepository.findAllByProcessInstanceId(processInstanceId);

        assertThat(result).containsExactly(processRelation);
        verify(processRelationJpaRepository).findAllByProcessInstanceId(processInstanceId);
    }

    @Test
    void exists_delegatesToJpaRepository() {
        UUID processInstanceId = UUID.randomUUID();
        when(processRelation.getName()).thenReturn("name");
        when(processRelation.getRoleType()).thenReturn(ProcessRelationRoleType.ORIGIN);
        when(processRelation.getOriginRole()).thenReturn("originRole");
        when(processRelation.getTargetRole()).thenReturn("targetRole");
        when(processRelation.getVisibilityType()).thenReturn(ProcessRelationRoleVisibility.BOTH);
        when(processRelation.getRelatedProcessId()).thenReturn("relatedId");
        when(processRelationJpaRepository.existsByProcessInstance_IdAndNameAndRoleTypeAndOriginRoleAndTargetRoleAndVisibilityTypeAndRelatedProcessId(processInstanceId, "name",
                ProcessRelationRoleType.ORIGIN, "originRole", "targetRole",
                ProcessRelationRoleVisibility.BOTH, "relatedId"))
                .thenReturn(true);

        boolean result = processRelationRepository.exists(processInstanceId, processRelation);

        assertThat(result).isTrue();
        verify(processRelationJpaRepository).existsByProcessInstance_IdAndNameAndRoleTypeAndOriginRoleAndTargetRoleAndVisibilityTypeAndRelatedProcessId(processInstanceId, "name",
                ProcessRelationRoleType.ORIGIN, "originRole", "targetRole",
                ProcessRelationRoleVisibility.BOTH, "relatedId");
    }

    @Test
    void saveAll_delegatesToJpaRepository() {
        List<ProcessRelation> relations = List.of(processRelation);
        when(processRelationJpaRepository.saveAll(relations))
                .thenReturn(relations);

        List<ProcessRelation> result = processRelationRepository.saveAll(relations);

        assertThat(result).containsExactly(processRelation);
        verify(processRelationJpaRepository).saveAll(relations);
    }
}
