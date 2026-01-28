package ch.admin.bit.jeap.processcontext.domain.processinstance.relation;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessData;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessDataRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.Relation;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelationFactoryTest {

    private static final String SYSTEM_ID = "test-system";
    private static final String PREDICATE_TYPE = "relates-to";
    private static final String OBJECT_TYPE = "ObjectEntity";
    private static final String SUBJECT_TYPE = "SubjectEntity";
    private static final String OBJECT_KEY = "objectKey";
    private static final String SUBJECT_KEY = "subjectKey";

    @Mock
    private ProcessDataRepository processDataRepository;

    @Mock
    private ProcessInstance processInstance;

    private RelationFactory relationFactory;

    @BeforeEach
    void setUp() {
        relationFactory = new RelationFactory(processDataRepository);
    }

    @Test
    void createNewRelations_emptyProcessDataList_returnsEmptySet() {
        Set<Relation> relations = relationFactory.createNewRelations(processInstance, List.of());

        assertThat(relations).isEmpty();
    }

    @Test
    void createNewRelations_noMatchingPatterns_returnsEmptySet() {
        ProcessTemplate processTemplate = createProcessTemplate(List.of());
        when(processInstance.getProcessTemplate()).thenReturn(processTemplate);

        ProcessData processData = new ProcessData("someKey", "someValue");

        Set<Relation> relations = relationFactory.createNewRelations(processInstance, List.of(processData));

        assertThat(relations).isEmpty();
    }

    @Test
    void createNewRelations_onlyObjectDataProvided_createsRelationWithPersistentSubject() {
        RelationPattern pattern = createRelationPattern(null);
        ProcessTemplate processTemplate = createProcessTemplate(List.of(pattern));
        when(processInstance.getProcessTemplate()).thenReturn(processTemplate);

        ProcessData persistentSubject = new ProcessData(SUBJECT_KEY, "persistent-subject-value");
        when(processDataRepository.findProcessData(any(), eq(SUBJECT_KEY), eq(null)))
                .thenReturn(List.of(persistentSubject));

        ProcessData objectData = new ProcessData(OBJECT_KEY, "object-value-1");

        Set<Relation> relations = relationFactory.createNewRelations(processInstance, List.of(objectData));

        assertThat(relations).hasSize(1);
        Relation relation = relations.iterator().next();
        assertThat(relation.getSystemId()).isEqualTo(SYSTEM_ID);
        assertThat(relation.getPredicateType()).isEqualTo(PREDICATE_TYPE);
        assertThat(relation.getObjectType()).isEqualTo(OBJECT_TYPE);
        assertThat(relation.getObjectId()).isEqualTo("object-value-1");
        assertThat(relation.getSubjectType()).isEqualTo(SUBJECT_TYPE);
        assertThat(relation.getSubjectId()).isEqualTo("persistent-subject-value");
    }

    @Test
    void createNewRelations_onlySubjectDataProvided_createsRelationWithPersistentObject() {
        RelationPattern pattern = createRelationPattern(null);
        ProcessTemplate processTemplate = createProcessTemplate(List.of(pattern));
        when(processInstance.getProcessTemplate()).thenReturn(processTemplate);

        ProcessData persistentObject = new ProcessData(OBJECT_KEY, "persistent-object-value");
        when(processDataRepository.findProcessData(any(), eq(SUBJECT_KEY), eq(null)))
                .thenReturn(List.of(persistentObject));

        ProcessData subjectData = new ProcessData(SUBJECT_KEY, "subject-value-1");

        Set<Relation> relations = relationFactory.createNewRelations(processInstance, List.of(subjectData));

        assertThat(relations).hasSize(1);
        Relation relation = relations.iterator().next();
        assertThat(relation.getSubjectId()).isEqualTo("subject-value-1");
        assertThat(relation.getObjectId()).isEqualTo("persistent-object-value");
    }

    @Test
    void createNewRelations_bothObjectAndSubjectNew_createsSingleUniqueRelation() {
        // When both object and subject are new, the factory processes from both perspectives
        // but returns a Set, so duplicates are eliminated
        RelationPattern pattern = createRelationPattern(null);
        ProcessTemplate processTemplate = createProcessTemplate(List.of(pattern));
        when(processInstance.getProcessTemplate()).thenReturn(processTemplate);
        when(processDataRepository.findProcessData(any(), any(), any())).thenReturn(List.of());

        ProcessData objectData = new ProcessData(OBJECT_KEY, "object-value");
        ProcessData subjectData = new ProcessData(SUBJECT_KEY, "subject-value");

        Set<Relation> relations = relationFactory.createNewRelations(processInstance, List.of(objectData, subjectData));

        // Only 1 unique relation (duplicates eliminated by Set)
        assertThat(relations).hasSize(1);
        Relation relation = relations.iterator().next();
        assertThat(relation.getObjectId()).isEqualTo("object-value");
        assertThat(relation.getSubjectId()).isEqualTo("subject-value");
    }

    @Test
    void createNewRelations_multipleObjectsAndSubjects_createsUniqueRelationsOnly() {
        // With 2 objects and 2 subjects, all new:
        // Creates 4 unique relations (2 objects * 2 subjects), duplicates eliminated by Set
        RelationPattern pattern = createRelationPattern(null);
        ProcessTemplate processTemplate = createProcessTemplate(List.of(pattern));
        when(processInstance.getProcessTemplate()).thenReturn(processTemplate);
        when(processDataRepository.findProcessData(any(), any(), any())).thenReturn(List.of());

        ProcessData objectData1 = new ProcessData(OBJECT_KEY, "object-1");
        ProcessData objectData2 = new ProcessData(OBJECT_KEY, "object-2");
        ProcessData subjectData1 = new ProcessData(SUBJECT_KEY, "subject-1");
        ProcessData subjectData2 = new ProcessData(SUBJECT_KEY, "subject-2");

        Set<Relation> relations = relationFactory.createNewRelations(
                processInstance, List.of(objectData1, objectData2, subjectData1, subjectData2));

        assertThat(relations).hasSize(4);
    }

    @Test
    void createNewRelations_joinByRole_onlyCreatesRelationForMatchingRoles() {
        RelationPattern pattern = createRelationPattern(RelationPattern.JoinType.BY_ROLE);
        ProcessTemplate processTemplate = createProcessTemplate(List.of(pattern));
        when(processInstance.getProcessTemplate()).thenReturn(processTemplate);
        when(processDataRepository.findProcessData(any(), any(), any())).thenReturn(List.of());

        ProcessData objectData1 = new ProcessData(OBJECT_KEY, "object-1", "role-A");
        ProcessData objectData2 = new ProcessData(OBJECT_KEY, "object-2", "role-B");
        ProcessData subjectData1 = new ProcessData(SUBJECT_KEY, "subject-1", "role-A");
        ProcessData subjectData2 = new ProcessData(SUBJECT_KEY, "subject-2", "role-C");

        Set<Relation> relations = relationFactory.createNewRelations(
                processInstance, List.of(objectData1, objectData2, subjectData1, subjectData2));

        // Only object-1/subject-1 match (both have role-A), 1 unique relation
        assertThat(relations).hasSize(1);
        Relation relation = relations.iterator().next();
        assertThat(relation.getObjectId()).isEqualTo("object-1");
        assertThat(relation.getSubjectId()).isEqualTo("subject-1");
    }

    @Test
    void createNewRelations_joinByRole_noMatchWhenObjectRoleIsNull() {
        RelationPattern pattern = createRelationPattern(RelationPattern.JoinType.BY_ROLE);
        ProcessTemplate processTemplate = createProcessTemplate(List.of(pattern));
        when(processInstance.getProcessTemplate()).thenReturn(processTemplate);
        when(processDataRepository.findProcessData(any(), any(), any())).thenReturn(List.of());

        ProcessData objectData = new ProcessData(OBJECT_KEY, "object-1"); // no role
        ProcessData subjectData = new ProcessData(SUBJECT_KEY, "subject-1", "role-A");

        Set<Relation> relations = relationFactory.createNewRelations(
                processInstance, List.of(objectData, subjectData));

        assertThat(relations).isEmpty();
    }

    @Test
    void createNewRelations_joinByValue_onlyCreatesRelationForMatchingValues() {
        RelationPattern pattern = createRelationPattern(RelationPattern.JoinType.BY_VALUE);
        ProcessTemplate processTemplate = createProcessTemplate(List.of(pattern));
        when(processInstance.getProcessTemplate()).thenReturn(processTemplate);
        when(processDataRepository.findProcessData(any(), any(), any())).thenReturn(List.of());

        ProcessData objectData1 = new ProcessData(OBJECT_KEY, "shared-value");
        ProcessData objectData2 = new ProcessData(OBJECT_KEY, "different-value");
        ProcessData subjectData1 = new ProcessData(SUBJECT_KEY, "shared-value");
        ProcessData subjectData2 = new ProcessData(SUBJECT_KEY, "another-value");

        Set<Relation> relations = relationFactory.createNewRelations(
                processInstance, List.of(objectData1, objectData2, subjectData1, subjectData2));

        // Only object-1/subject-1 match (both have "shared-value"), 1 unique relation
        assertThat(relations).hasSize(1);
        Relation relation = relations.iterator().next();
        assertThat(relation.getObjectId()).isEqualTo("shared-value");
        assertThat(relation.getSubjectId()).isEqualTo("shared-value");
    }

    @Test
    void createNewRelations_withFeatureFlag_setsFeatureFlagOnRelation() {
        RelationPattern pattern = RelationPattern.builder()
                .predicateType(PREDICATE_TYPE)
                .objectSelector(RelationNodeSelector.builder()
                        .type(OBJECT_TYPE)
                        .processDataKey(OBJECT_KEY)
                        .build())
                .subjectSelector(RelationNodeSelector.builder()
                        .type(SUBJECT_TYPE)
                        .processDataKey(SUBJECT_KEY)
                        .build())
                .featureFlag("MY_FEATURE_FLAG")
                .build();

        ProcessTemplate processTemplate = createProcessTemplate(List.of(pattern));
        when(processInstance.getProcessTemplate()).thenReturn(processTemplate);
        when(processDataRepository.findProcessData(any(), any(), any())).thenReturn(List.of());

        ProcessData objectData = new ProcessData(OBJECT_KEY, "object-value");
        ProcessData subjectData = new ProcessData(SUBJECT_KEY, "subject-value");

        Set<Relation> relations = relationFactory.createNewRelations(
                processInstance, List.of(objectData, subjectData));

        assertThat(relations).hasSize(1);
        assertThat(relations.iterator().next().getFeatureFlag()).isEqualTo("MY_FEATURE_FLAG");
    }

    @Test
    void createNewRelations_withRoleInSelector_onlyMatchesProcessDataWithRole() {
        RelationNodeSelector objectSelector = RelationNodeSelector.builder()
                .type(OBJECT_TYPE)
                .processDataKey(OBJECT_KEY)
                .processDataRole("specific-role")
                .build();

        RelationNodeSelector subjectSelector = RelationNodeSelector.builder()
                .type(SUBJECT_TYPE)
                .processDataKey(SUBJECT_KEY)
                .build();

        RelationPattern pattern = RelationPattern.builder()
                .predicateType(PREDICATE_TYPE)
                .objectSelector(objectSelector)
                .subjectSelector(subjectSelector)
                .build();

        ProcessTemplate processTemplate = createProcessTemplate(List.of(pattern));
        when(processInstance.getProcessTemplate()).thenReturn(processTemplate);
        when(processDataRepository.findProcessData(any(), any(), any())).thenReturn(List.of());

        ProcessData objectDataWithRole = new ProcessData(OBJECT_KEY, "object-with-role", "specific-role");
        ProcessData objectDataWithoutRole = new ProcessData(OBJECT_KEY, "object-without-role");
        ProcessData subjectData = new ProcessData(SUBJECT_KEY, "subject-value");

        Set<Relation> relations = relationFactory.createNewRelations(
                processInstance, List.of(objectDataWithRole, objectDataWithoutRole, subjectData));

        // Only objectDataWithRole matches the selector, 1 unique relation
        assertThat(relations).hasSize(1);
        assertThat(relations.iterator().next().getObjectId()).isEqualTo("object-with-role");
    }

    @Test
    void createNewRelations_multiplePatterns_createsRelationsForAllMatchingPatterns() {
        RelationPattern pattern1 = RelationPattern.builder()
                .predicateType("predicate-1")
                .objectSelector(RelationNodeSelector.builder()
                        .type("Type1")
                        .processDataKey("key1")
                        .build())
                .subjectSelector(RelationNodeSelector.builder()
                        .type("Type2")
                        .processDataKey("key2")
                        .build())
                .build();

        RelationPattern pattern2 = RelationPattern.builder()
                .predicateType("predicate-2")
                .objectSelector(RelationNodeSelector.builder()
                        .type("Type3")
                        .processDataKey("key3")
                        .build())
                .subjectSelector(RelationNodeSelector.builder()
                        .type("Type4")
                        .processDataKey("key4")
                        .build())
                .build();

        ProcessTemplate processTemplate = createProcessTemplate(List.of(pattern1, pattern2));
        when(processInstance.getProcessTemplate()).thenReturn(processTemplate);
        when(processDataRepository.findProcessData(any(), any(), any())).thenReturn(List.of());

        ProcessData data1 = new ProcessData("key1", "value1");
        ProcessData data2 = new ProcessData("key2", "value2");
        ProcessData data3 = new ProcessData("key3", "value3");
        ProcessData data4 = new ProcessData("key4", "value4");

        Set<Relation> relations = relationFactory.createNewRelations(
                processInstance, List.of(data1, data2, data3, data4));

        // Each pattern creates 1 unique relation: total 2
        assertThat(relations).hasSize(2);
        assertThat(relations)
                .extracting(Relation::getPredicateType)
                .containsExactlyInAnyOrder("predicate-1", "predicate-2");
    }

    @Test
    void createNewRelations_combinedNewAndPersistentData_createsRelation() {
        RelationPattern pattern = createRelationPattern(null);
        ProcessTemplate processTemplate = createProcessTemplate(List.of(pattern));
        when(processInstance.getProcessTemplate()).thenReturn(processTemplate);

        // Persistent subject exists
        ProcessData persistentSubject = new ProcessData(SUBJECT_KEY, "persistent-subject");
        when(processDataRepository.findProcessData(any(), eq(SUBJECT_KEY), eq(null)))
                .thenReturn(List.of(persistentSubject));

        // New object arrives
        ProcessData newObject = new ProcessData(OBJECT_KEY, "new-object");

        Set<Relation> relations = relationFactory.createNewRelations(
                processInstance, List.of(newObject));

        assertThat(relations).hasSize(1);
        Relation relation = relations.iterator().next();
        assertThat(relation.getObjectId()).isEqualTo("new-object");
        assertThat(relation.getSubjectId()).isEqualTo("persistent-subject");
    }

    private ProcessTemplate createProcessTemplate(List<RelationPattern> relationPatterns) {
        return ProcessTemplate.builder()
                .name("test-template")
                .templateHash("hash")
                .relationSystemId(SYSTEM_ID)
                .relationPatterns(relationPatterns)
                .taskTypes(List.of(TaskType.builder()
                        .name("task")
                        .cardinality(TaskCardinality.SINGLE_INSTANCE)
                        .lifecycle(TaskLifecycle.STATIC)
                        .build()))
                .build();
    }

    private RelationPattern createRelationPattern(RelationPattern.JoinType joinType) {
        return RelationPattern.builder()
                .predicateType(PREDICATE_TYPE)
                .joinType(joinType)
                .objectSelector(RelationNodeSelector.builder()
                        .type(OBJECT_TYPE)
                        .processDataKey(OBJECT_KEY)
                        .build())
                .subjectSelector(RelationNodeSelector.builder()
                        .type(SUBJECT_TYPE)
                        .processDataKey(SUBJECT_KEY)
                        .build())
                .build();
    }
}
