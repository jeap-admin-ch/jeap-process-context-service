package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.message.MessageData;
import ch.admin.bit.jeap.processcontext.domain.message.MessageRepository;
import ch.admin.bit.jeap.processcontext.domain.message.OriginTaskId;
import ch.admin.bit.jeap.processcontext.domain.processinstance.*;
import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextFactory;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import com.fasterxml.uuid.Generators;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ContextConfiguration(classes = JpaAdapterConfig.class)
class ProcessInstanceJpaRepositoryTest {

    private static final String R1 = "ref1";
    private static final String R2 = "ref2";
    private static final String R3 = "ref3";
    private static final String V1 = "val1";
    private static final String V2 = "val2";
    private static final String V3 = "val3";

    @PersistenceContext
    EntityManager entityManager;
    @MockitoBean
    private ProcessTemplateRepository processTemplateRepository;
    @MockitoBean
    private ProcessContextFactory processContextFactory;
    @Autowired
    private ProcessInstanceJpaRepository repository;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private MessageReferenceJpaRepository messageReferenceJpaRepository;

    private JpaRepositoryTestSupport jpaRepositoryTestSupport;

    @BeforeEach
    void setUp() {
        jpaRepositoryTestSupport = new JpaRepositoryTestSupport(messageReferenceJpaRepository);
    }

    @Test
    void testFindUncompletedProcessInstancesHavingProcessData() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataProcessData(messageRepository);
        ProcessInstance processInstanceSaved = repository.saveAndFlush(processInstance);
        final String originProcessId = processInstanceSaved.getOriginProcessId();
        final String templateName = processInstanceSaved.getProcessTemplateName();


        assertProcessInstanceWithProcessDataFound(originProcessId, templateName, "targetKeyName", "someValue", "someRole");
        assertProcessInstanceWithProcessDataFound(originProcessId, templateName, "targetKeyName", "someValueOtherValue", "someOtherRole");

        assertProcessInstanceWithProcessDataNotFound("wrongTemplate", "targetKeyName", "someValue", "someRole");
        assertProcessInstanceWithProcessDataNotFound(templateName, "wrongKey", "someValue", "someRole");
        assertProcessInstanceWithProcessDataNotFound(templateName, "targetKeyName", "wrongValue", "someRole");
        assertProcessInstanceWithProcessDataNotFound(templateName, "targetKeyName", "someValue", "wrongRole");
    }

    @SuppressWarnings("SameParameterValue")
    private void assertProcessInstanceWithProcessDataFound(String originProcessId, String templateName, String processDataKey, String processDataValue, String processDataRole) {
        Set<String> originProcessIds = repository.findUncompletedProcessInstancesHavingProcessData(templateName, processDataKey, processDataValue, processDataRole);
        assertThat(originProcessIds).containsOnly(originProcessId);
    }

    private void assertProcessInstanceWithProcessDataNotFound(String templateName, String processDataKey, String processDataValue, String processDataRole) {
        Set<String> originProcessIds = repository.findUncompletedProcessInstancesHavingProcessData(templateName, processDataKey, processDataValue, processDataRole);
        assertThat(originProcessIds).isEmpty();
    }

    @Test
    void testFindUncompletedProcessInstancesHavingProcessDataWithoutRole() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataProcessData(messageRepository);
        ProcessInstance processInstanceSaved = repository.saveAndFlush(processInstance);
        final String originProcessId = processInstanceSaved.getOriginProcessId();
        final String templateName = processInstanceSaved.getProcessTemplateName();

        assertProcessInstanceWithProcessDataWithoutRoleFound(originProcessId, templateName, "anotherTargetKeyName", "anotherValue");
        assertProcessInstanceWithProcessDataWithoutRoleNotFound("wrongTemplate", "anotherTargetKeyName", "anotherValue");
        assertProcessInstanceWithProcessDataWithoutRoleNotFound(templateName, "wrongKey", "anotherValue");
        assertProcessInstanceWithProcessDataWithoutRoleNotFound(templateName, "anotherTargetKeyName", "wrongValue");
    }

    @SuppressWarnings("SameParameterValue")
    private void assertProcessInstanceWithProcessDataWithoutRoleFound(String originProcessId, String templateName, String processDataKey, String processDataValue) {
        Set<String> originProcessIds = repository.findUncompletedProcessInstancesHavingProcessDataWithoutRole(templateName, processDataKey, processDataValue);
        assertThat(originProcessIds).containsOnly(originProcessId);
    }

    private void assertProcessInstanceWithProcessDataWithoutRoleNotFound(String templateName, String processDataKey, String processDataValue) {
        Set<String> originProcessIds = repository.findUncompletedProcessInstancesHavingProcessDataWithoutRole(templateName, processDataKey, processDataValue);
        assertThat(originProcessIds).isEmpty();
    }


    @Test
    void save_thenFind_expectEntityToBePersistedSuccessfully() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();

        ProcessInstance processInstanceSaved = repository.saveAndFlush(processInstance);
        entityManager.detach(processInstanceSaved);
        Optional<ProcessInstance> processInstanceReadOptional = repository.findByOriginProcessId(processInstance.getOriginProcessId());

        assertTrue(processInstanceReadOptional.isPresent());
        ProcessInstance persistedProcessInstanceRead = processInstanceReadOptional.get();
        assertEquals(processInstance.getProcessData(), persistedProcessInstanceRead.getProcessData());
        assertEquals(1, persistedProcessInstanceRead.getTasks().size());
        assertEquals(processInstance.getTasks().getFirst().getOriginTaskId(), persistedProcessInstanceRead.getTasks().getFirst().getOriginTaskId());
        assertTrue(persistedProcessInstanceRead.getProcessCompletion().isEmpty());
    }

    @Test
    void testNextSnapshotVersion() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        assertEquals(0, processInstance.getLatestSnapshotVersion());

        ProcessInstance processInstanceSaved = repository.saveAndFlush(processInstance);
        entityManager.detach(processInstanceSaved);
        Optional<ProcessInstance> processInstanceReadOptional = repository.findByOriginProcessId(processInstance.getOriginProcessId());

        assertTrue(processInstanceReadOptional.isPresent());
        ProcessInstance persistedProcessInstanceRead = processInstanceReadOptional.get();
        assertEquals(0, persistedProcessInstanceRead.getLatestSnapshotVersion());
        assertEquals(1, persistedProcessInstanceRead.nextSnapshotVersion());

        ProcessInstance processInstanceReadSaved = repository.saveAndFlush(persistedProcessInstanceRead);
        entityManager.detach(processInstanceReadSaved);
        Optional<ProcessInstance> processInstanceReadAgainOptional = repository.findByOriginProcessId(processInstance.getOriginProcessId());

        assertEquals(2, processInstanceReadAgainOptional.orElseThrow().nextSnapshotVersion());
    }

    @Test
    void testSnapshotNames() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        assertTrue(processInstance.getSnapshotNames().isEmpty());

        ProcessInstance processInstanceSaved = repository.saveAndFlush(processInstance);
        entityManager.detach(processInstanceSaved);
        Optional<ProcessInstance> processInstanceReadOptional = repository.findByOriginProcessId(processInstance.getOriginProcessId());

        assertTrue(processInstanceReadOptional.isPresent());
        ProcessInstance persistedProcessInstanceRead = processInstanceReadOptional.get();
        assertTrue(persistedProcessInstanceRead.getSnapshotNames().isEmpty());

        persistedProcessInstanceRead.registerSnapshot("MyTestCondition");

        ProcessInstance processInstanceReadSaved = repository.saveAndFlush(persistedProcessInstanceRead);
        entityManager.detach(processInstanceReadSaved);
        Optional<ProcessInstance> processInstanceReadAgainOptional = repository.findByOriginProcessId(processInstance.getOriginProcessId());

        assertTrue(processInstanceReadAgainOptional.isPresent());
        ProcessInstance processInstanceReadAgain = processInstanceReadAgainOptional.get();
        assertEquals(1, persistedProcessInstanceRead.getSnapshotNames().size());
        assertTrue(persistedProcessInstanceRead.getSnapshotNames().contains("MyTestCondition"));

        processInstanceReadAgain.registerSnapshot("MyOtherTestCondition");
        processInstanceReadAgain.registerSnapshot("YetAnotherTestCondition");

        ProcessInstance processInstanceReadAgainSaved = repository.saveAndFlush(processInstanceReadAgain);
        entityManager.detach(processInstanceReadAgainSaved);
        Optional<ProcessInstance> processInstanceReadAgainAgainOptional = repository.findByOriginProcessId(processInstance.getOriginProcessId());

        assertTrue(processInstanceReadAgainAgainOptional.isPresent());
        ProcessInstance processInstanceReadAgainAgain = processInstanceReadAgainAgainOptional.get();
        assertEquals(3, processInstanceReadAgainAgain.getSnapshotNames().size());
        Iterator<String> it = processInstanceReadAgainAgain.getSnapshotNames().iterator();
        assertEquals("MyTestCondition", it.next());
        assertEquals("MyOtherTestCondition", it.next());
        assertEquals("YetAnotherTestCondition", it.next());
    }

    @Test
    void saveCompletedProcessInstance_thenFindIt_expectProcessCompletionToBePersistedSuccessfully() {
        ProcessInstance completedProcessInstance = ProcessInstanceStubs.createCompletedProcessInstance();

        ProcessInstance processInstanceSaved = repository.saveAndFlush(completedProcessInstance);
        entityManager.detach(processInstanceSaved);
        Optional<ProcessInstance> processInstanceReadOptional = repository.findByOriginProcessId(completedProcessInstance.getOriginProcessId());

        assertTrue(processInstanceReadOptional.isPresent());
        ProcessInstance persistedProcessInstanceRead = processInstanceReadOptional.get();
        assertTrue(persistedProcessInstanceRead.getProcessCompletion().isPresent());
        assertNotNull(persistedProcessInstanceRead.getProcessCompletion().get().getConclusion());
        assertNotNull(persistedProcessInstanceRead.getProcessCompletion().get().getCompletedAt());
    }

    @Test
    void findProcessInstanceTemplate() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        repository.saveAndFlush(processInstance);

        Optional<ProcessInstanceTemplate> processInstanceTemplate = repository.findProcessInstanceTemplate(processInstance.getOriginProcessId());

        assertThat(processInstanceTemplate).isPresent();
        assertThat(processInstanceTemplate.get().getTemplateName()).isEqualTo("template");
        assertThat(processInstanceTemplate.get().getTemplateHash()).isEqualTo("hash");
    }

    @Test
    void save_havingProcessDataEventData_expectEntityToBePersistedSuccessfully() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataProcessData(messageRepository);

        ProcessInstance processInstanceSaved = repository.saveAndFlush(processInstance);
        entityManager.detach(processInstanceSaved);
        Optional<ProcessInstance> processInstanceReadOptional = repository.findByOriginProcessId(processInstance.getOriginProcessId());

        assertTrue(processInstanceReadOptional.isPresent());
        ProcessInstance persistedProcessInstanceRead = processInstanceReadOptional.get();
        assertEquals(processInstance.getProcessData(), persistedProcessInstanceRead.getProcessData());
        assertEquals(1, persistedProcessInstanceRead.getTasks().size());
        assertEquals(processInstance.getTasks().getFirst().getOriginTaskId(), persistedProcessInstanceRead.getTasks().getFirst().getOriginTaskId());
    }

    @Test
    void findIdOriginProcessIdByStateAndModifiedAtBefore_processInstanceCompleted_processInstanceFound() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataProcessData(messageRepository);
        ReflectionTestUtils.setField(processInstance, "state", ProcessState.COMPLETED);
        repository.saveAndFlush(processInstance);

        repository.saveAndFlush(ProcessInstanceStubs.createProcessWithEventDataProcessData(messageRepository));

        final Slice<ProcessInstanceQueryResult> completedProcessInstances = repository.findIdOriginProcessIdByStateAndModifiedAtBefore(ProcessState.COMPLETED, ZonedDateTime.now().plusDays(1), Pageable.ofSize(100));
        assertEquals(1, completedProcessInstances.getNumberOfElements());
        final ProcessInstanceQueryResult processInstanceQueryResult = completedProcessInstances.iterator().next();
        assertEquals(processInstance.getId(), processInstanceQueryResult.getId());
        assertEquals(processInstance.getOriginProcessId(), processInstanceQueryResult.getOriginProcessId());
    }

    @Test
    void findIdOriginProcessIdByStateAndModifiedAtBefore_processInstanceCompletedButNotOld_processInstanceNotFound() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataProcessData(messageRepository);
        ReflectionTestUtils.setField(processInstance, "state", ProcessState.COMPLETED);
        repository.saveAndFlush(processInstance);

        repository.saveAndFlush(ProcessInstanceStubs.createProcessWithEventDataProcessData(messageRepository));

        final Slice<ProcessInstanceQueryResult> completedProcessInstances = repository.findIdOriginProcessIdByStateAndModifiedAtBefore(ProcessState.COMPLETED, ZonedDateTime.now().minusDays(1), Pageable.ofSize(100));
        assertEquals(0, completedProcessInstances.getNumberOfElements());
    }

    @Test
    void findUncompletedProcessInstanceOriginIdsByTemplateHashChanged() {
        // Given three process instances:
        // - Non-complete instance with modifiedAt now
        // - Complete instance with modifiedAt now
        // - Non-complete nstance with modifiedAt < 1 year old
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        ProcessInstance processInstanceSaved = repository.saveAndFlush(processInstance);
        ProcessInstance completedProcessInstance = ProcessInstanceStubs.createCompletedProcessInstance();
        repository.saveAndFlush(completedProcessInstance);
        ProcessInstance oldProcessInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        repository.saveAndFlush(oldProcessInstance);
        setModifiedAtToMinusOneYear(oldProcessInstance);
        ProcessTemplate template = processInstance.getProcessTemplate();

        // When retrieving all non-complete instances that are at most 1 day old
        ZonedDateTime lastModifiedAfter = processInstanceSaved.getModifiedAt().minusDays(1);
        Slice<String> results =
                repository.findUncompletedProcessInstanceOriginIdsByTemplateHashChanged(
                        template.getName(), "different-hash", lastModifiedAfter, Pageable.ofSize(5));

        // Then expect only the now modified non-completed instance to be returned
        assertEquals(1, results.getNumberOfElements());
        assertEquals(processInstance.getOriginProcessId(), results.getContent().getFirst(),
                "Only new non-completed process instance is found");

        // When retrieving all non-complete instances that are at most 1 day old with an up-to-date-has
        Slice<String> sameHashResults =
                repository.findUncompletedProcessInstanceOriginIdsByTemplateHashChanged(
                        template.getName(), template.getTemplateHash(), lastModifiedAfter, Pageable.ofSize(5));
        assertTrue(sameHashResults.isEmpty());
    }

    private void setModifiedAtToMinusOneYear(ProcessInstance oldProcessInstance) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaUpdate<ProcessInstance> update = builder.createCriteriaUpdate(ProcessInstance.class);
        Root<ProcessInstance> root = update.from(ProcessInstance.class);
        update.set("modifiedAt", ZonedDateTime.now().minusYears(1))
                .where(builder.equal(root.get("id"), oldProcessInstance.getId()));
        entityManager.createQuery(update).executeUpdate();
        entityManager.flush();
    }
    @Test
    void existsByOriginProcessId() {
        ProcessInstance processInstance = ProcessInstanceStubs.createCompletedProcessInstance();
        repository.saveAndFlush(processInstance);

        assertTrue(repository.existsByOriginProcessId(processInstance.getOriginProcessId()));
        assertFalse(repository.existsByOriginProcessId("notexists"));
    }

    @Test
    void getOriginProcessIdsByInstanceIds() {
        ProcessInstance processInstance = ProcessInstanceStubs.createCompletedProcessInstance();
        repository.saveAndFlush(processInstance);

        Set<String> originProcessIdsByInstanceIds = repository.getOriginProcessIdsByInstanceIds(Set.of(processInstance.getId()));

        assertEquals(
                originProcessIdsByInstanceIds,
                Set.of(processInstance.getOriginProcessId()));
    }

    @Test
    void deleteProcessInstanceProcessRelationsByRelatedOriginProcessIds() {
        ProcessInstance processInstance = ProcessInstanceStubs.createCompletedProcessInstance();
        repository.saveAndFlush(processInstance);

        repository
                .deleteProcessInstanceProcessRelationsByRelatedOriginProcessIds(Set.of(processInstance.getOriginProcessId()));

        assertTrue(
                repository.existsByOriginProcessId(processInstance.getOriginProcessId())
        );
    }

    @Test
    void testFindProcessInstanceByProcessData() {
        PageRequest pageRequest = PageRequest.of(0, 10);

        ProcessInstance processInstance1 = ProcessInstanceStubs.createProcessWithSingleTaskInstance(
                "someTemplateName",
                Set.of(new ProcessData(R1, V1), new ProcessData(R2, V2)));
        repository.save(processInstance1);

        ProcessInstance processInstance2 = ProcessInstanceStubs.createProcessWithSingleTaskInstance(
                "someTemplateName",
                Set.of(new ProcessData(R1, V1)));
        repository.save(processInstance2);

        ProcessInstance processInstance3 = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        repository.save(processInstance3);

        ProcessInstance processInstance4 = ProcessInstanceStubs.createProcessWithSingleTaskInstance(
                "someTemplateName",
                Set.of(new ProcessData(R2, V3), new ProcessData(R3, V3)));
        repository.save(processInstance4);

        // Find all: We expect 3
        List<ProcessInstance> processInstanceList = repository.findAll();
        assertEquals(4, processInstanceList.size());

        // Find with ProcessData Value V1
        Page<ProcessInstance> processInstancePage = repository.findDistinctByProcessData_value(V1, pageRequest);
        assertEquals(2, processInstancePage.getTotalElements());

        // Find with ProcessData Value V2
        processInstancePage = repository.findDistinctByProcessData_value(V2, pageRequest);
        assertEquals(1, processInstancePage.getTotalElements());

        // Find with not existing ProcessData Value
        processInstancePage = repository.findDistinctByProcessData_value("something that not exists", pageRequest);
        assertEquals(0, processInstancePage.getTotalElements());

        // Find distinct with ProcessData Value V3
        processInstancePage = repository.findDistinctByProcessData_value(V3, pageRequest);
        assertEquals(1, processInstancePage.getTotalElements());
    }

    @Test
    void findProcessInstanceSummaryByOriginProcessId() {
        ProcessInstance completedProcessInstance = ProcessInstanceStubs.createCompletedProcessInstance();

        ProcessInstance processInstanceSaved = repository.saveAndFlush(completedProcessInstance);
        entityManager.detach(processInstanceSaved);
        Optional<ProcessInstanceSummary> processInstanceSummaryOptional = repository.findProcessInstanceSummaryByOriginProcessId(completedProcessInstance.getOriginProcessId());

        assertTrue(processInstanceSummaryOptional.isPresent());
        ProcessInstanceSummary processInstanceSummary = processInstanceSummaryOptional.get();
        assertEquals(processInstanceSaved.getProcessTemplateName(), processInstanceSummary.getProcessTemplateName());
        assertEquals(processInstanceSaved.getOriginProcessId(), processInstanceSummary.getOriginProcessId());
        assertEquals(processInstanceSaved.getState(), processInstanceSummary.getState());
    }

    @Test
    void areAllTasksInFinalState_singleCompletedTask_expectTrue() {
        ProcessInstance processInstance = ProcessInstanceStubs.createCompletedProcessInstance();
        ProcessInstance processInstanceSaved = repository.saveAndFlush(processInstance);

        boolean result = repository.areAllTasksInFinalState(processInstanceSaved.getId());

        assertThat(result)
                .isTrue();
    }

    @Test
    void areAllTasksInFinalState_twoPlannedTasks_expectFalse() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithTwoPlannedTaskInstances();
        ProcessInstance processInstanceSaved = repository.saveAndFlush(processInstance);

        boolean result = repository.areAllTasksInFinalState(processInstanceSaved.getId());

        assertThat(result)
                .isFalse();
    }

    @Test
    void areAllTasksInFinalState_twoTasksPlannedAndCompleted_expectFalse() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithTwoTaskInstancesPlannedAndCompleted();
        ProcessInstance processInstanceSaved = repository.saveAndFlush(processInstance);

        boolean result = repository.areAllTasksInFinalState(processInstanceSaved.getId());

        assertThat(result)
                .isFalse();
    }

    @Test
    void areAllTasksInFinalState_twoCompletedTasks_expectTrue() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithTwoCompletedTaskInstances();
        ProcessInstance processInstanceSaved = repository.saveAndFlush(processInstance);

        boolean result = repository.areAllTasksInFinalState(processInstanceSaved.getId());

        assertThat(result)
                .isTrue();
    }

    @Test
    void areAllTasksInFinalState_twoFinalStateTasks_expectTrue() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithTwoTaskInstancesNotRequiredAndDeleted();
        ProcessInstance processInstanceSaved = repository.saveAndFlush(processInstance);

        boolean result = repository.areAllTasksInFinalState(processInstanceSaved.getId());

        assertThat(result)
                .isTrue();
    }

    @Test
    void areAllTasksInFinalState_noTasksExpectFalse() {
        ProcessInstance completedProcessInstance = ProcessInstanceStubs.createProcessWithoutTaskInstance();
        ProcessInstance processInstanceSaved = repository.saveAndFlush(completedProcessInstance);
        assertThat(processInstanceSaved.getTasks()).isEmpty();

        boolean result = repository.areAllTasksInFinalState(processInstanceSaved.getId());

        assertThat(result)
                .isFalse();
    }

    @Test
    void findMessageReferencesByMessageType_multipleMessages_returnsLatest() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        ProcessInstance savedProcessInstance = repository.saveAndFlush(processInstance);

        Message oldMessage = saveEvent("TestMessageType", Set.of(), Set.of(), "traceId1", ZonedDateTime.now().minusMinutes(10));
        Message newMessage = saveEvent("TestMessageType", Set.of(), Set.of(), "traceId2", ZonedDateTime.now());
        Message otherTypeMessage = saveEvent("OtherMessageType", Set.of(), Set.of(), "traceId3");

        jpaRepositoryTestSupport.createAndPersist(oldMessage, savedProcessInstance);
        jpaRepositoryTestSupport.createAndPersist(newMessage, savedProcessInstance);
        jpaRepositoryTestSupport.createAndPersist(otherTypeMessage, savedProcessInstance);
        messageReferenceJpaRepository.flush();

        List<MessageReference> result = repository.findMessageReferencesByMessageType(
                savedProcessInstance.getId(), "TestMessageType", PageRequest.of(0, 1));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getMessageId()).isEqualTo(newMessage.getId());
    }

    @Test
    void findMessageReferencesByMessageType_noMatchingMessages_returnsEmpty() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        ProcessInstance savedProcessInstance = repository.saveAndFlush(processInstance);

        Message message = saveEvent("SomeMessageType", Set.of(), Set.of(), "traceId1");
        jpaRepositoryTestSupport.createAndPersist(message, savedProcessInstance);
        messageReferenceJpaRepository.flush();

        List<MessageReference> result = repository.findMessageReferencesByMessageType(
                savedProcessInstance.getId(), "NonExistentType", PageRequest.of(0, 1));

        assertThat(result).isEmpty();
    }

    @Test
    void findMessageReferencesByMessageTypeAndOriginTaskId_multipleMessages_returnsLatestWithMatchingTaskId() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        ProcessInstance savedProcessInstance = repository.saveAndFlush(processInstance);
        String templateName = processInstance.getProcessTemplateName();

        Message oldMessage = saveEvent("TestMessageType",
                Set.of(),
                Set.of(OriginTaskId.from(templateName, "task-1")),
                "traceId1", ZonedDateTime.now().minusMinutes(10));
        Message newMessage = saveEvent("TestMessageType",
                Set.of(),
                Set.of(OriginTaskId.from(templateName, "task-1")),
                "traceId2", ZonedDateTime.now());
        Message differentTaskMessage = saveEvent("TestMessageType",
                Set.of(),
                Set.of(OriginTaskId.from(templateName, "task-2")),
                "traceId3");

        jpaRepositoryTestSupport.createAndPersist(oldMessage, savedProcessInstance);
        jpaRepositoryTestSupport.createAndPersist(newMessage, savedProcessInstance);
        jpaRepositoryTestSupport.createAndPersist(differentTaskMessage, savedProcessInstance);
        messageReferenceJpaRepository.flush();

        List<MessageReference> result = repository.findMessageReferencesByMessageTypeAndOriginTaskId(
                savedProcessInstance.getId(), "TestMessageType", "task-1", PageRequest.of(0, 1));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getMessageId()).isEqualTo(newMessage.getId());
    }

    @Test
    void findMessageReferencesByMessageTypeAndOriginTaskId_noMatchingTaskId_returnsEmpty() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        ProcessInstance savedProcessInstance = repository.saveAndFlush(processInstance);
        String templateName = processInstance.getProcessTemplateName();

        Message message = saveEvent("TestMessageType",
                Set.of(),
                Set.of(OriginTaskId.from(templateName, "task-1")),
                "traceId1");
        jpaRepositoryTestSupport.createAndPersist(message, savedProcessInstance);
        messageReferenceJpaRepository.flush();

        List<MessageReference> result = repository.findMessageReferencesByMessageTypeAndOriginTaskId(
                savedProcessInstance.getId(), "TestMessageType", "non-existent-task", PageRequest.of(0, 1));

        assertThat(result).isEmpty();
    }

    @Test
    void findMessageReferencesByMessageTypeAndOriginTaskId_wrongTemplateName_returnsEmpty() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        ProcessInstance savedProcessInstance = repository.saveAndFlush(processInstance);

        Message message = saveEvent("TestMessageType",
                Set.of(),
                Set.of(OriginTaskId.from("other-template", "task-1")),
                "traceId1");
        jpaRepositoryTestSupport.createAndPersist(message, savedProcessInstance);
        messageReferenceJpaRepository.flush();

        List<MessageReference> result = repository.findMessageReferencesByMessageTypeAndOriginTaskId(
                savedProcessInstance.getId(), "TestMessageType", "task-1", PageRequest.of(0, 1));

        assertThat(result).isEmpty();
    }

    private Message saveEvent(String name, Set<MessageData> messageData, Set<OriginTaskId> originTaskIds, String traceId) {
        return saveEvent(name, messageData, originTaskIds, traceId, ZonedDateTime.now().truncatedTo(ChronoUnit.MILLIS));
    }

    private Message saveEvent(String name, Set<MessageData> messageData, Set<OriginTaskId> originTaskIds,
                              String traceId, ZonedDateTime createdAt) {
        Message message = Message.messageBuilder()
                .messageId(Generators.timeBasedEpochGenerator().generate().toString())
                .idempotenceId(Generators.timeBasedEpochGenerator().generate().toString())
                .messageName(name)
                .messageData(messageData)
                .originTaskIds(originTaskIds)
                .createdAt(createdAt)
                .messageCreatedAt(ZonedDateTime.now().truncatedTo(ChronoUnit.MILLIS))
                .traceId(traceId)
                .build();
        return messageRepository.save(message);
    }

}
