package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.*;
import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextFactory;
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
}
