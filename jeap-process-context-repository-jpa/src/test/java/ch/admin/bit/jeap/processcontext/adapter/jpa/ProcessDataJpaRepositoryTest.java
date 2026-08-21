package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.*;
import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextFactory;
import ch.admin.bit.jeap.processcontext.domain.processinstance.relation.RelationCandidate;
import ch.admin.bit.jeap.processcontext.domain.processinstance.relation.RelationCandidateCursor;
import ch.admin.bit.jeap.processcontext.domain.processinstance.relation.RelationCandidateRepository;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.RelationNodeSelector;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.RelationPattern;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = JpaAdapterConfig.class)
class ProcessDataJpaRepositoryTest {

    @PersistenceContext
    EntityManager entityManager;

    @MockitoBean
    private ProcessTemplateRepository processTemplateRepository;
    @MockitoBean
    private ProcessContextFactory processContextFactory;

    @Autowired
    private ProcessDataJpaRepository processDataJpaRepository;

    @Autowired
    private ProcessDataRepository processDataRepository;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private ProcessInstanceJpaRepository processInstanceJpaRepository;
    @Autowired
    private RelationCandidateRepository relationCandidateRepository;

    @Test
    void saveIfNew() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance);
        ProcessData data1 = new ProcessData("key1", "value1");
        ProcessData data2 = new ProcessData("key1", "value2");
        ReflectionTestUtils.setField(data1, "processInstance", processInstance);
        ReflectionTestUtils.setField(data2, "processInstance", processInstance);

        assertThat(processDataRepository.saveIfNew(data1))
                .isTrue();
        assertThat(processDataRepository.saveIfNew(data2))
                .isTrue();
        assertThat(processDataRepository.saveIfNew(data1))
                .isFalse();
        assertThat(processDataRepository.saveIfNew(data2))
                .isFalse();
    }

    @Test
    void findByProcessInstanceAndKey_findsMatchingProcessData() {
        ProcessData data1 = new ProcessData("key1", "value1");
        ProcessData data2 = new ProcessData("key1", "value2");
        ProcessData data3 = new ProcessData("key2", "value3");
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstanceSavingProcessData(
                "template", List.of(data1, data2, data3), processInstanceRepository, processDataRepository);
        processInstanceJpaRepository.saveAndFlush(processInstance);
        entityManager.clear();

        ProcessInstance savedInstance = processInstanceJpaRepository.findByOriginProcessId(processInstance.getOriginProcessId()).orElseThrow();
        List<ProcessData> result = processDataJpaRepository.findByProcessInstanceAndKey(savedInstance, "key1");

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(ProcessData::getValue)
                .containsExactlyInAnyOrder("value1", "value2");
    }

    @Test
    void findByProcessInstanceAndKey_noMatch_returnsEmptyList() {
        ProcessData data = new ProcessData("key1", "value1");
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstanceSavingProcessData(
                "template", List.of(data), processInstanceRepository, processDataRepository);
        processInstanceJpaRepository.saveAndFlush(processInstance);
        entityManager.clear();

        ProcessInstance savedInstance = processInstanceJpaRepository.findByOriginProcessId(processInstance.getOriginProcessId()).orElseThrow();
        List<ProcessData> result = processDataJpaRepository.findByProcessInstanceAndKey(savedInstance, "nonexistent-key");

        assertThat(result).isEmpty();
    }

    @Test
    void findByProcessInstanceAndKeyAndRole_findsMatchingProcessData() {
        ProcessData data1 = new ProcessData("key1", "value1", "roleA");
        ProcessData data2 = new ProcessData("key1", "value2", "roleB");
        ProcessData data3 = new ProcessData("key1", "value3", "roleA");
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstanceSavingProcessData(
                "template", List.of(data1, data2, data3), processInstanceRepository, processDataRepository);
        processInstanceJpaRepository.saveAndFlush(processInstance);
        entityManager.clear();

        ProcessInstance savedInstance = processInstanceJpaRepository.findByOriginProcessId(processInstance.getOriginProcessId()).orElseThrow();
        List<ProcessData> result = processDataJpaRepository.findByProcessInstanceAndKeyAndRole(savedInstance, "key1", "roleA");

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(ProcessData::getValue)
                .containsExactlyInAnyOrder("value1", "value3");
    }

    @Test
    void findByProcessInstanceAndKeyAndRole_noMatchingRole_returnsEmptyList() {
        ProcessData data = new ProcessData("key1", "value1", "roleA");
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstanceSavingProcessData(
                "template", List.of(data), processInstanceRepository, processDataRepository);
        processInstanceJpaRepository.saveAndFlush(processInstance);
        entityManager.clear();

        ProcessInstance savedInstance = processInstanceJpaRepository.findByOriginProcessId(processInstance.getOriginProcessId()).orElseThrow();
        List<ProcessData> result = processDataJpaRepository.findByProcessInstanceAndKeyAndRole(savedInstance, "key1", "roleB");

        assertThat(result).isEmpty();
    }

    @Test
    void findByProcessInstanceId_paged_returnsPagedProcessData() {
        ProcessData data1 = new ProcessData("key1", "value1");
        ProcessData data2 = new ProcessData("key2", "value2");
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstanceSavingProcessData(
                "template", List.of(data1, data2), processInstanceRepository, processDataRepository);
        processInstanceJpaRepository.saveAndFlush(processInstance);
        entityManager.clear();

        Page<ProcessData> firstPage = processDataJpaRepository.findByProcessInstanceId(
                processInstance.getId(), PageRequest.of(0, 1));

        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent()).hasSize(1);

        Page<ProcessData> secondPage = processDataJpaRepository.findByProcessInstanceId(
                processInstance.getId(), PageRequest.of(1, 1));

        assertThat(secondPage.getContent()).hasSize(1);
        assertThat(secondPage.getContent().getFirst().getKey())
                .isNotEqualTo(firstPage.getContent().getFirst().getKey());
    }

    @Test
    void findByProcessInstanceAndKey_doesNotReturnDataFromOtherProcessInstances() {
        ProcessData data1 = new ProcessData("key1", "value1");
        ProcessInstance processInstance1 = ProcessInstanceStubs.createProcessWithSingleTaskInstanceSavingProcessData(
                "template", List.of(data1), processInstanceRepository, processDataRepository);
        processInstanceJpaRepository.saveAndFlush(processInstance1);

        ProcessData data2 = new ProcessData("key1", "value2");
        ProcessInstance processInstance2 = ProcessInstanceStubs.createProcessWithSingleTaskInstanceSavingProcessData(
                "template", List.of(data2), processInstanceRepository, processDataRepository);
        processInstanceJpaRepository.saveAndFlush(processInstance2);
        entityManager.clear();

        ProcessInstance savedInstance1 = processInstanceJpaRepository.findByOriginProcessId(processInstance1.getOriginProcessId()).orElseThrow();
        List<ProcessData> result = processDataJpaRepository.findByProcessInstanceAndKey(savedInstance1, "key1");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getValue()).isEqualTo("value1");
    }

    @Test
    void findRelationCandidates_cartesianProductUsesKeysetPaging() {
        ProcessInstance process = saveProcessWithData(List.of(
                new ProcessData("object", "object-1"),
                new ProcessData("object", "object-2"),
                new ProcessData("subject", "subject-1"),
                new ProcessData("subject", "subject-2")));
        RelationPattern pattern = relationPattern(null, null, null);

        List<RelationCandidate> firstPage = relationCandidateRepository.findCandidates(
                process.getId(), pattern, null, 2);
        RelationCandidate last = firstPage.getLast();
        List<RelationCandidate> secondPage = relationCandidateRepository.findCandidates(
                process.getId(), pattern,
                new RelationCandidateCursor(last.objectProcessDataId(), last.subjectProcessDataId()), 2);

        assertThat(firstPage).hasSize(2);
        assertThat(secondPage).hasSize(2);
        assertThat(java.util.stream.Stream.concat(firstPage.stream(), secondPage.stream()).toList())
                .extracting(RelationCandidate::objectValue, RelationCandidate::subjectValue)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("object-1", "subject-1"),
                        org.assertj.core.groups.Tuple.tuple("object-1", "subject-2"),
                        org.assertj.core.groups.Tuple.tuple("object-2", "subject-1"),
                        org.assertj.core.groups.Tuple.tuple("object-2", "subject-2"));
    }

    @Test
    void findRelationCandidates_appliesValueJoin() {
        ProcessInstance process = saveProcessWithData(List.of(
                new ProcessData("object", "same"),
                new ProcessData("object", "different"),
                new ProcessData("subject", "same"),
                new ProcessData("subject", "other")));

        List<RelationCandidate> candidates = relationCandidateRepository.findCandidates(
                process.getId(), relationPattern(RelationPattern.JoinType.BY_VALUE, null, null), null, 10);

        assertThat(candidates).singleElement()
                .satisfies(candidate -> {
                    assertThat(candidate.objectValue()).isEqualTo("same");
                    assertThat(candidate.subjectValue()).isEqualTo("same");
                });
    }

    @Test
    void findRelationCandidates_appliesRoleJoinAndSelectorRoles() {
        ProcessInstance process = saveProcessWithData(List.of(
                new ProcessData("object", "object-a", "a"),
                new ProcessData("object", "object-b", "b"),
                new ProcessData("subject", "subject-a", "a"),
                new ProcessData("subject", "subject-b", "b")));

        List<RelationCandidate> candidates = relationCandidateRepository.findCandidates(
                process.getId(), relationPattern(RelationPattern.JoinType.BY_ROLE, "a", "a"), null, 10);

        assertThat(candidates).singleElement()
                .satisfies(candidate -> {
                    assertThat(candidate.objectValue()).isEqualTo("object-a");
                    assertThat(candidate.subjectValue()).isEqualTo("subject-a");
                });
    }

    private ProcessInstance saveProcessWithData(List<ProcessData> processData) {
        ProcessInstance process = ProcessInstanceStubs.createProcessWithSingleTaskInstanceSavingProcessData(
                "template", processData, processInstanceRepository, processDataRepository);
        processInstanceJpaRepository.saveAndFlush(process);
        entityManager.flush();
        entityManager.clear();
        return process;
    }

    private static RelationPattern relationPattern(RelationPattern.JoinType joinType,
                                                   String objectRole, String subjectRole) {
        return RelationPattern.builder()
                .predicateType("relates-to")
                .joinType(joinType)
                .objectSelector(RelationNodeSelector.builder()
                        .type("Object")
                        .processDataKey("object")
                        .processDataRole(objectRole)
                        .build())
                .subjectSelector(RelationNodeSelector.builder()
                        .type("Subject")
                        .processDataKey("subject")
                        .processDataRole(subjectRole)
                        .build())
                .build();
    }
}
