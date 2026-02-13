package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.Relation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
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
        lenient().when(processInstance.getId()).thenReturn(UUID.randomUUID());
    }

    @Test
    void saveAll_newRelations_savesAllAndReturnsAllNewRelations() {
        Relation relation1 = createRelation("subject-1", "object-1");
        Relation relation2 = createRelation("subject-2", "object-2");

        when(relationJpaRepository.findAll(any(Specification.class))).thenReturn(List.of());

        Set<Relation> relations = Set.of(relation1, relation2);

        Set<Relation> result = relationRepository.saveAllNewRelations(relations);

        assertThat(result).containsExactlyInAnyOrder(relation1, relation2);
        verify(relationJpaRepository).findAll(any(Specification.class));
        verify(relationJpaRepository).saveAll(result);
    }

    @Test
    void saveAll_NewRelations_someRelationsExist_savesOnlyNewRelations() {
        Relation existingRelation = createRelation("subject-1", "object-1");
        Relation newRelation = createRelation("subject-2", "object-2");

        // Simulate that existingRelation is already in the database
        Relation persistedExisting = createRelation("subject-1", "object-1");
        when(relationJpaRepository.findAll(any(Specification.class))).thenReturn(List.of(persistedExisting));

        Set<Relation> relations = Set.of(existingRelation, newRelation);

        Set<Relation> result = relationRepository.saveAllNewRelations(relations);

        assertThat(result).containsExactly(newRelation);
        verify(relationJpaRepository).saveAll(Set.of(newRelation));
    }

    @Test
    void saveAll_allNewRelationsRelationsExist_savesNothingAndReturnsEmpty() {
        Relation existingRelation = createRelation("subject-1", "object-1");

        // Simulate that existingRelation is already in the database
        Relation persistedExisting = createRelation("subject-1", "object-1");
        when(relationJpaRepository.findAll(any(Specification.class))).thenReturn(List.of(persistedExisting));

        Set<Relation> relations = Set.of(existingRelation);

        Set<Relation> result = relationRepository.saveAllNewRelations(relations);

        assertThat(result).isEmpty();
        verify(relationJpaRepository, never()).saveAll(any());
    }

    @Test
    void saveAll_NewRelations_emptyCollection_returnsEmptyAndDoesNotQuery() {
        Set<Relation> result = relationRepository.saveAllNewRelations(Set.of());

        assertThat(result).isEmpty();
        verify(relationJpaRepository, never()).findAll(any(Specification.class));
        verify(relationJpaRepository, never()).saveAll(any());
    }

    @Test
    void saveAll_NewRelations_largeCollection_processesBatches() {
        // Create more relations than BATCH_SIZE
        List<Relation> relations = new ArrayList<>();
        for (int i = 0; i < RelationRepositoryImpl.BATCH_SIZE + 50; i++) {
            relations.add(createRelation("subject-" + i, "object-" + i));
        }

        when(relationJpaRepository.findAll(any(Specification.class))).thenReturn(List.of());

        Set<Relation> result = relationRepository.saveAllNewRelations(relations);

        assertThat(result).hasSize(RelationRepositoryImpl.BATCH_SIZE + 50);
        // Should have queried twice: once for first batch, once for remaining
        verify(relationJpaRepository, times(2)).findAll(any(Specification.class));
        verify(relationJpaRepository).saveAll(result);
    }

    @Test
    void saveAll_NewRelations_largeCollectionWithSomeExisting_filtersCorrectlyAcrossBatches() {
        // Create more relations than BATCH_SIZE
        List<Relation> relations = new ArrayList<>();
        for (int i = 0; i < RelationRepositoryImpl.BATCH_SIZE + 50; i++) {
            relations.add(createRelation("subject-" + i, "object-" + i));
        }

        // Simulate first relation in each batch already exists
        Relation existingInFirstBatch = createRelation("subject-0", "object-0");
        Relation existingInSecondBatch = createRelation("subject-" + RelationRepositoryImpl.BATCH_SIZE, "object-" + RelationRepositoryImpl.BATCH_SIZE);

        when(relationJpaRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(existingInFirstBatch))
                .thenReturn(List.of(existingInSecondBatch));

        Set<Relation> result = relationRepository.saveAllNewRelations(relations);

        // Total minus 2 existing relations
        assertThat(result).hasSize(RelationRepositoryImpl.BATCH_SIZE + 50 - 2);
        verify(relationJpaRepository, times(2)).findAll(any(Specification.class));
    }

    private Relation createRelation(String subjectId, String objectId) {
        return Relation.builder()
                .processInstance(processInstance)
                .systemId("system")
                .subjectType("SubjectType")
                .subjectId(subjectId)
                .objectType("ObjectType")
                .objectId(objectId)
                .predicateType("relates-to")
                .build();
    }
}
