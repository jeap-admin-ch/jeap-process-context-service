package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceStubs;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessRelation;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessRelationStubs;
import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextFactory;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessRelationRoleVisibility;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = JpaAdapterConfig.class)
class ProcessRelationJpaRepositoryTest {

    @PersistenceContext
    EntityManager entityManager;

    @MockitoBean
    private ProcessTemplateRepository processTemplateRepository;

    @MockitoBean
    private ProcessContextFactory processContextFactory;

    @Autowired
    private ProcessRelationJpaRepository processRelationJpaRepository;

    @Autowired
    private ProcessInstanceJpaRepository processInstanceJpaRepository;

    @Test
    void findAllByProcessInstanceId_returnsMatchingRelations() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance);

        ProcessRelation relation1 = ProcessRelationStubs.createProcessRelation(processInstance, "relation1", "related-1");
        ProcessRelation relation2 = ProcessRelationStubs.createProcessRelation(processInstance, "relation2", "related-2");
        processRelationJpaRepository.saveAll(List.of(relation1, relation2));
        entityManager.flush();
        entityManager.clear();

        List<ProcessRelation> result = processRelationJpaRepository.findAllByProcessInstanceId(processInstance.getId());

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(ProcessRelation::getName)
                .containsExactlyInAnyOrder("relation1", "relation2");
    }

    @Test
    void findAllByProcessInstanceId_noRelations_returnsEmptyList() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance);
        entityManager.clear();

        List<ProcessRelation> result = processRelationJpaRepository.findAllByProcessInstanceId(processInstance.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void findAllByProcessInstanceId_paged_returnsPagedRelations() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance);

        ProcessRelation relation1 = ProcessRelationStubs.createProcessRelation(processInstance, "relation1", "related-1");
        ProcessRelation relation2 = ProcessRelationStubs.createProcessRelation(processInstance, "relation2", "related-2");
        processRelationJpaRepository.saveAll(List.of(relation1, relation2));
        entityManager.flush();
        entityManager.clear();

        Page<ProcessRelation> firstPage = processRelationJpaRepository.findAllByProcessInstanceId(
                processInstance.getId(), PageRequest.of(0, 1));

        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent()).hasSize(1);

        Page<ProcessRelation> secondPage = processRelationJpaRepository.findAllByProcessInstanceId(
                processInstance.getId(), PageRequest.of(1, 1));

        assertThat(secondPage.getContent()).hasSize(1);
        assertThat(secondPage.getContent().getFirst().getName())
                .isNotEqualTo(firstPage.getContent().getFirst().getName());
    }

    @Test
    void findAllByProcessInstanceId_onlyReturnsRelationsForRequestedProcessInstance() {
        ProcessInstance processInstance1 = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance1);

        ProcessInstance processInstance2 = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance2);

        ProcessRelation relation1 = ProcessRelationStubs.createProcessRelation(processInstance1, "relation1", "related-1");
        ProcessRelation relation2 = ProcessRelationStubs.createProcessRelation(processInstance2, "relation2", "related-2");
        processRelationJpaRepository.saveAll(List.of(relation1, relation2));
        entityManager.flush();
        entityManager.clear();

        List<ProcessRelation> result = processRelationJpaRepository.findAllByProcessInstanceId(processInstance1.getId());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("relation1");
    }

    @Test
    void findAllVisibleForProcess_returnsDirectAndExternalRelations() {
        ProcessInstance processInstance1 = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance1);

        ProcessInstance processInstance2 = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance2);

        // Direct relation on processInstance1 pointing to processInstance2
        ProcessRelation directRelation = ProcessRelationStubs.createProcessRelation(
                processInstance1, "direct", processInstance2.getOriginProcessId(), ProcessRelationRoleVisibility.BOTH);
        // External relation on processInstance2 pointing to processInstance1
        ProcessRelation externalRelation = ProcessRelationStubs.createProcessRelation(
                processInstance2, "external", processInstance1.getOriginProcessId(), ProcessRelationRoleVisibility.BOTH);
        processRelationJpaRepository.saveAll(List.of(directRelation, externalRelation));
        entityManager.flush();
        entityManager.clear();

        Page<ProcessRelation> result = processRelationJpaRepository.findAllVisibleForProcess(
                processInstance1.getId(), processInstance1.getOriginProcessId(),
                PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(ProcessRelation::getName)
                .containsExactlyInAnyOrder("direct", "external");
    }

    @Test
    void findAllVisibleForProcess_excludesTargetVisibilityForDirect() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance);

        ProcessRelation visibleRelation = ProcessRelationStubs.createProcessRelation(
                processInstance, "visible", "other-process", ProcessRelationRoleVisibility.ORIGIN);
        ProcessRelation hiddenRelation = ProcessRelationStubs.createProcessRelation(
                processInstance, "hidden", "other-process-2", ProcessRelationRoleVisibility.TARGET);
        processRelationJpaRepository.saveAll(List.of(visibleRelation, hiddenRelation));
        entityManager.flush();
        entityManager.clear();

        Page<ProcessRelation> result = processRelationJpaRepository.findAllVisibleForProcess(
                processInstance.getId(), processInstance.getOriginProcessId(),
                PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getName()).isEqualTo("visible");
    }

    @Test
    void findAllVisibleForProcess_excludesOriginVisibilityForExternal() {
        ProcessInstance processInstance1 = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance1);

        ProcessInstance processInstance2 = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance2);

        // External relation visible from target side
        ProcessRelation visibleExternal = ProcessRelationStubs.createProcessRelation(
                processInstance2, "visible-ext", processInstance1.getOriginProcessId(), ProcessRelationRoleVisibility.TARGET);
        // External relation only visible from origin side (should be excluded)
        ProcessRelation hiddenExternal = ProcessRelationStubs.createProcessRelation(
                processInstance2, "hidden-ext", processInstance1.getOriginProcessId(), ProcessRelationRoleVisibility.ORIGIN);
        processRelationJpaRepository.saveAll(List.of(visibleExternal, hiddenExternal));
        entityManager.flush();
        entityManager.clear();

        Page<ProcessRelation> result = processRelationJpaRepository.findAllVisibleForProcess(
                processInstance1.getId(), processInstance1.getOriginProcessId(),
                PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getName()).isEqualTo("visible-ext");
    }

    @Test
    void findAllVisibleForProcess_paginatesCorrectly() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        processInstanceJpaRepository.saveAndFlush(processInstance);

        ProcessRelation relation1 = ProcessRelationStubs.createProcessRelation(
                processInstance, "relation1", "related-1", ProcessRelationRoleVisibility.BOTH);
        ProcessRelation relation2 = ProcessRelationStubs.createProcessRelation(
                processInstance, "relation2", "related-2", ProcessRelationRoleVisibility.BOTH);
        processRelationJpaRepository.saveAll(List.of(relation1, relation2));
        entityManager.flush();
        entityManager.clear();

        Page<ProcessRelation> firstPage = processRelationJpaRepository.findAllVisibleForProcess(
                processInstance.getId(), processInstance.getOriginProcessId(),
                PageRequest.of(0, 1));

        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getContent()).hasSize(1);

        Page<ProcessRelation> secondPage = processRelationJpaRepository.findAllVisibleForProcess(
                processInstance.getId(), processInstance.getOriginProcessId(),
                PageRequest.of(1, 1));

        assertThat(secondPage.getContent()).hasSize(1);
        assertThat(secondPage.getContent().getFirst().getName())
                .isNotEqualTo(firstPage.getContent().getFirst().getName());
    }
}
