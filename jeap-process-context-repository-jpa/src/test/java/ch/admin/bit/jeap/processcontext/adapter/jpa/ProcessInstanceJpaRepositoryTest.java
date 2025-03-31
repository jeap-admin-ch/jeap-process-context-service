package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.message.MessageData;
import ch.admin.bit.jeap.processcontext.domain.message.MessageRepository;
import ch.admin.bit.jeap.processcontext.domain.message.OriginTaskId;
import ch.admin.bit.jeap.processcontext.domain.processinstance.*;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import com.fasterxml.uuid.Generators;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;
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
import java.util.*;

import static java.util.stream.Collectors.*;
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
    @SuppressWarnings("unused")
    private ProcessTemplateRepository processTemplateRepository;
    @Autowired
    private ProcessInstanceJpaRepository repository;
    @Autowired
    private MessageRepository messageRepository;

    @Test
    void testFindUncompletedProcessInstancesHavingProcessData() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataProcessDataAndRelations(messageRepository);
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
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataProcessDataAndRelations(messageRepository);
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
        assertEquals(processInstance.getTasks().get(0).getOriginTaskId(), persistedProcessInstanceRead.getTasks().get(0).getOriginTaskId());
        assertTrue(persistedProcessInstanceRead.getProcessCompletion().isEmpty());
    }

    @Test
    void testNextSnapshotVersion() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        assertEquals(0, processInstance.getLatestSnapshotVersion());
        Integer snapshotVersionPrePersisting = repository.getLatestProcessSnapshotVersion(processInstance.getOriginProcessId());
        assertThat(snapshotVersionPrePersisting).isNull();

        ProcessInstance processInstanceSaved = repository.saveAndFlush(processInstance);
        entityManager.detach(processInstanceSaved);
        Optional<ProcessInstance> processInstanceReadOptional = repository.findByOriginProcessId(processInstance.getOriginProcessId());
        Integer snapshotVersionAfterPersisting = repository.getLatestProcessSnapshotVersion(processInstance.getOriginProcessId());

        assertThat(snapshotVersionAfterPersisting).isZero();
        assertTrue(processInstanceReadOptional.isPresent());
        ProcessInstance persistedProcessInstanceRead = processInstanceReadOptional.get();
        assertEquals(0, persistedProcessInstanceRead.getLatestSnapshotVersion());
        assertEquals(1, persistedProcessInstanceRead.nextSnapshotVersion());

        ProcessInstance processInstanceReadSaved = repository.saveAndFlush(persistedProcessInstanceRead);
        entityManager.detach(processInstanceReadSaved);
        Optional<ProcessInstance> processInstanceReadAgainOptional = repository.findByOriginProcessId(processInstance.getOriginProcessId());
        Integer snapshotVersionAfterNext = repository.getLatestProcessSnapshotVersion(processInstance.getOriginProcessId());

        assertThat(snapshotVersionAfterNext).isEqualTo(1);
        assertTrue(processInstanceReadAgainOptional.isPresent());
        assertEquals(1, processInstanceReadAgainOptional.get().getLatestSnapshotVersion());
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
    void save_havingProcessDataEventDataAndRelations_expectEntityToBePersistedSuccessfully() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataProcessDataAndRelations(messageRepository);

        ProcessInstance processInstanceSaved = repository.saveAndFlush(processInstance);
        entityManager.detach(processInstanceSaved);
        Optional<ProcessInstance> processInstanceReadOptional = repository.findByOriginProcessId(processInstance.getOriginProcessId());

        assertTrue(processInstanceReadOptional.isPresent());
        ProcessInstance persistedProcessInstanceRead = processInstanceReadOptional.get();
        assertEquals(processInstance.getProcessData(), persistedProcessInstanceRead.getProcessData());
        assertEquals(1, persistedProcessInstanceRead.getTasks().size());
        assertEquals(processInstance.getTasks().get(0).getOriginTaskId(), persistedProcessInstanceRead.getTasks().get(0).getOriginTaskId());
    }

    @Test
    void getProcessTemplateNameByOriginProcessId() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        repository.save(processInstance);

        String templateName = repository.getProcessTemplateNameByOriginProcessId(processInstance.getOriginProcessId())
                .orElseThrow();

        assertEquals(processInstance.getProcessTemplateName(), templateName);
    }

    @Test
    void findIdOriginProcessIdByStateAndModifiedAtBefore_processInstanceCompleted_processInstanceFound() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataProcessDataAndRelations(messageRepository);
        ReflectionTestUtils.setField(processInstance, "state", ProcessState.COMPLETED);
        repository.saveAndFlush(processInstance);

        repository.saveAndFlush(ProcessInstanceStubs.createProcessWithEventDataProcessDataAndRelations(messageRepository));

        final Slice<ProcessInstanceQueryResult> completedProcessInstances = repository.findIdOriginProcessIdByStateAndModifiedAtBefore(ProcessState.COMPLETED, ZonedDateTime.now().plusDays(1), Pageable.ofSize(100));
        assertEquals(1, completedProcessInstances.getNumberOfElements());
        final ProcessInstanceQueryResult processInstanceQueryResult = completedProcessInstances.iterator().next();
        assertEquals(processInstance.getId(), processInstanceQueryResult.getId());
        assertEquals(processInstance.getOriginProcessId(), processInstanceQueryResult.getOriginProcessId());
    }

    @Test
    void findIdOriginProcessIdByStateAndModifiedAtBefore_processInstanceCompletedButNotOld_processInstanceNotFound() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataProcessDataAndRelations(messageRepository);
        ReflectionTestUtils.setField(processInstance, "state", ProcessState.COMPLETED);
        repository.saveAndFlush(processInstance);

        repository.saveAndFlush(ProcessInstanceStubs.createProcessWithEventDataProcessDataAndRelations(messageRepository));

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
        assertEquals(processInstance.getOriginProcessId(), results.getContent().get(0),
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
    void setInitialTemplateHashIfNotSet() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        repository.saveAndFlush(processInstance);
        setTemplateHashToNull(processInstance);

        assertNull(repository.getReferenceById(processInstance.getId()).getProcessTemplateHash());

        ProcessTemplate template = processInstance.getProcessTemplate();
        repository.setInitialTemplateHashIfNotSet(template.getName(), "updated-hash");
        entityManager.clear();

        assertEquals("updated-hash",
                repository.getReferenceById(processInstance.getId()).getProcessTemplateHash());
    }

    private void setTemplateHashToNull(ProcessInstance processInstance) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaUpdate<ProcessInstance> update = builder.createCriteriaUpdate(ProcessInstance.class);
        Root<ProcessInstance> root = update.from(ProcessInstance.class);
        update.set("processTemplateHash", null)
                .where(builder.equal(root.get("id"), processInstance.getId()));
        entityManager.createQuery(update).executeUpdate();
        entityManager.flush();
        entityManager.clear();
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
    void testFindEventReferences() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        String templateName = processInstance.getProcessTemplateName();
        Message e1 = saveEvent("e1",
                Set.of(createEventData(templateName, "d1-a"), createEventData(templateName, "d1-b")),
                Set.of(OriginTaskId.from(templateName, "i1-a"), OriginTaskId.from(templateName, "i1-b")), "traceIdFore1");
        MessageReferenceMessageDTO reference1 = processInstance.addMessage(e1);
        Message e2 = saveEvent("e2",
                Set.of(createEventData(templateName, "d2")),
                Set.of(OriginTaskId.from(templateName, "i2")), "traceIdFore2");
        MessageReferenceMessageDTO reference2 = processInstance.addMessage(e2);
        Message e3 = saveEvent("e3", Set.of(), Set.of(), "traceIdFore3");
        MessageReferenceMessageDTO reference3 = processInstance.addMessage(e3);
        saveEvent("e4",
                Set.of(createEventData("other", "d4")),
                Set.of(OriginTaskId.from("other", "i4")), "traceIdFore4");
        ProcessInstance otherProcessInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        Message eother = saveEvent("eother",
                Set.of(createEventData(templateName, "dother")),
                Set.of(OriginTaskId.from(templateName, "idother")), "traceIdForeother");
        otherProcessInstance.addMessage(eother);
        repository.save(otherProcessInstance);
        repository.saveAndFlush(processInstance);


        // EventReferencesEventData
        List<MessageReferenceMessageData> messageReferenceMessageData = repository.findMessageReferencesMessageData(processInstance.getId());

        assertThat(messageReferenceMessageData).hasSize(3);
        Map<UUID, Set<MessageReferenceMessageDataDTO>> eventDataByReferenceId = messageReferenceMessageData.stream().collect(groupingBy(MessageReferenceMessageData::getMessageReferenceId,
                mapping(d -> MessageReferenceMessageDataDTO.builder()
                        .messageDataKey(d.getMessageDataKey())
                        .messageDataValue(d.getMessageDataValue())
                        .messageDataRole(d.getMessageDataRole())
                        .build(), toSet())));
        assertThat(eventDataByReferenceId).hasSize(2);
        assertThat(eventDataByReferenceId.get(reference1.getMessageReferenceId())).hasSize(2);
        assertThat(eventDataByReferenceId).containsEntry(reference1.getMessageReferenceId(), reference1.getMessageData());
        assertThat(eventDataByReferenceId.get(reference2.getMessageReferenceId())).hasSize(1);
        assertThat(eventDataByReferenceId).containsEntry(reference2.getMessageReferenceId(), reference2.getMessageData());


        // EventReferencesRelatedOriginTaskIds
        List<MessageReferenceRelatedTaskId> eventReferenceTaskIds = repository.findMessageReferencesRelatedOriginTaskIds(processInstance.getId());

        assertThat(eventReferenceTaskIds).hasSize(3);
        Map<UUID, Set<String>> taskIdsByReferenceId = eventReferenceTaskIds.stream().collect(groupingBy(MessageReferenceRelatedTaskId::getMessageReferenceId,
                mapping(MessageReferenceRelatedTaskId::getRelatedOriginTaskId, toSet())));
        assertThat(taskIdsByReferenceId).hasSize(2);
        assertThat(taskIdsByReferenceId.get(reference1.getMessageReferenceId())).hasSize(2);
        assertThat(taskIdsByReferenceId).containsEntry(reference1.getMessageReferenceId(), reference1.getRelatedOriginTaskIds());
        assertThat(taskIdsByReferenceId.get(reference2.getMessageReferenceId())).hasSize(1);
        assertThat(taskIdsByReferenceId).containsEntry(reference2.getMessageReferenceId(), reference2.getRelatedOriginTaskIds());


        // EventReferencesEvents
        List<MessageReferenceMessage> events = repository.findMessageReferencesMessages(processInstance.getId());

        assertThat(events).hasSize(3);
        assertThat(events.stream().map(e -> MessageReferenceMessageDTO.builder()
                .messageReferenceId(e.getMessageReferenceId())
                .messageId(e.getMessageId())
                .messageName(e.getMessageName())
                .traceId(e.getTraceId())
                .messageReceivedAt(e.getMessageReceivedAt().truncatedTo(ChronoUnit.MILLIS).withFixedOffsetZone())
                .messageCreatedAt(e.getMessageCreatedAt().truncatedTo(ChronoUnit.MILLIS).withFixedOffsetZone())
                .messageData(Set.of())
                .relatedOriginTaskIds(Set.of())
                .build()).collect(toSet())
        ).containsOnly(
                toEventReferenceEventDTO(reference1.getMessageReferenceId(), e1),
                toEventReferenceEventDTO(reference2.getMessageReferenceId(), e2),
                toEventReferenceEventDTO(reference3.getMessageReferenceId(), e3)
        );
    }

    private MessageReferenceMessageDTO toEventReferenceEventDTO(UUID eventReferenceId, Message message) {
        return MessageReferenceMessageDTO.builder()
                .messageReferenceId(eventReferenceId)
                .messageId(message.getId())
                .messageName(message.getMessageName())
                .messageReceivedAt(message.getReceivedAt().truncatedTo(ChronoUnit.MILLIS).withFixedOffsetZone())
                .messageCreatedAt(message.getMessageCreatedAt().truncatedTo(ChronoUnit.MILLIS).withFixedOffsetZone())
                .traceId(message.getTraceId())
                .messageData(Set.of())
                .relatedOriginTaskIds(Set.of())
                .build();
    }

    private MessageData createEventData(String templateName, String key) {
        return MessageData.builder().key(key).value(key + "-value").templateName(templateName).build();
    }

    private Message saveEvent(String name, Set<MessageData> messageData, Set<OriginTaskId> originTaskIds, String traceId) {
        Message message = Message.messageBuilder()
                .messageId(Generators.timeBasedEpochGenerator().generate().toString())
                .idempotenceId(Generators.timeBasedEpochGenerator().generate().toString())
                .messageName(name)
                .messageData(messageData)
                .originTaskIds(originTaskIds)
                .createdAt(ZonedDateTime.now().truncatedTo(ChronoUnit.MILLIS))
                .messageCreatedAt(ZonedDateTime.now().truncatedTo(ChronoUnit.MILLIS))
                .traceId(traceId)
                .build();
        return messageRepository.save(message);
    }

}
