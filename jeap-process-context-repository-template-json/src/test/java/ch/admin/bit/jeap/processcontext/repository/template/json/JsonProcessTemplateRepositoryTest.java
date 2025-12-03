package ch.admin.bit.jeap.processcontext.repository.template.json;

import ch.admin.bit.jeap.processcontext.domain.TranslateService;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.*;
import ch.admin.bit.jeap.processcontext.plugin.api.condition.*;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessCompletionConclusion;
import ch.admin.bit.jeap.processcontext.plugin.api.event.MessageProcessIdCorrelationProvider;
import ch.admin.bit.jeap.processcontext.repository.template.json.stubs.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JsonProcessTemplateRepositoryTest {

    private static final String SERIALIZER_PROCESS_TEMPLATE_NAME = "serializerTestProcess";
    private static final String SERIALIZER_MESSAGE_PROCESS_TEMPLATE_NAME = "serializerMessageTestProcess";
    private static final String ADDITIONAL_PROCESS_TEMPLATE_NAME = "additionalTestProcess";
    private static final String PROCESS_RELATIONS_TEMPLATE_NAME = "processRelationsTestProcess";
    private static final String MESSAGE_CLUSTER_TEMPLATE_NAME = "messageClusterTestProcess";

    private JsonProcessTemplateRepository jsonProcessTemplateRepository;
    private final JsonProcessTemplateConsumerContractValidator mockValidator = mock(JsonProcessTemplateConsumerContractValidator.class);

    private static TaskType assertAndGetTaskType(ProcessTemplate template, String taskName) {
        return template.getTaskTypeByName(taskName).orElseThrow(() -> new AssertionFailedError("Missing expected task " + taskName));
    }

    @BeforeEach
    void setUp() throws IOException {
        jsonProcessTemplateRepository = new JsonProcessTemplateRepository(mock(TranslateService.class), mockValidator);
        jsonProcessTemplateRepository.loadTemplates();
    }

    @Test
    void testGetDomainEventReferencesByTemplateNameForEventName() {
        assertThat(jsonProcessTemplateRepository.getMessageReferencesByTemplateNameForMessageName("TestEvent"))
                .containsOnlyKeys(SERIALIZER_PROCESS_TEMPLATE_NAME, SERIALIZER_MESSAGE_PROCESS_TEMPLATE_NAME, ADDITIONAL_PROCESS_TEMPLATE_NAME, PROCESS_RELATIONS_TEMPLATE_NAME);
        assertThat(jsonProcessTemplateRepository.getMessageReferencesByTemplateNameForMessageName("TestEvent2"))
                .containsOnlyKeys(SERIALIZER_PROCESS_TEMPLATE_NAME, SERIALIZER_MESSAGE_PROCESS_TEMPLATE_NAME);
        assertThat(jsonProcessTemplateRepository.getMessageReferencesByTemplateNameForMessageName("TestEvent3"))
                .containsOnlyKeys(SERIALIZER_PROCESS_TEMPLATE_NAME, SERIALIZER_MESSAGE_PROCESS_TEMPLATE_NAME);
        assertThat(jsonProcessTemplateRepository.getMessageReferencesByTemplateNameForMessageName("TestEvent4"))
                .containsOnlyKeys(SERIALIZER_PROCESS_TEMPLATE_NAME, SERIALIZER_MESSAGE_PROCESS_TEMPLATE_NAME);
        assertThat(jsonProcessTemplateRepository.getMessageReferencesByTemplateNameForMessageName("AdditionalTestEvent"))
                .containsOnlyKeys(ADDITIONAL_PROCESS_TEMPLATE_NAME);
    }

    @Test
    void isAnyTemplateHasEventsCorrelatedByProcessData() {
        assertTrue(jsonProcessTemplateRepository.isAnyTemplateHasEventsCorrelatedByProcessData());
        assertTrue(jsonProcessTemplateRepository.findByName(SERIALIZER_MESSAGE_PROCESS_TEMPLATE_NAME).orElseThrow().isAnyEventCorrelatedByProcessData());
        assertFalse(jsonProcessTemplateRepository.findByName(ADDITIONAL_PROCESS_TEMPLATE_NAME).orElseThrow().isAnyEventCorrelatedByProcessData());
    }

    @Test
    void testConsumerContractValidationByTemplate() {
        verify(mockValidator, times(5)).validateContract(any(ProcessTemplate.class));
    }

    @Test
    void templateDeserialization_legacyEventAttributeNaming() {
        ProcessTemplate template = jsonProcessTemplateRepository.findByName(SERIALIZER_PROCESS_TEMPLATE_NAME).orElseThrow();
        assertTemplate(template);
    }

    @Test
    void templateDeserialization() {
        ProcessTemplate template = jsonProcessTemplateRepository.findByName(SERIALIZER_MESSAGE_PROCESS_TEMPLATE_NAME).orElseThrow();
        assertTemplate(template);
    }

    private static void assertTemplate(ProcessTemplate template) {
        assertEquals(12, template.getTaskTypes().size());
        TaskType taskTypeSingleLegacy = assertAndGetTaskType(template, "single-legacy");
        TaskType taskTypeDynamicLegacy = assertAndGetTaskType(template, "dynamic-legacy");
        TaskType taskTypeDefaults = assertAndGetTaskType(template, "defaults");
        TaskType taskTypeSingle = assertAndGetTaskType(template, "single");
        TaskType taskTypeSingleExplicit = assertAndGetTaskType(template, "single-explicit");
        TaskType taskTypeDynamic = assertAndGetTaskType(template, "dynamic");
        TaskType taskTypeDynamicMulti = assertAndGetTaskType(template, "dynamic-multi");
        TaskType taskTypePlannedByDomainEventOrMessage = assertAndGetTaskType(template, "plannedByEventOrMessage");
        TaskType taskTypePlannedByDomainEventOrMessageWithCondition = assertAndGetTaskType(template, "plannedByEventOrMessageWithCondition");
        TaskType taskTypeObserved = assertAndGetTaskType(template, "observed");
        TaskType taskTypeObservedWithCondition = assertAndGetTaskType(template, "observedWithCondition");

        TaskType completedByDomainEvent = assertAndGetTaskType(template, "completedByDomainEvent");

        assertSame(TaskCardinality.SINGLE_INSTANCE, taskTypeSingleLegacy.getCardinality());
        assertSame(TaskLifecycle.STATIC, taskTypeSingleLegacy.getLifecycle());
        assertSame(TaskLifecycle.DYNAMIC, taskTypeDynamicLegacy.getLifecycle());
        assertSame(TaskCardinality.MULTI_INSTANCE, taskTypeDynamicLegacy.getCardinality());

        assertSame(TaskCardinality.SINGLE_INSTANCE, taskTypeDefaults.getCardinality());
        assertSame(TaskLifecycle.STATIC, taskTypeDefaults.getLifecycle());
        assertSame(TaskCardinality.SINGLE_INSTANCE, taskTypeSingle.getCardinality());
        assertSame(TaskLifecycle.STATIC, taskTypeSingle.getLifecycle());
        assertSame(TaskCardinality.SINGLE_INSTANCE, taskTypeSingleExplicit.getCardinality());
        assertSame(TaskLifecycle.STATIC, taskTypeSingleExplicit.getLifecycle());
        assertSame(TaskLifecycle.DYNAMIC, taskTypeDynamic.getLifecycle());
        assertSame(TaskCardinality.MULTI_INSTANCE, taskTypeDynamic.getCardinality());
        assertSame(TaskLifecycle.DYNAMIC, taskTypeDynamicMulti.getLifecycle());
        assertSame(TaskCardinality.MULTI_INSTANCE, taskTypeDynamicMulti.getCardinality());
        assertSame(TaskLifecycle.DYNAMIC, taskTypePlannedByDomainEventOrMessage.getLifecycle());
        assertSame(TaskCardinality.MULTI_INSTANCE, taskTypePlannedByDomainEventOrMessage.getCardinality());
        assertSame(TaskLifecycle.DYNAMIC, taskTypePlannedByDomainEventOrMessageWithCondition.getLifecycle());
        assertSame(TaskCardinality.MULTI_INSTANCE, taskTypePlannedByDomainEventOrMessageWithCondition.getCardinality());
        assertSame(TaskLifecycle.OBSERVED, taskTypeObserved.getLifecycle());
        assertSame(TaskCardinality.MULTI_INSTANCE, taskTypeObserved.getCardinality());
        assertSame(TaskLifecycle.OBSERVED, taskTypeObservedWithCondition.getLifecycle());
        assertSame(TaskCardinality.MULTI_INSTANCE, taskTypeObservedWithCondition.getCardinality());

        assertEquals("TestEvent", completedByDomainEvent.getCompletedByDomainEvent());
        assertTrue(taskTypePlannedByDomainEventOrMessageWithCondition.getInstantiationCondition() instanceof TestTaskInstantiationCondition);
        assertTrue(taskTypeObservedWithCondition.getInstantiationCondition() instanceof TestTaskInstantiationCondition);

        assertEquals(1, taskTypePlannedByDomainEventOrMessage.getTaskData().size());
        TaskData taskDataPlanned = taskTypePlannedByDomainEventOrMessage.getTaskData().iterator().next();
        assertEquals("TestEvent", taskDataPlanned.getSourceMessage());
        assertEquals(Set.of("foo", "bar"), taskDataPlanned.getMessageDataKeys());

        assertEquals(1, completedByDomainEvent.getTaskData().size());
        TaskData taskDataCompleted = completedByDomainEvent.getTaskData().iterator().next();
        assertEquals("TestEvent", taskDataCompleted.getSourceMessage());
        assertEquals(Set.of("foo"), taskDataCompleted.getMessageDataKeys());

        Optional<MilestoneCondition> customMilestoneCondition = template.getMilestoneConditionByMilestoneName("BorderCrossed");
        assertTrue(customMilestoneCondition.isPresent());
        assertTrue(customMilestoneCondition.get() instanceof TestMilestoneCondition);

        Optional<MilestoneCondition> multiTaskCompletedMilestoneCondition = template.getMilestoneConditionByMilestoneName("MultiTasks");
        assertTrue(multiTaskCompletedMilestoneCondition.isPresent());
        assertTrue(multiTaskCompletedMilestoneCondition.get() instanceof TasksCompletedMilestoneCondition);
        TasksCompletedMilestoneCondition multiTasksCondition = (TasksCompletedMilestoneCondition) multiTaskCompletedMilestoneCondition.get();
        assertEquals(Set.of("completedByDomainEvent"), multiTasksCondition.getTaskNames());

        assertEquals(2, template.getProcessCompletionConditions().size());
        assertTrue(template.getProcessCompletionConditions().get(0) instanceof AllTasksInFinalStateProcessCompletionCondition);
        assertTrue(template.getProcessCompletionConditions().get(1) instanceof MessageProcessCompletionCondition);
        MessageProcessCompletionCondition messageProcessCompletionCondition = (MessageProcessCompletionCondition) template.getProcessCompletionConditions().get(1);
        assertEquals("TestEvent4", messageProcessCompletionCondition.getMessageName());
        assertEquals(ProcessCompletionConclusion.CANCELLED, messageProcessCompletionCondition.getConclusion());
        assertThat(messageProcessCompletionCondition.getName()).isEqualTo("Cancelled because of TestEvent4");

        assertEquals(2, template.getProcessSnapshotConditions().size());
        assertTrue(template.getProcessSnapshotConditions().get(0) instanceof TestProcessSnapshotCondition);
        assertTrue(template.getProcessSnapshotConditions().get(1) instanceof ProcessCompletionProcessSnapshotCondition);
        ProcessCompletionProcessSnapshotCondition completionSnapshotCondition = (ProcessCompletionProcessSnapshotCondition) template.getProcessSnapshotConditions().get(1);
        assertNull(completionSnapshotCondition.getTriggeringConclusion());

        List<MessageReference> messageReferences = template.getMessageReferences();
        assertEquals(4, messageReferences.size());
        // TestEvent 1
        MessageReference event1 = messageReferences.getFirst();
        assertEquals("TestEvent", event1.getMessageName());
        assertEquals("test.topic", event1.getTopicName());
        assertTrue(event1.getCorrelationProvider() instanceof TestCorrelationProvider);
        // TestEvent 2
        MessageReference event2 = messageReferences.get(1);
        assertTrue(event2.getCorrelationProvider() instanceof MessageProcessIdCorrelationProvider);
        // Test Event 3
        MessageReference event3 = messageReferences.get(2);
        assertTrue(event3.getPayloadExtractor() instanceof TestPayloadExtractor);
        // Test Event 4
        MessageReference event4 = messageReferences.get(3);
        assertTrue(event4.getReferenceExtractor() instanceof TestReferenceExtractor);
    }

    @Test
    void findByName_whenFindingTemplateThatDoesNotExist_thenShouldReturnEmptyOptional() {
        Optional<ProcessTemplate> template = jsonProcessTemplateRepository.findByName("doesnotexist");
        assertTrue(template.isEmpty());
    }

    @Test
    void getProcessData() {
        Optional<ProcessTemplate> templateOptional = jsonProcessTemplateRepository.findByName(SERIALIZER_PROCESS_TEMPLATE_NAME);
        assertTrue(templateOptional.isPresent());
        ProcessTemplate template = templateOptional.get();

        List<ProcessDataTemplate> processDataTemplateList = template.getProcessDataTemplates();
        assertEquals(1, processDataTemplateList.size());
        ProcessDataTemplate processData = processDataTemplateList.getFirst();
        assertEquals("processDataKey", processData.getKey());
        assertEquals("TestEvent4", processData.getSourceMessageName());
        assertEquals("sourceDataKey", processData.getSourceMessageDataKey());
    }

    @Test
    void getProcessReferences() {
        Optional<ProcessTemplate> templateOptional = jsonProcessTemplateRepository.findByName(PROCESS_RELATIONS_TEMPLATE_NAME);
        assertTrue(templateOptional.isPresent());
        ProcessTemplate template = templateOptional.get();

        List<ProcessRelationPattern> processRelationPatternList = template.getProcessRelationPatterns();
        assertEquals(2, processRelationPatternList.size());

        ProcessRelationPattern processRelationPattern = processRelationPatternList.getFirst();
        assertEquals("aName", processRelationPattern.getName());
    }

    @Test
    void getMessageCluster() {
        Optional<ProcessTemplate> templateOptional = jsonProcessTemplateRepository.findByName(MESSAGE_CLUSTER_TEMPLATE_NAME);
        assertTrue(templateOptional.isPresent());
        ProcessTemplate template = templateOptional.get();

        List<MessageReference> messageReferences = template.getMessageReferences();
        assertThat(messageReferences).hasSize(2);

        assertThat(messageReferences.get(0).getMessageName()).isEqualTo("TestEventWithCluster");
        assertThat(messageReferences.get(0).getClusterName()).isEqualTo("aws");

        assertThat(messageReferences.get(1).getMessageName()).isEqualTo("TestEventWithoutCluster");
        assertThat(messageReferences.get(1).getClusterName()).isNull();
    }
}
