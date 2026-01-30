package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessContextFactory;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessData;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceStubs;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Set;

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
    private ProcessInstanceJpaRepository processInstanceJpaRepository;

    @Test
    void findByProcessInstanceAndKey_findsMatchingProcessData() {
        ProcessData data1 = new ProcessData("key1", "value1");
        ProcessData data2 = new ProcessData("key1", "value2");
        ProcessData data3 = new ProcessData("key2", "value3");
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance(
                "template", Set.of(data1, data2, data3));
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
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance(
                "template", Set.of(data));
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
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance(
                "template", Set.of(data1, data2, data3));
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
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance(
                "template", Set.of(data));
        processInstanceJpaRepository.saveAndFlush(processInstance);
        entityManager.clear();

        ProcessInstance savedInstance = processInstanceJpaRepository.findByOriginProcessId(processInstance.getOriginProcessId()).orElseThrow();
        List<ProcessData> result = processDataJpaRepository.findByProcessInstanceAndKeyAndRole(savedInstance, "key1", "roleB");

        assertThat(result).isEmpty();
    }

    @Test
    void findByProcessInstanceAndKey_doesNotReturnDataFromOtherProcessInstances() {
        ProcessData data1 = new ProcessData("key1", "value1");
        ProcessInstance processInstance1 = ProcessInstanceStubs.createProcessWithSingleTaskInstance(
                "template", Set.of(data1));
        processInstanceJpaRepository.saveAndFlush(processInstance1);

        ProcessData data2 = new ProcessData("key1", "value2");
        ProcessInstance processInstance2 = ProcessInstanceStubs.createProcessWithSingleTaskInstance(
                "template", Set.of(data2));
        processInstanceJpaRepository.saveAndFlush(processInstance2);
        entityManager.clear();

        ProcessInstance savedInstance1 = processInstanceJpaRepository.findByOriginProcessId(processInstance1.getOriginProcessId()).orElseThrow();
        List<ProcessData> result = processDataJpaRepository.findByProcessInstanceAndKey(savedInstance1, "key1");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getValue()).isEqualTo("value1");
    }
}
