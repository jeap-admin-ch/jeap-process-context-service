package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceStubs;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessRelation;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessRelationStubs;
import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextFactory;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
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
}
