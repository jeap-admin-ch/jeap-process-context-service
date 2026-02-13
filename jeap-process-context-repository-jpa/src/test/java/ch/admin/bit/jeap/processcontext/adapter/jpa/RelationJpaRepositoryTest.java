package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceStubs;
import ch.admin.bit.jeap.processcontext.domain.processinstance.Relation;
import ch.admin.bit.jeap.processcontext.domain.processinstance.RelationRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextFactory;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = JpaAdapterConfig.class)
class RelationJpaRepositoryTest {

    @PersistenceContext
    EntityManager entityManager;

    @MockitoBean
    private ProcessTemplateRepository processTemplateRepository;

    @MockitoBean
    private ProcessContextFactory processContextFactory;

    @Autowired
    private RelationJpaRepository relationJpaRepository;

    @Autowired
    private RelationRepository relationRepository;

    @Autowired
    private ProcessInstanceJpaRepository processInstanceJpaRepository;

    @Test
    void saveAll_persistsRelations() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance);

        Relation relation1 = Relation.builder()
                .processInstance(processInstance)
                .systemId("test-system")
                .subjectType("SubjectType")
                .subjectId("subject-1")
                .objectType("ObjectType")
                .objectId("object-1")
                .predicateType("relates-to")
                .build();
        relation1.onPrePersist();

        Relation relation2 = Relation.builder()
                .processInstance(processInstance)
                .systemId("test-system")
                .subjectType("SubjectType")
                .subjectId("subject-2")
                .objectType("ObjectType")
                .objectId("object-2")
                .predicateType("relates-to")
                .featureFlag("MY_FEATURE")
                .build();
        relation2.onPrePersist();

        relationJpaRepository.saveAll(Set.of(relation1, relation2));
        entityManager.flush();
        entityManager.clear();

        List<Relation> savedRelations = relationJpaRepository.findAll();

        assertThat(savedRelations).hasSize(2);
        assertThat(savedRelations)
                .extracting(Relation::getSubjectId)
                .containsExactlyInAnyOrder("subject-1", "subject-2");
        assertThat(savedRelations)
                .allSatisfy(rel -> {
                    assertThat(rel.getSystemId()).isEqualTo("test-system");
                    assertThat(rel.getIdempotenceId()).isNotNull();
                    assertThat(rel.getCreatedAt()).isNotNull();
                });
    }

    @Test
    void save_relationWithAllFields_persistsCorrectly() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance);

        Relation relation = Relation.builder()
                .processInstance(processInstance)
                .systemId("test-system")
                .subjectType("SubjectType")
                .subjectId("subject-id")
                .objectType("ObjectType")
                .objectId("object-id")
                .predicateType("relates-to")
                .featureFlag("FEATURE_FLAG")
                .build();
        relation.onPrePersist();

        relationJpaRepository.saveAndFlush(relation);
        entityManager.clear();

        Relation savedRelation = relationJpaRepository.findById(relation.getId()).orElseThrow();

        assertThat(savedRelation.getSystemId()).isEqualTo("test-system");
        assertThat(savedRelation.getSubjectType()).isEqualTo("SubjectType");
        assertThat(savedRelation.getSubjectId()).isEqualTo("subject-id");
        assertThat(savedRelation.getObjectType()).isEqualTo("ObjectType");
        assertThat(savedRelation.getObjectId()).isEqualTo("object-id");
        assertThat(savedRelation.getPredicateType()).isEqualTo("relates-to");
        assertThat(savedRelation.getFeatureFlag()).isEqualTo("FEATURE_FLAG");
        assertThat(savedRelation.getIdempotenceId()).isNotNull();
        assertThat(savedRelation.getCreatedAt()).isNotNull();
    }

    @Test
    void findByProcessInstanceId_returnsPagedRelations() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance);

        Relation relation1 = Relation.builder()
                .processInstance(processInstance)
                .systemId("test-system")
                .subjectType("SubjectType")
                .subjectId("subject-1")
                .objectType("ObjectType")
                .objectId("object-1")
                .predicateType("relates-to")
                .build();
        relation1.onPrePersist();

        Relation relation2 = Relation.builder()
                .processInstance(processInstance)
                .systemId("test-system")
                .subjectType("SubjectType")
                .subjectId("subject-2")
                .objectType("ObjectType")
                .objectId("object-2")
                .predicateType("relates-to")
                .build();
        relation2.onPrePersist();

        relationJpaRepository.saveAll(Set.of(relation1, relation2));
        entityManager.flush();
        entityManager.clear();

        Page<Relation> firstPage = relationJpaRepository.findByProcessInstanceId(
                processInstance.getId(), PageRequest.of(0, 1, Sort.by("createdAt")));

        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent()).hasSize(1);

        Page<Relation> secondPage = relationJpaRepository.findByProcessInstanceId(
                processInstance.getId(), PageRequest.of(1, 1, Sort.by("createdAt")));

        assertThat(secondPage.getContent()).hasSize(1);
        assertThat(secondPage.getContent().getFirst().getSubjectId())
                .isNotEqualTo(firstPage.getContent().getFirst().getSubjectId());
    }

    @Test
    void findByProcessInstanceId_doesNotReturnRelationsFromOtherProcessInstances() {
        ProcessInstance processInstance1 = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance1);

        ProcessInstance processInstance2 = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance2);

        Relation relation1 = Relation.builder()
                .processInstance(processInstance1)
                .systemId("test-system")
                .subjectType("SubjectType")
                .subjectId("subject-1")
                .objectType("ObjectType")
                .objectId("object-1")
                .predicateType("relates-to")
                .build();
        relation1.onPrePersist();

        Relation relation2 = Relation.builder()
                .processInstance(processInstance2)
                .systemId("test-system")
                .subjectType("SubjectType")
                .subjectId("subject-2")
                .objectType("ObjectType")
                .objectId("object-2")
                .predicateType("relates-to")
                .build();
        relation2.onPrePersist();

        relationJpaRepository.saveAll(Set.of(relation1, relation2));
        entityManager.flush();
        entityManager.clear();

        Page<Relation> result = relationJpaRepository.findByProcessInstanceId(
                processInstance1.getId(), PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getSubjectId()).isEqualTo("subject-1");
    }

    @Test
    void saveAll_withBatching_persistsAllRelationsAcrossMultipleBatches() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance);

        // Create 150 relations (exceeds BATCH_SIZE of 100, so requires 2 batches)
        int totalRelations = 150;
        List<Relation> relations = new ArrayList<>();
        for (int i = 0; i < totalRelations; i++) {
            Relation relation = Relation.builder()
                    .processInstance(processInstance)
                    .systemId("test-system")
                    .subjectType("SubjectType")
                    .subjectId("subject-" + i)
                    .objectType("ObjectType")
                    .objectId("object-" + i)
                    .predicateType("relates-to")
                    .build();
            relation.onPrePersist();
            relations.add(relation);
        }

        Set<Relation> savedRelations = relationRepository.saveAllNewRelations(relations);
        entityManager.flush();
        entityManager.clear();

        assertThat(savedRelations).hasSize(totalRelations);

        List<Relation> allPersistedRelations = relationJpaRepository.findAll();
        assertThat(allPersistedRelations).hasSize(totalRelations);
    }

    @Test
    void saveAll_withBatching_filtersExistingRelationsAcrossBatches() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance);

        // Pre-create some relations that will be in different batches
        Relation existingInFirstBatch = Relation.builder()
                .processInstance(processInstance)
                .systemId("test-system")
                .subjectType("SubjectType")
                .subjectId("subject-10")
                .objectType("ObjectType")
                .objectId("object-10")
                .predicateType("relates-to")
                .build();
        existingInFirstBatch.onPrePersist();

        Relation existingInSecondBatch = Relation.builder()
                .processInstance(processInstance)
                .systemId("test-system")
                .subjectType("SubjectType")
                .subjectId("subject-110")
                .objectType("ObjectType")
                .objectId("object-110")
                .predicateType("relates-to")
                .build();
        existingInSecondBatch.onPrePersist();

        relationJpaRepository.saveAll(List.of(existingInFirstBatch, existingInSecondBatch));
        entityManager.flush();
        entityManager.clear();

        // Now try to save 150 relations including duplicates at positions 10 and 110
        int totalRelations = 150;
        List<Relation> relations = new ArrayList<>();
        for (int i = 0; i < totalRelations; i++) {
            Relation relation = Relation.builder()
                    .processInstance(processInstance)
                    .systemId("test-system")
                    .subjectType("SubjectType")
                    .subjectId("subject-" + i)
                    .objectType("ObjectType")
                    .objectId("object-" + i)
                    .predicateType("relates-to")
                    .build();
            relation.onPrePersist();
            relations.add(relation);
        }

        Set<Relation> newlySavedRelations = relationRepository.saveAllNewRelations(relations);
        entityManager.flush();
        entityManager.clear();

        // Should have saved 148 new relations (150 - 2 existing)
        assertThat(newlySavedRelations).hasSize(totalRelations - 2);

        // Total in DB should be 150 (2 pre-existing + 148 new)
        List<Relation> allPersistedRelations = relationJpaRepository.findAll();
        assertThat(allPersistedRelations).hasSize(totalRelations);
    }
}
