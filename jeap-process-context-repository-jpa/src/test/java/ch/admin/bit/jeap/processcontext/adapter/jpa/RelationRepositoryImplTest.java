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
import static org.mockito.Mockito.*;

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
    void saveAll_newRelations_savesAllAndReturnsAll() {
        Relation relation1 = Relation.builder()
                .processInstance(processInstance)
                .systemId("system")
                .subjectType("SubjectType")
                .subjectId("subject-1")
                .objectType("ObjectType")
                .objectId("object-1")
                .predicateType("relates-to")
                .build();

        Relation relation2 = Relation.builder()
                .processInstance(processInstance)
                .systemId("system")
                .subjectType("SubjectType")
                .subjectId("subject-2")
                .objectType("ObjectType")
                .objectId("object-2")
                .predicateType("relates-to")
                .build();

        when(relationJpaRepository.existsByProcessInstanceAndSubjectTypeAndSubjectIdAndObjectTypeAndObjectIdAndPredicateType(
                processInstance, "SubjectType", "subject-1", "ObjectType", "object-1", "relates-to"))
                .thenReturn(false);
        when(relationJpaRepository.existsByProcessInstanceAndSubjectTypeAndSubjectIdAndObjectTypeAndObjectIdAndPredicateType(
                processInstance, "SubjectType", "subject-2", "ObjectType", "object-2", "relates-to"))
                .thenReturn(false);

        Set<Relation> relations = Set.of(relation1, relation2);

        Set<Relation> result = relationRepository.saveAll(relations);

        assertThat(result).containsExactlyInAnyOrder(relation1, relation2);
        verify(relationJpaRepository).saveAll(result);
    }

    @Test
    void saveAll_someRelationsExist_savesOnlyNewRelations() {
        Relation existingRelation = Relation.builder()
                .processInstance(processInstance)
                .systemId("system")
                .subjectType("SubjectType")
                .subjectId("subject-1")
                .objectType("ObjectType")
                .objectId("object-1")
                .predicateType("relates-to")
                .build();

        Relation newRelation = Relation.builder()
                .processInstance(processInstance)
                .systemId("system")
                .subjectType("SubjectType")
                .subjectId("subject-2")
                .objectType("ObjectType")
                .objectId("object-2")
                .predicateType("relates-to")
                .build();

        when(relationJpaRepository.existsByProcessInstanceAndSubjectTypeAndSubjectIdAndObjectTypeAndObjectIdAndPredicateType(
                processInstance, "SubjectType", "subject-1", "ObjectType", "object-1", "relates-to"))
                .thenReturn(true);
        when(relationJpaRepository.existsByProcessInstanceAndSubjectTypeAndSubjectIdAndObjectTypeAndObjectIdAndPredicateType(
                processInstance, "SubjectType", "subject-2", "ObjectType", "object-2", "relates-to"))
                .thenReturn(false);

        Set<Relation> relations = Set.of(existingRelation, newRelation);

        Set<Relation> result = relationRepository.saveAll(relations);

        assertThat(result).containsExactly(newRelation);
        verify(relationJpaRepository).saveAll(Set.of(newRelation));
    }

    @Test
    void saveAll_allRelationsExist_savesNothingAndReturnsEmpty() {
        Relation existingRelation = Relation.builder()
                .processInstance(processInstance)
                .systemId("system")
                .subjectType("SubjectType")
                .subjectId("subject-1")
                .objectType("ObjectType")
                .objectId("object-1")
                .predicateType("relates-to")
                .build();

        when(relationJpaRepository.existsByProcessInstanceAndSubjectTypeAndSubjectIdAndObjectTypeAndObjectIdAndPredicateType(
                processInstance, "SubjectType", "subject-1", "ObjectType", "object-1", "relates-to"))
                .thenReturn(true);

        Set<Relation> relations = Set.of(existingRelation);

        Set<Relation> result = relationRepository.saveAll(relations);

        assertThat(result).isEmpty();
        verify(relationJpaRepository, never()).saveAll(any());
    }

    @Test
    void saveAll_emptyCollection_returnsEmptyAndDoesNotSave() {
        List<Relation> relations = List.of();

        Set<Relation> result = relationRepository.saveAll(relations);

        assertThat(result).isEmpty();
        verify(relationJpaRepository, never()).saveAll(any());
    }

    @Test
    void findByProcessInstance_delegatesToJpaRepository() {
        Relation relation = Relation.builder()
                .processInstance(processInstance)
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
