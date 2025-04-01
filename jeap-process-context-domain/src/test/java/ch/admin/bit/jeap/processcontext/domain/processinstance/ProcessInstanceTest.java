package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.message.MessageData;
import ch.admin.bit.jeap.processcontext.domain.message.OriginTaskId;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskCardinality;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskLifecycle;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskType;
import ch.admin.bit.jeap.processcontext.plugin.api.condition.MilestoneCondition;
import ch.admin.bit.jeap.processcontext.plugin.api.condition.ProcessSnapshotCondition;
import ch.admin.bit.jeap.processcontext.plugin.api.condition.ProcessSnapshotConditionResult;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessCompletion;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext;
import com.fasterxml.uuid.Generators;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.time.ZonedDateTime;
import java.util.*;

import static ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceStubs.createProcessWithProcessSnapshotCondition;
import static ch.admin.bit.jeap.processcontext.domain.processinstance.TaskState.*;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ProcessInstanceTest {

    @Test
    void startProcess() {
        TaskType mandatoryTaskType = TaskType.builder()
                .name("mandatory")
                .lifecycle(TaskLifecycle.STATIC)
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .build();
        TaskType multipleTaskType = TaskType.builder()
                .name("multiple")
                .lifecycle(TaskLifecycle.DYNAMIC)
                .cardinality(TaskCardinality.MULTI_INSTANCE)
                .build();
        MilestoneCondition milestoneCondition = Mockito.mock(MilestoneCondition.class);
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name("template")
                .templateHash("hash")
                .taskTypes(List.of(mandatoryTaskType, multipleTaskType))
                .milestones(Map.of("milestone", milestoneCondition))
                .build();

        ProcessInstance processInstance = ProcessInstance.startProcess("id", processTemplate, emptySet());

        TaskInstance firstTask = processInstance.getTasks().getFirst();
        assertEquals(1, processInstance.getTasks().size());
        assertNull(null, firstTask.getOriginTaskId());
        assertEquals(processInstance, firstTask.getProcessInstance());
        assertEquals(mandatoryTaskType, firstTask.requireTaskType());
        assertEquals(PLANNED, firstTask.getState());
        assertEquals(1, processInstance.getMilestones().size());
        Milestone milestone = processInstance.getMilestones().getFirst();
        assertEquals("milestone", milestone.getName());
        assertFalse(milestone.isReached());
    }

    @Test
    void completeTask() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleDynamicTaskInstance();

        completeTask(processInstance, "1");

        assertSame(TaskState.COMPLETED, processInstance.getTasks().getFirst().getState());
        assertSame(ProcessState.COMPLETED, processInstance.getState());
    }

    @Test
    void updateProcessState_whenMandatoryTaskInstanceIsCompleted_thenShouldUpdateProcessStateToCompleted() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleDynamicTaskInstance();
        TaskInstance task = processInstance.getTasks().getFirst();

        completeTask(processInstance, task.getOriginTaskId());

        assertSame(ProcessState.COMPLETED, processInstance.getState());
    }

    @Test
    void getProcessCompletion_whenInProgess_thenCompletionNotPresent() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleStaticTaskInstance();
        assertSame(ProcessState.STARTED, processInstance.getState());
        assertFalse(processInstance.getProcessCompletion().isPresent());
    }

    @Test
    void getProcessCompletion_whenOldCompletedProcessInstanceWithoutCompletionData_thenCompletionDerivedFromProcessInstancePresent() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleStaticTaskInstance();
        ReflectionTestUtils.setField(processInstance, "state", ProcessState.COMPLETED);
        ReflectionTestUtils.setField(processInstance, "modifiedAt", ZonedDateTime.now());

        Optional<ProcessCompletion> completion = processInstance.getProcessCompletion();

        assertTrue(completion.isPresent());
        assertEquals(ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessCompletionConclusion.SUCCEEDED, completion.get().getConclusion());
        assertEquals(processInstance.getModifiedAt(), completion.get().getCompletedAt());
    }

    @Test
    void getProcessCompletion_whenCompleted_thenCompletionPresent() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleDynamicTaskInstance();
        TaskInstance task = processInstance.getTasks().getFirst();
        completeTask(processInstance, task.getOriginTaskId());

        Optional<ProcessCompletion> completion = processInstance.getProcessCompletion();

        assertTrue(completion.isPresent());
        assertEquals(ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessCompletionConclusion.SUCCEEDED, completion.get().getConclusion());
        assertNotNull(completion.get().getCompletedAt());
    }

    @Test
    void updateProcessState_whenNotAllMultipleTaskInstancesAreCompleted_thenShouldKeepProcessStateAsStarted_whenAllCompleted_thenProcessCompleted() {
        TaskType mandatoryTaskType = TaskType.builder()
                .name("single")
                .lifecycle(TaskLifecycle.STATIC)
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .completedByDomainEvent("messageName2")
                .build();
        TaskType dynamicTaskType = TaskType.builder()
                .name("dynamic")
                .lifecycle(TaskLifecycle.DYNAMIC)
                .cardinality(TaskCardinality.MULTI_INSTANCE)
                .plannedByDomainEvent("messageName")
                .completedByDomainEvent("messageName2")
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name("template")
                .templateHash("hash")
                .taskTypes(List.of(mandatoryTaskType, dynamicTaskType))
                .build();
        ProcessInstance processInstance = ProcessInstance.startProcess(Generators.timeBasedEpochGenerator().generate().toString(), processTemplate, emptySet());

        processInstance.planDomainEventTask(dynamicTaskType, "multiple-id-1", ZonedDateTime.now(), null);
        processInstance.planDomainEventTask(dynamicTaskType, "multiple-id-2", ZonedDateTime.now(), null);

        completeTask(processInstance, "mandatory-id", "messageName2");
        assertSame(ProcessState.STARTED, processInstance.getState());

        completeTask(processInstance, "multiple-id-1", "messageName2");
        assertSame(ProcessState.STARTED, processInstance.getState());

        completeTask(processInstance, "multiple-id-2", "messageName2");
        assertSame(ProcessState.COMPLETED, processInstance.getState());
    }

    void completeTask(ProcessInstance processInstance, String originTaskId, String messageName) {
        processInstance.addMessage(Message.messageBuilder()
                .messageId(UUID.randomUUID().toString())
                .idempotenceId("idempotenceId")
                .messageName(messageName)
                .originTaskIds(Set.of(OriginTaskId.from("template", originTaskId)))
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .build());
        processInstance.evaluateCompletedTasks(ZonedDateTime.now());
    }

    void completeTask(ProcessInstance processInstance, String originTaskId) {
        completeTask(processInstance, originTaskId, "messageName");
    }

    @Test
    void evaluateMilestones() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithTwoTasksAndThreeMilestoneConditions();
        assertEquals(3, processInstance.getMilestones().size());

        // Initially, no milestones should be reached
        processInstance.evaluateReachedMilestones();
        assertEquals(3, processInstance.getMilestones().size());
        assertNoMilestonesReached(processInstance);

        // After completion of task 1, milestone 1 should be reached according to its condition
        completeTask(processInstance, "1");
        processInstance.evaluateReachedMilestones();
        assertMilestonesReached(Set.of(ProcessInstanceStubs.milestone1), processInstance);

        // After completion of task 2, milestone 2 should be reached according to its condition
        // MilestoneDone, which depends on task 1 & 2 to be complete, should now be reached as well
        completeTask(processInstance, "2", "messageName2");
        processInstance.evaluateReachedMilestones();
        Set<String> expectedMilestoneNames = Set.of(
                ProcessInstanceStubs.milestone1,
                ProcessInstanceStubs.milestone2,
                ProcessInstanceStubs.milestoneDone);
        assertMilestonesReached(expectedMilestoneNames, processInstance);
        assertEquals(3, processInstance.getMilestones().size());
    }

    @Test
    void setProcessTemplate() throws Exception {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleDynamicTaskInstance();
        ProcessTemplate processTemplate = processInstance.getProcessTemplate();
        setProcessTemplateToNull(processInstance);

        processInstance.setProcessTemplate(processTemplate);

        assertSame(processTemplate, processInstance.getProcessTemplate());
        assertSame(processTemplate.getTaskTypeByName(ProcessInstanceStubs.singleTaskName).orElseThrow(),
                processInstance.getTasks().getFirst().requireTaskType());
    }

    @Test
    void setProcessTemplate_shouldThrowIfAlreadySet() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleDynamicTaskInstance();

        assertThrows(IllegalStateException.class, () ->
                processInstance.setProcessTemplate(processInstance.getProcessTemplate()));
    }

    private static void assertNoMilestonesReached(ProcessInstance processInstance) {
        assertTrue(processInstance.getMilestones().stream().noneMatch(Milestone::isReached));
    }

    private static void assertMilestonesReached(Set<String> expectedMilestoneNames, ProcessInstance processInstance) {
        Set<String> actualMilestoneNames = processInstance.getReachedMilestones();
        assertEquals(expectedMilestoneNames, actualMilestoneNames);
    }

    /**
     * Simulate empty template properties after loading the domain object from persistent state
     */
    private static void setProcessTemplateToNull(ProcessInstance processInstance) throws Exception {
        String fieldName = "processTemplate";
        setFieldToNull(processInstance, fieldName);
        assertNull(processInstance.getProcessTemplate());
        for (TaskInstance task : processInstance.getTasks()) {
            setFieldToOptionalEmpty(task, "taskType");
        }
        for (Milestone milestone : processInstance.getMilestones()) {
            setFieldToNull(milestone, "condition");
        }
    }

    private static void setFieldToNull(Object obj, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field processTemplateField = obj.getClass().getDeclaredField(fieldName);
        processTemplateField.setAccessible(true);
        processTemplateField.set(obj, null);
    }

    private static void setFieldToOptionalEmpty(Object obj, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field processTemplateField = obj.getClass().getDeclaredField(fieldName);
        processTemplateField.setAccessible(true);
        processTemplateField.set(obj, Optional.empty());
    }

    @Test
    void copyEventDataToProcessData_NoTemplateDefined() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleDynamicTaskInstance();
        MessageData messageData = MessageData.builder()
                .templateName(processInstance.getProcessTemplateName())
                .key("sourceEventDatakey")
                .value("someValue")
                .role("someRole")
                .build();
        Message domainMessage = Message.messageBuilder()
                .messageName("sourceEventName")
                .messageId("eventId")
                .idempotenceId("idempotenceId")
                .originTaskIds(Set.of())
                .messageData(Set.of(messageData))
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .build();

        processInstance.addMessage(domainMessage);

        Set<ProcessData> processDataSet = processInstance.getProcessData();
        assertEquals(0, processDataSet.size());
    }

    @Test
    void copyEventDataToProcessData_NoEventNameMatches() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventData();
        MessageData messageData = MessageData.builder()
                .templateName(processInstance.getProcessTemplateName())
                .key("sourceEventDatakey")
                .value("someValue")
                .role("someRole")
                .build();
        Message domainMessage = Message.messageBuilder()
                .messageName("sourceEventName")
                .idempotenceId("idempotenceId")
                .messageId("eventId")
                .messageData(Set.of(messageData))
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .build();

        processInstance.addMessage(domainMessage);

        Set<ProcessData> processDataSet = processInstance.getProcessData();
        assertEquals(0, processDataSet.size());
    }

    @Test
    void copyEventDataToProcessData() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventData();
        String templateName = processInstance.getProcessTemplateName();
        MessageData messageData1 = new MessageData(templateName, "sourceEventDataKey", "someValue", "someRole");
        MessageData messageData2 = new MessageData(templateName, "sourceEventDataKey", "someValueOtherValue", "someOtherRole");
        MessageData messageData3 = new MessageData(templateName, "anotherSourceEventDataKey", "anotherValue");
        Message domainMessage1 = Message.messageBuilder()
                .messageName("sourceEventName")
                .idempotenceId("idempotenceId")
                .messageId("eventId")
                .messageData(Set.of(messageData1, messageData2))
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .build();
        Message domainMessage2 = Message.messageBuilder()
                .messageName("anotherSourceEventName")
                .idempotenceId("idempotenceId")
                .messageId("eventId")
                .messageData(Set.of(messageData3))
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .build();

        processInstance.addMessage(domainMessage1);
        processInstance.addMessage(domainMessage2);

        Set<ProcessData> processDataSet = processInstance.getProcessData();
        assertTrue(processDataSet.contains(new ProcessData("targetKeyName", "someValue", "someRole")));
        assertTrue(processDataSet.contains(new ProcessData("targetKeyName", "someValueOtherValue", "someOtherRole")));
        assertTrue(processDataSet.contains(new ProcessData("anotherTargetKeyName", "anotherValue")));
        assertEquals(3, processDataSet.size());
    }

    @Test
    void evaluateRelations() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventData();
        String templateName = processInstance.getProcessTemplateName();
        MessageData messageData1 = new MessageData(templateName, "sourceEventDataKey", "someValue", "someRole");
        MessageData messageData2 = new MessageData(templateName, "sourceEventDataKey", "someValueOtherValue", "someOtherRole");
        MessageData messageData3 = new MessageData(templateName, "anotherSourceEventDataKey", "anotherValue");
        Message domainMessage1 = Message.messageBuilder()
                .messageName("sourceEventName")
                .messageId("eventId")
                .idempotenceId("idempotenceId")
                .messageData(Set.of(messageData1, messageData2))
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .build();
        Message domainMessage2 = Message.messageBuilder()
                .messageName("anotherSourceEventName")
                .idempotenceId("idempotenceId")
                .messageId("eventId")
                .messageData(Set.of(messageData3))
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .build();
        processInstance.addMessage(domainMessage1);
        processInstance.addMessage(domainMessage2);

        processInstance.evaluateRelations();

        Set<Relation> relations = processInstance.getRelations();
        assertTrue(relations.contains(Relation.builder()
                .systemId("ch.admin.test.System")
                .objectType("ch.admin.bit.entity.Some")
                .objectId("someValue")
                .subjectType("ch.admin.bit.entity.Other")
                .subjectId("someValueOtherValue")
                .predicateType("ch.admin.bit.test.predicate.Declares")
                .build()));
        assertTrue(relations.contains(Relation.builder()
                .systemId("ch.admin.test.System")
                .objectType("ch.admin.bit.entity.Foo")
                .objectId("someValueOtherValue")
                .subjectType("ch.admin.bit.entity.Bar")
                .subjectId("anotherValue")
                .predicateType("ch.admin.bit.test.predicate.Knows")
                .build()));
        assertTrue(relations.contains(Relation.builder()
                .systemId("ch.admin.test.System")
                .objectType("ch.admin.bit.entity.Foo")
                .objectId("someValue")
                .subjectType("ch.admin.bit.entity.Bar")
                .subjectId("anotherValue")
                .predicateType("ch.admin.bit.test.predicate.Knows")
                .build()));
        assertEquals(3, relations.size());
    }

    @Test
    void evaluateRelationsJoinByRole() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataAndJoinByRole();
        String templateName = processInstance.getProcessTemplateName();
        MessageData messageData1 = new MessageData(templateName, "sourceEventDataKey", "someValue", "someRole");
        MessageData messageData3 = new MessageData(templateName, "anotherSourceEventDataKey", "anotherValue", "someRole");
        Message domainMessage1 = Message.messageBuilder()
                .messageName("sourceEventName")
                .messageId("eventId")
                .idempotenceId("idempotenceId")
                .messageData(Set.of(messageData1))
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .build();
        Message domainMessage2 = Message.messageBuilder()
                .messageName("anotherSourceEventName")
                .idempotenceId("idempotenceId")
                .messageId("eventId")
                .messageData(Set.of(messageData3))
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .build();
        processInstance.addMessage(domainMessage1);
        processInstance.addMessage(domainMessage2);

        processInstance.evaluateRelations();

        Set<Relation> relations = processInstance.getRelations();
        assertTrue(relations.contains(Relation.builder()
                .systemId("ch.admin.test.System")
                .objectType("ch.admin.bit.entity.Foo")
                .objectId("someValue")
                .subjectType("ch.admin.bit.entity.Bar")
                .subjectId("anotherValue")
                .predicateType("ch.admin.bit.test.predicate.Knows")
                .build()));
        assertEquals(1, relations.size());
    }

    @Test
    void evaluateRelationsJoinByRoleDifferentRoleValues() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataAndJoinByRole();
        String templateName = processInstance.getProcessTemplateName();
        MessageData messageData1 = new MessageData(templateName, "sourceEventDataKey", "someValue", "someRole");
        MessageData messageData3 = new MessageData(templateName, "anotherSourceEventDataKey", "anotherValue", "anotherRole");
        Message domainMessage1 = Message.messageBuilder()
                .messageName("sourceEventName")
                .messageId("eventId")
                .idempotenceId("idempotenceId")
                .messageData(Set.of(messageData1))
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .build();
        Message domainMessage2 = Message.messageBuilder()
                .messageName("anotherSourceEventName")
                .idempotenceId("idempotenceId")
                .messageId("eventId")
                .messageData(Set.of(messageData3))
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .build();
        processInstance.addMessage(domainMessage1);
        processInstance.addMessage(domainMessage2);

        processInstance.evaluateRelations();

        assertThat(processInstance.getRelations()).isEmpty();
    }

    @Test
    void evaluateRelationsJoinByValue() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataAndJoinByValue();
        String templateName = processInstance.getProcessTemplateName();
        MessageData messageData1 = new MessageData(templateName, "sourceEventDataKey", "someValue", "someRole");
        MessageData messageData3 = new MessageData(templateName, "anotherSourceEventDataKey", "someValue", "someOtherRole");
        Message domainMessage1 = Message.messageBuilder()
                .messageName("sourceEventName")
                .messageId("eventId")
                .idempotenceId("idempotenceId")
                .messageData(Set.of(messageData1))
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .build();
        Message domainMessage2 = Message.messageBuilder()
                .messageName("anotherSourceEventName")
                .idempotenceId("idempotenceId")
                .messageId("eventId")
                .messageData(Set.of(messageData3))
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .build();
        processInstance.addMessage(domainMessage1);
        processInstance.addMessage(domainMessage2);

        processInstance.evaluateRelations();

        Set<Relation> relations = processInstance.getRelations();
        assertTrue(relations.contains(Relation.builder()
                .systemId("ch.admin.test.System")
                .objectType("ch.admin.bit.entity.Foo")
                .objectId("someValue")
                .subjectType("ch.admin.bit.entity.Bar")
                .subjectId("someValue")
                .predicateType("ch.admin.bit.test.predicate.Knows")
                .build()));
        assertEquals(1, relations.size());
    }

    @Test
    void evaluateRelationsJoinByValueDifferentValues() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataAndJoinByValue();
        String templateName = processInstance.getProcessTemplateName();
        MessageData messageData1 = new MessageData(templateName, "sourceEventDataKey", "someValue", "someRole");
        MessageData messageData3 = new MessageData(templateName, "anotherSourceEventDataKey", "someOtherValue", "someOtherRole");
        Message domainMessage1 = Message.messageBuilder()
                .messageName("sourceEventName")
                .messageId("eventId")
                .idempotenceId("idempotenceId")
                .messageData(Set.of(messageData1))
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .build();
        Message domainMessage2 = Message.messageBuilder()
                .messageName("anotherSourceEventName")
                .idempotenceId("idempotenceId")
                .messageId("eventId")
                .messageData(Set.of(messageData3))
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .build();
        processInstance.addMessage(domainMessage1);
        processInstance.addMessage(domainMessage2);

        processInstance.evaluateRelations();

        assertThat(processInstance.getRelations()).isEmpty();
    }

    @Test
    void evaluateLastEvent() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventData();

        ZonedDateTime now = ZonedDateTime.now();

        Message domainMessage1 = Message.messageBuilder()
                .messageName("sourceEventName")
                .messageId("oldestEvent")
                .idempotenceId("idempotenceIdOldest")
                .createdAt(now.minusDays(2))
                .messageCreatedAt(now.minusDays(2))
                .build();
        Message domainMessage2 = Message.messageBuilder()
                .messageName("anotherSourceEventName")
                .messageId("middleEvent")
                .idempotenceId("idempotenceIdMiddle")
                .messageData(Set.of())
                .createdAt(now.minusDays(1))
                .messageCreatedAt(now.minusDays(1))
                .build();
        Message domainMessage3 = Message.messageBuilder()
                .messageName("anotherSourceEventName")
                .messageId("newewstEvent")
                .idempotenceId("idempotenceIdNewest")
                .messageData(Set.of())
                .createdAt(now)
                .messageCreatedAt(now)
                .build();


        processInstance.addMessage(domainMessage3);
        processInstance.addMessage(domainMessage2);
        processInstance.addMessage(domainMessage1);

        Optional<ZonedDateTime> lastEventDateOptional = processInstance.getLastMessageDateTime();

        assertEquals(now, lastEventDateOptional.orElseThrow());
    }

    @Test
    void applyTemplateMigrationIfChanged_planTaskInstancesForNewTaskTypes_newTaskInstancesAdded() {
        ProcessInstance processInstance = createProcessWithThreeTaskInstances();
        TaskInstance taskInstanceCompleted = createTaskInstance("dont-touch-task-type", processInstance, COMPLETED);
        TaskInstance taskInstanceToDelete = createTaskInstance("deleted-task-type", processInstance, PLANNED);
        TaskInstance taskInstanceToNotDelete = createTaskInstance("deleted-task-type", processInstance, COMPLETED);
        List<TaskInstance> taskInstances = new ArrayList<>();
        taskInstances.add(taskInstanceCompleted);
        taskInstances.add(taskInstanceToDelete);
        taskInstances.add(taskInstanceToNotDelete);
        ReflectionTestUtils.setField(processInstance, "processTemplateHash", "new");
        ReflectionTestUtils.setField(processInstance, "tasks", taskInstances);

        processInstance.applyTemplateMigrationIfChanged();

        assertThat(processInstance.getTasks()).hasSize(4);
        assertThat((processInstance.getTasks().stream().filter(t -> t.getTaskTypeName().equals("dont-touch-task-type") && t.getState() == COMPLETED).count())).isEqualTo(1L);
        assertThat((processInstance.getTasks().stream().filter(t -> t.getTaskTypeName().equals("deleted-task-type") && t.getState() == DELETED).count())).isEqualTo(1L);
        assertThat((processInstance.getTasks().stream().filter(t -> t.getTaskTypeName().equals("deleted-task-type") && t.getState() == COMPLETED).count())).isEqualTo(1L);
        assertThat((processInstance.getTasks().stream().filter(t -> t.getTaskTypeName().equals("new-added-single-task-type") && t.getState() == UNKNOWN).count())).isEqualTo(1L);
    }

    @Test
    void applyTemplateMigrationIfChanged_deleteTaskInstancesForDeletedTaskTypes_oldTaskInstancesDeleted() {
        ProcessInstance processInstance = createProcessWithThreeTaskInstances();
        TaskInstance taskInstanceCompleted = createTaskInstance("dont-touch-task-type", processInstance, COMPLETED);
        TaskInstance taskInstanceToDelete = createTaskInstance("deleted-task-type", processInstance, PLANNED);
        TaskInstance taskInstanceToNotDelete = createTaskInstance("deleted-task-type", processInstance, COMPLETED);
        List<TaskInstance> taskInstances = new ArrayList<>();
        taskInstances.add(taskInstanceCompleted);
        taskInstances.add(taskInstanceToDelete);
        taskInstances.add(taskInstanceToNotDelete);
        ReflectionTestUtils.setField(processInstance, "processTemplateHash", "new");
        ReflectionTestUtils.setField(processInstance, "tasks", taskInstances);

        processInstance.applyTemplateMigrationIfChanged();

        assertThat(processInstance.getTasks()).hasSize(4);
        assertThat((processInstance.getTasks().stream().filter(t -> t.getTaskTypeName().equals("dont-touch-task-type") && t.getState() == COMPLETED).count())).isEqualTo(1L);
        assertThat((processInstance.getTasks().stream().filter(t -> t.getTaskTypeName().equals("deleted-task-type") && t.getState() == DELETED).count())).isEqualTo(1L);
        assertThat((processInstance.getTasks().stream().filter(t -> t.getTaskTypeName().equals("deleted-task-type") && t.getState() == COMPLETED).count())).isEqualTo(1L);
        assertThat((processInstance.getTasks().stream().filter(t -> t.getTaskTypeName().equals("new-added-single-task-type") && t.getState() == UNKNOWN).count())).isEqualTo(1L);
    }

    @Test
    void applyTemplateMigrationIfChanged_milestonesInTemplateChanged_oldMilestonesDeletedNewMilestonesAdded() {
        ProcessInstance processInstance = createProcessWithOneMilestone();
        Milestone milestoneDontTouch = Milestone.createNew("dont-touch-milestone", mock(MilestoneCondition.class), processInstance);
        Milestone milestoneToDelete = Milestone.createNew("to-delete-milestone", mock(MilestoneCondition.class), processInstance);
        List<Milestone> milestones = new ArrayList<>();
        milestones.add(milestoneDontTouch);
        milestones.add(milestoneToDelete);
        ReflectionTestUtils.setField(processInstance, "processTemplateHash", "new");
        ReflectionTestUtils.setField(processInstance, "milestones", milestones);

        processInstance.applyTemplateMigrationIfChanged();

        assertThat(processInstance.getMilestones()).hasSize(3);
        assertThat((processInstance.getMilestones().stream().filter(t -> t.getName().equals(milestoneDontTouch.getName()) && t.getState() == MilestoneState.NOT_REACHED).count())).isEqualTo(1L);
        assertThat((processInstance.getMilestones().stream().filter(t -> t.getName().equals(milestoneToDelete.getName()) && t.getState() == MilestoneState.DELETED).count())).isEqualTo(1L);
        assertThat((processInstance.getMilestones().stream().filter(t -> t.getName().equals("new-added-milestone") && t.getState() == MilestoneState.UNKNOWN).count())).isEqualTo(1L);

    }

    @Test
    void testEvaluateSnapshotConditions() {
        ProcessSnapshotCondition alwaysTrueProcessSnapshotCondition = new AlwaysTrueProcessSnapshotCondition();
        ProcessInstance processInstance = createProcessWithProcessSnapshotCondition(alwaysTrueProcessSnapshotCondition);
        assertThat(processInstance.getSnapshotNames()).isEmpty();

        // register some snapshot name other than the AlwaysTrueProcessSnapshotCondition
        processInstance.registerSnapshot("some-unrelated-snapshot-name");
        assertThat(processInstance.getSnapshotNames()).containsExactly("some-unrelated-snapshot-name");

        // AlwaysTrueProcessSnapshotCondition should trigger as its name has not yet been registerd as already triggered
        Set<String> snapshotNamesFirstEvaluation = processInstance.evaluateSnapshotConditions();
        assertThat(snapshotNamesFirstEvaluation).containsExactly(AlwaysTrueProcessSnapshotCondition.SNAPSHOT_CONDITION_NAME);

        // AlwaysTrueProcessSnapshotCondition should trigger again as the snapshot triggere still has not been registered
        Set<String> snapshotNamesSecondEvaluation = processInstance.evaluateSnapshotConditions();
        assertThat(snapshotNamesSecondEvaluation).containsExactly(AlwaysTrueProcessSnapshotCondition.SNAPSHOT_CONDITION_NAME);

        // Register the triggered snapshot
        processInstance.registerSnapshot(snapshotNamesSecondEvaluation.iterator().next());
        assertThat(processInstance.getSnapshotNames()).containsExactly(
                "some-unrelated-snapshot-name", AlwaysTrueProcessSnapshotCondition.SNAPSHOT_CONDITION_NAME);

        // AlwaysTrueProcessSnapshotCondition should no longer trigger as the snapshot has now been registered as already triggered
        Set<String> snapshotNamesThirdEvaluation = processInstance.evaluateSnapshotConditions();
        assertThat(snapshotNamesThirdEvaluation).isEmpty();
    }

    static class AlwaysTrueProcessSnapshotCondition implements ProcessSnapshotCondition {
        static final String SNAPSHOT_CONDITION_NAME = "AlwaysTrueCondition";
        @Override
        public ProcessSnapshotConditionResult triggerSnapshot(ProcessContext processContext) {
            return ProcessSnapshotConditionResult.triggeredFor("AlwaysTrueCondition");
        }
    }

    static ProcessInstance createProcessWithThreeTaskInstances() {
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name("template")
                .templateHash("hash")
                .taskTypes(List.of(
                        TaskType.builder().name("dont-touch-task-type").cardinality(TaskCardinality.SINGLE_INSTANCE).lifecycle(TaskLifecycle.STATIC).build(),
                        TaskType.builder().name("new-added-single-task-type").cardinality(TaskCardinality.SINGLE_INSTANCE).lifecycle(TaskLifecycle.STATIC).build(),
                        TaskType.builder().name("new-added-dynamic-task-type").cardinality(TaskCardinality.MULTI_INSTANCE).lifecycle(TaskLifecycle.DYNAMIC).build()))
                .build();
        Set<ProcessData> processData = Set.of(
                new ProcessData("key1", "value1"),
                new ProcessData("key1", "value2"));
        return ProcessInstance.startProcess(Generators.timeBasedEpochGenerator().generate().toString(), processTemplate, processData);
    }

    static ProcessInstance createProcessWithOneMilestone() {
        Map<String, MilestoneCondition> milestones = new HashMap<>();
        milestones.put("dont-touch-milestone", Mockito.mock(MilestoneCondition.class));
        milestones.put("new-added-milestone", Mockito.mock(MilestoneCondition.class));
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name("template")
                .templateHash("hash")
                .taskTypes(List.of(TaskType.builder().name("dont-touch-task-type").cardinality(TaskCardinality.SINGLE_INSTANCE)
                        .lifecycle(TaskLifecycle.STATIC).build()))
                .milestones(milestones)
                .build();
        Set<ProcessData> processData = Set.of(
                new ProcessData("key1", "value1"),
                new ProcessData("key1", "value2"));
        return ProcessInstance.startProcess(Generators.timeBasedEpochGenerator().generate().toString(), processTemplate, processData);
    }

    private TaskInstance createTaskInstance(String name, ProcessInstance processInstance, TaskState taskState) {
        final TaskInstance taskInstance = TaskInstance.createInitialTaskInstance(TaskType.builder().name(name)
                .cardinality(TaskCardinality.SINGLE_INSTANCE).lifecycle(TaskLifecycle.STATIC).build(), processInstance, ZonedDateTime.now());
        ReflectionTestUtils.setField(taskInstance, "state", taskState);
        return taskInstance;

    }

}
