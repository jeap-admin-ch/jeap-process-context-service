package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.Relation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelationRepositoryImplTest {

    @Mock
    private RelationJpaRepository relationJpaRepository;

    @Mock
    private ProcessInstance processInstance;

    private RelationRepositoryImpl relationRepository;

    @BeforeEach
    void setUp() {
        relationRepository = new RelationRepositoryImpl(relationJpaRepository);
    }

    @Test
    void saveAll_delegatesToJpaRepository() {
        Relation relation1 = Relation.builder()
                .systemId("system")
                .subjectType("SubjectType")
                .subjectId("subject-1")
                .objectType("ObjectType")
                .objectId("object-1")
                .predicateType("relates-to")
                .build();

        Relation relation2 = Relation.builder()
                .systemId("system")
                .subjectType("SubjectType")
                .subjectId("subject-2")
                .objectType("ObjectType")
                .objectId("object-2")
                .predicateType("relates-to")
                .build();

        Set<Relation> relations = Set.of(relation1, relation2);

        relationRepository.saveAll(relations);

        verify(relationJpaRepository).saveAll(relations);
    }

    @Test
    void saveAll_emptyCollection_delegatesToJpaRepository() {
        List<Relation> relations = List.of();

        relationRepository.saveAll(relations);

        verify(relationJpaRepository).saveAll(relations);
    }

    @Test
    void findByProcessInstance_delegatesToJpaRepository() {
        Relation relation = Relation.builder()
                .systemId("system")
                .subjectType("SubjectType")
                .subjectId("subject-1")
                .objectType("ObjectType")
                .objectId("object-1")
                .predicateType("relates-to")
                .build();
        when(relationJpaRepository.findByProcessInstance(processInstance)).thenReturn(Set.of(relation));

        Set<Relation> result = relationRepository.findByProcessInstance(processInstance);

        assertThat(result).containsExactly(relation);
        verify(relationJpaRepository).findByProcessInstance(processInstance);
    }

    @Test
    void findByProcessInstance_emptyResult_returnsEmptySet() {
        when(relationJpaRepository.findByProcessInstance(processInstance)).thenReturn(Set.of());

        Set<Relation> result = relationRepository.findByProcessInstance(processInstance);

        assertThat(result).isEmpty();
        verify(relationJpaRepository).findByProcessInstance(processInstance);
    }
}
