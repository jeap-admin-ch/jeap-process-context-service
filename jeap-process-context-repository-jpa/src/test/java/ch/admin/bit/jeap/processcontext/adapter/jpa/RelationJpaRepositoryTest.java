package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceStubs;
import ch.admin.bit.jeap.processcontext.domain.processinstance.Relation;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = JpaAdapterConfig.class)
class RelationJpaRepositoryTest {

    @PersistenceContext
    EntityManager entityManager;

    @MockitoBean
    @SuppressWarnings("unused")
    private ProcessTemplateRepository processTemplateRepository;

    @Autowired
    private RelationJpaRepository relationJpaRepository;

    @Autowired
    private ProcessInstanceJpaRepository processInstanceJpaRepository;

    @Test
    void saveAll_persistsRelations() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance);

        Relation relation1 = Relation.builder()
                .systemId("test-system")
                .subjectType("SubjectType")
                .subjectId("subject-1")
                .objectType("ObjectType")
                .objectId("object-1")
                .predicateType("relates-to")
                .build();
        relation1.onPrePersist();
        ReflectionTestUtils.setField(relation1, "processInstance", processInstance);

        Relation relation2 = Relation.builder()
                .systemId("test-system")
                .subjectType("SubjectType")
                .subjectId("subject-2")
                .objectType("ObjectType")
                .objectId("object-2")
                .predicateType("relates-to")
                .featureFlag("MY_FEATURE")
                .build();
        relation2.onPrePersist();
        ReflectionTestUtils.setField(relation2, "processInstance", processInstance);

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
                .systemId("test-system")
                .subjectType("SubjectType")
                .subjectId("subject-id")
                .objectType("ObjectType")
                .objectId("object-id")
                .predicateType("relates-to")
                .featureFlag("FEATURE_FLAG")
                .build();
        relation.onPrePersist();
        ReflectionTestUtils.setField(relation, "processInstance", processInstance);

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
    void findByProcessInstance_returnsRelationsForProcessInstance() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance);

        Relation relation1 = Relation.builder()
                .systemId("test-system")
                .subjectType("SubjectType")
                .subjectId("subject-1")
                .objectType("ObjectType")
                .objectId("object-1")
                .predicateType("relates-to")
                .build();
        relation1.onPrePersist();
        ReflectionTestUtils.setField(relation1, "processInstance", processInstance);

        Relation relation2 = Relation.builder()
                .systemId("test-system")
                .subjectType("SubjectType")
                .subjectId("subject-2")
                .objectType("ObjectType")
                .objectId("object-2")
                .predicateType("relates-to")
                .build();
        relation2.onPrePersist();
        ReflectionTestUtils.setField(relation2, "processInstance", processInstance);

        relationJpaRepository.saveAll(Set.of(relation1, relation2));
        entityManager.flush();
        entityManager.clear();

        ProcessInstance savedInstance = processInstanceJpaRepository.findByOriginProcessId(processInstance.getOriginProcessId()).orElseThrow();
        Set<Relation> result = relationJpaRepository.findByProcessInstance(savedInstance);

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(Relation::getSubjectId)
                .containsExactlyInAnyOrder("subject-1", "subject-2");
    }

    @Test
    void findByProcessInstance_doesNotReturnRelationsFromOtherProcessInstances() {
        ProcessInstance processInstance1 = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance1);

        ProcessInstance processInstance2 = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance2);

        Relation relation1 = Relation.builder()
                .systemId("test-system")
                .subjectType("SubjectType")
                .subjectId("subject-1")
                .objectType("ObjectType")
                .objectId("object-1")
                .predicateType("relates-to")
                .build();
        relation1.onPrePersist();
        ReflectionTestUtils.setField(relation1, "processInstance", processInstance1);

        Relation relation2 = Relation.builder()
                .systemId("test-system")
                .subjectType("SubjectType")
                .subjectId("subject-2")
                .objectType("ObjectType")
                .objectId("object-2")
                .predicateType("relates-to")
                .build();
        relation2.onPrePersist();
        ReflectionTestUtils.setField(relation2, "processInstance", processInstance2);

        relationJpaRepository.saveAll(Set.of(relation1, relation2));
        entityManager.flush();
        entityManager.clear();

        ProcessInstance savedInstance1 = processInstanceJpaRepository.findByOriginProcessId(processInstance1.getOriginProcessId()).orElseThrow();
        Set<Relation> result = relationJpaRepository.findByProcessInstance(savedInstance1);

        assertThat(result).hasSize(1);
        assertThat(result.iterator().next().getSubjectId()).isEqualTo("subject-1");
    }

    @Test
    void findByProcessInstance_noRelations_returnsEmptySet() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance);
        entityManager.clear();

        ProcessInstance savedInstance = processInstanceJpaRepository.findByOriginProcessId(processInstance.getOriginProcessId()).orElseThrow();
        Set<Relation> result = relationJpaRepository.findByProcessInstance(savedInstance);

        assertThat(result).isEmpty();
    }
}
