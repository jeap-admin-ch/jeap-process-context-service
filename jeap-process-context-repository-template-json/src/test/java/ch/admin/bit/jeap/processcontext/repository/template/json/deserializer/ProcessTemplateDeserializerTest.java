package ch.admin.bit.jeap.processcontext.repository.template.json.deserializer;

import ch.admin.bit.jeap.processcontext.domain.processtemplate.*;
import ch.admin.bit.jeap.processcontext.plugin.api.condition.AllTasksInFinalStateProcessCompletionCondition;
import ch.admin.bit.jeap.processcontext.plugin.api.condition.MessageProcessCompletionCondition;
import ch.admin.bit.jeap.processcontext.plugin.api.condition.ProcessCompletionProcessSnapshotCondition;
import ch.admin.bit.jeap.processcontext.plugin.api.condition.TaskInstantiationCondition;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessCompletionConclusion;
import ch.admin.bit.jeap.processcontext.plugin.api.event.AlwaysProcessInstantiationCondition;
import ch.admin.bit.jeap.processcontext.plugin.api.event.MessageProcessIdCorrelationProvider;
import ch.admin.bit.jeap.processcontext.plugin.api.event.NeverProcessInstantiationCondition;
import ch.admin.bit.jeap.processcontext.repository.template.json.TestProcessInstantiationCondition;
import ch.admin.bit.jeap.processcontext.repository.template.json.model.*;
import ch.admin.bit.jeap.processcontext.repository.template.json.stubs.TestCorrelationProvider;
import ch.admin.bit.jeap.processcontext.repository.template.json.stubs.TestPayloadExtractor;
import ch.admin.bit.jeap.processcontext.repository.template.json.stubs.TestProcessSnapshotCondition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskCardinality.MULTI_INSTANCE;
import static ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskCardinality.SINGLE_INSTANCE;
import static ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskLifecycle.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class ProcessTemplateDeserializerTest {

    @Test
    void toTemplate() {
        MessageReferenceDefinition messageReferenceDefinitionOne = new MessageReferenceDefinition();
        messageReferenceDefinitionOne.setMessageName("EventOne");
        messageReferenceDefinitionOne.setTopicName("TopicNameOne");

        MessageReferenceDefinition messageReference2 = new MessageReferenceDefinition();
        messageReference2.setMessageName("messageName");
        messageReference2.setTopicName("topicName");

        TaskTypeDefinition taskTypeDefinition = new TaskTypeDefinition("taskName");
        TaskCompletionConditionDefinition completedBy = new TaskCompletionConditionDefinition();
        completedBy.setMessage("messageName");
        taskTypeDefinition.setCompletedBy(completedBy);
        TaskDataDefinition taskDataDefinition = new TaskDataDefinition();
        taskDataDefinition.setSourceMessage("messageName");
        taskDataDefinition.setMessageDataKeys(Set.of("foo", "bar"));
        taskTypeDefinition.setTaskData(Set.of(taskDataDefinition));

        ProcessTemplateDefinition processTemplateDefinition = new ProcessTemplateDefinition();
        processTemplateDefinition.setMessages(List.of(messageReferenceDefinitionOne, messageReference2));
        processTemplateDefinition.setTasks(List.of(taskTypeDefinition));
        processTemplateDefinition.setName("name");

        // ProcessData
        ProcessDataSourceDefinition processDataSourceDefinition = new ProcessDataSourceDefinition();
        processDataSourceDefinition.setMessageDataKey("eventDataKey");
        processDataSourceDefinition.setMessage("EventOne");
        ProcessDataDefinition processDataDefinition = new ProcessDataDefinition();
        processDataDefinition.setKey("processDataKey");
        processDataDefinition.setSource(processDataSourceDefinition);
        processTemplateDefinition.setProcessData(List.of(processDataDefinition));

        // relationPatterns
        RelationPatternDefinition relationPatternDefinition = new RelationPatternDefinition();
        relationPatternDefinition.setPredicateType("some relation");
        relationPatternDefinition.setJoinType(RelationPatternDefinition.JOIN_BY_ROLE);
        relationPatternDefinition.setFeatureFlag("myFeatureFlag");

        RelationNodeDefinition relationNodeDefinition = new RelationNodeDefinition();
        relationNodeDefinition.setType("some_type");
        relationNodeDefinition.setSelector(new RelationSelectorDefinition(processDataDefinition.getKey(), "some-role"));

        relationPatternDefinition.setSubject(relationNodeDefinition);
        relationPatternDefinition.setObject(relationNodeDefinition);
        processTemplateDefinition.setRelationPatterns(List.of(relationPatternDefinition));
        processTemplateDefinition.setRelationSystemId("ch.test.System");

        // completions
        ProcessCompletionDefinition domainEventProcessCompletionDefinition =
                createProcessCompletionDefinition(null, "EventOne", ProcessCompletionConclusion.SUCCEEDED, "def1");
        ProcessCompletionDefinition conditionProcessCompletionDefinition =
                createProcessCompletionDefinition("ch.admin.bit.jeap.processcontext.plugin.api.condition.AllTasksInFinalStateProcessCompletionCondition", null,  null, "def2");
        processTemplateDefinition.setCompletions(List.of(domainEventProcessCompletionDefinition, conditionProcessCompletionDefinition));

        // process relations
        ProcessRelationSourceDefinition processRelationSource = new ProcessRelationSourceDefinition();
        processRelationSource.setMessageName("EventOne");
        processRelationSource.setMessageDataKey("aMessageDataKey");

        ProcessRelationPatternDefinition processRelationPatternDefinition = new ProcessRelationPatternDefinition();
        processRelationPatternDefinition.setName("processRelationName");
        processRelationPatternDefinition.setRoleType(ProcessRelationRoleType.ORIGIN);
        processRelationPatternDefinition.setOriginRole("theOriginRoleText");
        processRelationPatternDefinition.setTargetRole("theTargetRoleText");
        processRelationPatternDefinition.setVisibility(ProcessRelationRoleVisibility.BOTH);
        processRelationPatternDefinition.setSource(processRelationSource);

        processTemplateDefinition.setProcessRelationPatterns(List.of(processRelationPatternDefinition));

        // process snapshots
        ProcessSnapshotDefinition anyCompletionSnapshot = createProcessSnapshotDefinition(null, "any");
        ProcessSnapshotDefinition succeededCompletionSnapshot = createProcessSnapshotDefinition(null, "succeeded");
        ProcessSnapshotDefinition testConditionSnapshot = createProcessSnapshotDefinition(TestProcessSnapshotCondition.class.getCanonicalName(), null);
        processTemplateDefinition.setSnapshots(List.of(anyCompletionSnapshot, succeededCompletionSnapshot, testConditionSnapshot));


        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(processTemplateDefinition, "hash");
        ProcessTemplate template = deserializer.toProcessTemplate();

        assertEquals("name", template.getName());
        assertEquals(1, template.getTaskTypes().size());
        TaskType taskType = template.getTaskTypes().getFirst();
        assertEquals("taskName", taskType.getName());
        assertEquals("messageName", taskType.getCompletedByDomainEvent());
        assertNotNull(taskType.getTaskData());
        assertEquals(1, taskType.getTaskData().size());
        TaskData taskData = taskType.getTaskData().iterator().next();
        assertEquals("messageName", taskData.getSourceMessage());
        assertEquals(Set.of("foo","bar"), taskData.getMessageDataKeys());

        assertEquals(1, template.getRelationPatterns().size());
        RelationPattern relationPattern = template.getRelationPatterns().getFirst();
        assertEquals(RelationPatternDefinition.JOIN_BY_ROLE, relationPattern.getJoinType());
        assertEquals("some relation", relationPattern.getPredicateType());
        assertEquals("myFeatureFlag", relationPattern.getFeatureFlag());
        RelationNodeSelector objectSelector = relationPattern.getObjectSelector();
        assertEquals("some_type", objectSelector.getType());
        assertEquals("processDataKey", objectSelector.getProcessDataKey());
        assertEquals("some-role", objectSelector.getProcessDataRole());
        RelationNodeSelector subjectSelector = relationPattern.getSubjectSelector();
        assertEquals("some_type", subjectSelector.getType());
        assertEquals("processDataKey", subjectSelector.getProcessDataKey());
        assertEquals("some-role", subjectSelector.getProcessDataRole());
        assertEquals(1, template.getProcessRelationPatterns().size());
        assertEquals(2, template.getProcessCompletionConditions().size());
        assertTrue(template.getProcessCompletionConditions().getFirst() instanceof MessageProcessCompletionCondition);
        MessageProcessCompletionCondition messageProcessCompletionCondition = (MessageProcessCompletionCondition) template.getProcessCompletionConditions().get(0);
        assertEquals("EventOne", messageProcessCompletionCondition.getMessageName());
        assertEquals(ProcessCompletionConclusion.SUCCEEDED, messageProcessCompletionCondition.getConclusion());
        assertTrue(template.getProcessCompletionConditions().get(1) instanceof AllTasksInFinalStateProcessCompletionCondition);

        assertEquals(1, template.getProcessRelationPatterns().size());
        ProcessRelationPattern processRelationPattern = template.getProcessRelationPatterns().getFirst();
        assertNotNull(processRelationPattern);
        assertEquals("processRelationName", processRelationPattern.getName());
        assertEquals(ProcessRelationRoleType.ORIGIN, processRelationPattern.getRoleType());
        assertEquals("theOriginRoleText", processRelationPattern.getOriginRole());
        assertEquals("theTargetRoleText", processRelationPattern.getTargetRole());
        assertEquals(ProcessRelationRoleVisibility.BOTH, processRelationPattern.getVisibility());
        assertEquals("EventOne", processRelationPattern.getSource().getMessageName());
        assertEquals("aMessageDataKey", processRelationPattern.getSource().getMessageDataKey());

        assertEquals(3, template.getProcessSnapshotConditions().size());
        assertTrue(template.getProcessSnapshotConditions().getFirst() instanceof ProcessCompletionProcessSnapshotCondition);
        ProcessCompletionProcessSnapshotCondition anySnapshotCompletionCondition = (ProcessCompletionProcessSnapshotCondition) template.getProcessSnapshotConditions().get(0);
        assertNull(anySnapshotCompletionCondition.getTriggeringConclusion());
        assertTrue(template.getProcessSnapshotConditions().get(1) instanceof ProcessCompletionProcessSnapshotCondition);
        ProcessCompletionProcessSnapshotCondition succeededSnapshotCompletionCondition = (ProcessCompletionProcessSnapshotCondition) template.getProcessSnapshotConditions().get(1);
        assertEquals(ProcessCompletionConclusion.SUCCEEDED, succeededSnapshotCompletionCondition.getTriggeringConclusion());
        assertTrue(template.getProcessSnapshotConditions().get(2) instanceof TestProcessSnapshotCondition);
    }

    @Test
    void toTemplate_missingName() {
        ProcessTemplateDefinition processTemplateDefinition = new ProcessTemplateDefinition();
        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(processTemplateDefinition, "hash");
        TemplateDefinitionException ex = assertThrows(TemplateDefinitionException.class,
                deserializer::toProcessTemplate);
        assertTrue(ex.getMessage().contains("name"));
    }

    @Test
    void toTemplate_duplicateProcessReferenceNames() {
        ProcessTemplateDefinition processTemplateDefinition = new ProcessTemplateDefinition();
        processTemplateDefinition.setName("name");

        MessageReferenceDefinition messageReferenceDefinitionOne = new MessageReferenceDefinition();
        messageReferenceDefinitionOne.setMessageName("aMessage");
        messageReferenceDefinitionOne.setTopicName("TopicNameOne");
        processTemplateDefinition.setMessages(List.of(messageReferenceDefinitionOne));

        ProcessRelationPatternDefinition processRelationPatternDefinition1 = new ProcessRelationPatternDefinition();
        processRelationPatternDefinition1.setName("processRelation");
        processRelationPatternDefinition1.setRoleType(ProcessRelationRoleType.ORIGIN);
        processRelationPatternDefinition1.setOriginRole("bla bla");
        processRelationPatternDefinition1.setTargetRole("dideldum");
        processRelationPatternDefinition1.setVisibility(ProcessRelationRoleVisibility.BOTH);

        ProcessRelationSourceDefinition sourceDefinition = new ProcessRelationSourceDefinition();
        sourceDefinition.setMessageName("aMessage");
        sourceDefinition.setMessageDataKey("dataKey");
        processRelationPatternDefinition1.setSource(sourceDefinition);

        ProcessRelationPatternDefinition processRelationPatternDefinition2 = new ProcessRelationPatternDefinition();
        processRelationPatternDefinition2.setName("processRelation");
        processRelationPatternDefinition2.setRoleType(ProcessRelationRoleType.TARGET);
        processRelationPatternDefinition2.setOriginRole("some text");
        processRelationPatternDefinition2.setTargetRole("other text");
        processRelationPatternDefinition2.setVisibility(ProcessRelationRoleVisibility.BOTH);
        processRelationPatternDefinition2.setSource(sourceDefinition);

        processTemplateDefinition.setProcessRelationPatterns(List.of(processRelationPatternDefinition1, processRelationPatternDefinition2));
        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(processTemplateDefinition, "hash");

        TemplateDefinitionException ex = assertThrows(TemplateDefinitionException.class,
                deserializer::toProcessTemplate);

        assertTrue(ex.getMessage().contains("Duplicate processRelation"), ex::getMessage);
    }

    @Test
    void toTemplate_ProcessReferenceButMissingMessageDefinition() {
        ProcessTemplateDefinition processTemplateDefinition = new ProcessTemplateDefinition();
        processTemplateDefinition.setName("name");

        ProcessRelationPatternDefinition processRelationPatternDefinition = new ProcessRelationPatternDefinition();
        processRelationPatternDefinition.setName("processRelation");
        processRelationPatternDefinition.setRoleType(ProcessRelationRoleType.ORIGIN);
        processRelationPatternDefinition.setOriginRole("bla bla");
        processRelationPatternDefinition.setTargetRole("dideldum");
        processRelationPatternDefinition.setVisibility(ProcessRelationRoleVisibility.BOTH);

        ProcessRelationSourceDefinition sourceDefinition = new ProcessRelationSourceDefinition();
        sourceDefinition.setMessageName("aMessage");
        sourceDefinition.setMessageDataKey("dataKey");
        processRelationPatternDefinition.setSource(sourceDefinition);


        processTemplateDefinition.setProcessRelationPatterns(List.of(processRelationPatternDefinition));
        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(processTemplateDefinition, "hash");

        TemplateDefinitionException ex = assertThrows(TemplateDefinitionException.class,
                deserializer::toProcessTemplate);

        assertTrue(ex.getMessage().contains("Message 'aMessage' referenced in process relation definition does not exists in message references"), ex::getMessage);
    }

    @Test
    void toTemplate_optionalRelationPatternJoinType() {
        MessageReferenceDefinition messageReferenceDefinitionOne = new MessageReferenceDefinition();
        messageReferenceDefinitionOne.setMessageName("EventOne");
        messageReferenceDefinitionOne.setTopicName("TopicNameOne");

        MessageReferenceDefinition messageReference2 = new MessageReferenceDefinition();
        messageReference2.setMessageName("messageName");
        messageReference2.setTopicName("topicName");

        TaskTypeDefinition taskTypeDefinition = new TaskTypeDefinition("taskName");
        TaskCompletionConditionDefinition completedBy = new TaskCompletionConditionDefinition();
        completedBy.setMessage("messageName");
        taskTypeDefinition.setCompletedBy(completedBy);

        ProcessTemplateDefinition processTemplateDefinition = new ProcessTemplateDefinition();
        processTemplateDefinition.setMessages(List.of(messageReferenceDefinitionOne, messageReference2));
        processTemplateDefinition.setTasks(List.of(taskTypeDefinition));
        processTemplateDefinition.setName("name");

        ProcessDataSourceDefinition processDataSourceDefinition = new ProcessDataSourceDefinition();
        processDataSourceDefinition.setMessageDataKey("eventDataKey");
        processDataSourceDefinition.setMessage("EventOne");
        ProcessDataDefinition processDataDefinition = new ProcessDataDefinition();
        processDataDefinition.setKey("processDataKey");
        processDataDefinition.setSource(processDataSourceDefinition);
        processTemplateDefinition.setProcessData(List.of(processDataDefinition));

        // relationPatterns
        RelationPatternDefinition relationPatternDefinition = new RelationPatternDefinition();
        relationPatternDefinition.setPredicateType("some relation");

        RelationNodeDefinition relationNodeDefinition = new RelationNodeDefinition();
        relationNodeDefinition.setType("some_type");
        relationNodeDefinition.setSelector(new RelationSelectorDefinition(processDataDefinition.getKey(), "some-role"));

        relationPatternDefinition.setSubject(relationNodeDefinition);
        relationPatternDefinition.setObject(relationNodeDefinition);
        processTemplateDefinition.setRelationPatterns(List.of(relationPatternDefinition));
        processTemplateDefinition.setRelationSystemId("ch.test.System");

        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(processTemplateDefinition, "hash");
        ProcessTemplate template = deserializer.toProcessTemplate();

        assertNull(template.getRelationPatterns().getFirst().getJoinType());
    }

    @Test
    void toTemplate_unsupportedRelationPatternJoinType() {
        ProcessTemplateDefinition processTemplateDefinition = new ProcessTemplateDefinition();
        processTemplateDefinition.setName("name");

        // relationPatterns
        RelationPatternDefinition relationPatternDefinition = new RelationPatternDefinition();
        relationPatternDefinition.setPredicateType("some relation");
        relationPatternDefinition.setJoinType("UNSUPPORTED!");

        RelationNodeDefinition relationNodeDefinition = new RelationNodeDefinition();
        relationNodeDefinition.setType("some_type");
        relationNodeDefinition.setSelector(new RelationSelectorDefinition("some-key", "some-role"));

        relationPatternDefinition.setSubject(relationNodeDefinition);
        relationPatternDefinition.setObject(relationNodeDefinition);
        processTemplateDefinition.setRelationPatterns(List.of(relationPatternDefinition));
        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(processTemplateDefinition, "hash");

        TemplateDefinitionException ex = assertThrows(TemplateDefinitionException.class,
                deserializer::toProcessTemplate);

        assertTrue(ex.getMessage().contains("Unsupported relation join type 'UNSUPPORTED!'"), ex::getMessage);
    }

    @Test
    void toTemplate_duplicateTaskNames() {
        TaskTypeDefinition taskTypeDefinition = new TaskTypeDefinition("taskName");
        ProcessTemplateDefinition processTemplateDefinition = new ProcessTemplateDefinition();
        processTemplateDefinition.setTasks(List.of(taskTypeDefinition, taskTypeDefinition));
        processTemplateDefinition.setName("name");
        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(processTemplateDefinition, "hash");

        TemplateDefinitionException ex = assertThrows(TemplateDefinitionException.class,
                deserializer::toProcessTemplate);

        assertTrue(ex.getMessage().contains("Duplicate task"), ex::getMessage);
    }

    @Test
    void toTemplate_defaultLabel() {
        TaskTypeDefinition taskTypeDefinition = new TaskTypeDefinition("task");
        ProcessTemplateDefinition processTemplateDefinition = new ProcessTemplateDefinition();
        processTemplateDefinition.setName("name");
        processTemplateDefinition.setTasks(List.of(taskTypeDefinition));
        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(processTemplateDefinition, "hash");

        ProcessTemplate template = deserializer.toProcessTemplate();

        assertThat(template.getName()).isEqualTo("name");
    }

    @Test
    void toTemplate_domainEventReference() {
        TaskTypeDefinition taskTypeDefinition = new TaskTypeDefinition("task");
        ProcessTemplateDefinition processTemplateDefinition = new ProcessTemplateDefinition();
        processTemplateDefinition.setName("name");
        processTemplateDefinition.setTasks(List.of(taskTypeDefinition));

        MessageReferenceDefinition messageReferenceDefinitionOne = new MessageReferenceDefinition();
        messageReferenceDefinitionOne.setMessageName("EventOne");
        messageReferenceDefinitionOne.setTopicName("TopicNameOne");

        MessageReferenceDefinition messageReferenceDefinitionTwo = new MessageReferenceDefinition();
        messageReferenceDefinitionTwo.setMessageName("EventTwo");
        messageReferenceDefinitionTwo.setTopicName("TopicNameTwo");
        messageReferenceDefinitionTwo.setCorrelationProvider(TestCorrelationProvider.class.getName());

        MessageReferenceDefinition messageReferenceDefinitionThree = new MessageReferenceDefinition();
        messageReferenceDefinitionThree.setMessageName("EventThree");
        messageReferenceDefinitionThree.setTopicName("TopicNameThree");
        messageReferenceDefinitionThree.setPayloadExtractor(TestPayloadExtractor.class.getName());

        MessageReferenceDefinition messageReferenceDefinitionFour = new MessageReferenceDefinition();
        messageReferenceDefinitionFour.setMessageName("EventFour");
        messageReferenceDefinitionFour.setTopicName("TopicNameFour");
        CorrelatedByProcessDataDefinition correlatedByProcessDataDefinition = new CorrelatedByProcessDataDefinition();
        correlatedByProcessDataDefinition.setProcessDataKey("SomeProcessDataKey");
        correlatedByProcessDataDefinition.setMessageDataKey("SomeEventDataKey");
        messageReferenceDefinitionFour.setCorrelatedBy(correlatedByProcessDataDefinition);

        processTemplateDefinition.setMessages(List.of(messageReferenceDefinitionOne, messageReferenceDefinitionTwo, messageReferenceDefinitionThree, messageReferenceDefinitionFour));
        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(processTemplateDefinition, "hash");

        ProcessTemplate template = deserializer.toProcessTemplate();

        assertEquals(4, template.getMessageReferences().size());
        assertEquals("EventOne", template.getMessageReferences().getFirst().getMessageName());
        assertEquals("TopicNameOne", template.getMessageReferences().get(0).getTopicName());
        assertTrue(template.getMessageReferences().get(0).getCorrelationProvider() instanceof MessageProcessIdCorrelationProvider);
        assertTrue(template.getMessageReferences().get(1).getCorrelationProvider() instanceof TestCorrelationProvider);
        assertTrue(template.getMessageReferences().get(2).getPayloadExtractor() instanceof TestPayloadExtractor);
        assertTrue(template.getMessageReferences().get(2).getPayloadExtractor() instanceof TestPayloadExtractor);
        assertEquals("SomeProcessDataKey", template.getMessageReferences().get(3).getCorrelatedByProcessData().getProcessDataKey());
        assertEquals("SomeEventDataKey", template.getMessageReferences().get(3).getCorrelatedByProcessData().getMessageDataKey());
    }

    @Test
    void toTemplate_domainEventReference_createProcessInstanceIfNeeded() {
        TaskTypeDefinition taskTypeDefinition = new TaskTypeDefinition("task");
        ProcessTemplateDefinition processTemplateDefinition = new ProcessTemplateDefinition();
        processTemplateDefinition.setName("name");
        processTemplateDefinition.setTasks(List.of(taskTypeDefinition));

        MessageReferenceDefinition messageReferenceDefinitionTrueNoCondition = new MessageReferenceDefinition();
        messageReferenceDefinitionTrueNoCondition.setMessageName("EventTrueNoCondition");
        messageReferenceDefinitionTrueNoCondition.setTopicName("topic");
        messageReferenceDefinitionTrueNoCondition.setTriggersProcessInstantiation(true);
        MessageReferenceDefinition messageReferenceDefinitionTrueWithCondition = new MessageReferenceDefinition();
        messageReferenceDefinitionTrueWithCondition.setMessageName("EventTrueWithCondition");
        messageReferenceDefinitionTrueWithCondition.setTopicName("topic");
        messageReferenceDefinitionTrueWithCondition.setTriggersProcessInstantiation(true);
        messageReferenceDefinitionTrueWithCondition.setProcessInstantiationCondition(TestProcessInstantiationCondition.class.getName());
        MessageReferenceDefinition messageReferenceDefinitionFalseNoCondition = new MessageReferenceDefinition();
        messageReferenceDefinitionFalseNoCondition.setMessageName("EventFalseNoCondition");
        messageReferenceDefinitionFalseNoCondition.setTopicName("topic");
        messageReferenceDefinitionFalseNoCondition.setTriggersProcessInstantiation(false);
        MessageReferenceDefinition messageReferenceDefinitionFalseWithCondition = new MessageReferenceDefinition();
        messageReferenceDefinitionFalseWithCondition.setMessageName("EventFalseWithCondition");
        messageReferenceDefinitionFalseWithCondition.setTopicName("topic");
        messageReferenceDefinitionFalseWithCondition.setTriggersProcessInstantiation(false);
        messageReferenceDefinitionFalseWithCondition.setProcessInstantiationCondition(TestProcessInstantiationCondition.class.getName());
        MessageReferenceDefinition messageReferenceDefinitionNullWithCondition = new MessageReferenceDefinition();
        messageReferenceDefinitionNullWithCondition.setMessageName("EventNullWithCondition");
        messageReferenceDefinitionNullWithCondition.setTopicName("topic");
        messageReferenceDefinitionNullWithCondition.setProcessInstantiationCondition(TestProcessInstantiationCondition.class.getName());
        MessageReferenceDefinition messageReferenceDefinitionDefault = new MessageReferenceDefinition();
        messageReferenceDefinitionDefault.setMessageName("EventDefault");
        messageReferenceDefinitionDefault.setTopicName("topic");

        processTemplateDefinition.setMessages(List.of(
                messageReferenceDefinitionTrueNoCondition, messageReferenceDefinitionTrueWithCondition,
                messageReferenceDefinitionFalseNoCondition, messageReferenceDefinitionFalseWithCondition,
                messageReferenceDefinitionNullWithCondition, messageReferenceDefinitionDefault));
        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(processTemplateDefinition, "hash");

        ProcessTemplate template = deserializer.toProcessTemplate();

        assertTrue(template.getMessageReferences().get(0).getProcessInstantiationCondition() instanceof AlwaysProcessInstantiationCondition);
        assertTrue(template.getMessageReferences().get(1).getProcessInstantiationCondition() instanceof TestProcessInstantiationCondition);
        assertTrue(template.getMessageReferences().get(2).getProcessInstantiationCondition() instanceof NeverProcessInstantiationCondition);
        assertTrue(template.getMessageReferences().get(3).getProcessInstantiationCondition() instanceof NeverProcessInstantiationCondition);
        assertTrue(template.getMessageReferences().get(4).getProcessInstantiationCondition() instanceof TestProcessInstantiationCondition);
        assertTrue(template.getMessageReferences().get(5).getProcessInstantiationCondition() instanceof NeverProcessInstantiationCondition);
    }

    @Test
    void toTemplate_duplicateDomainEventNames() {
        TaskTypeDefinition taskTypeDefinition = new TaskTypeDefinition("task");
        ProcessTemplateDefinition processTemplateDefinition = new ProcessTemplateDefinition();
        processTemplateDefinition.setName("name");
        processTemplateDefinition.setTasks(List.of(taskTypeDefinition));

        MessageReferenceDefinition messageReferenceDefinitionOne = new MessageReferenceDefinition();
        messageReferenceDefinitionOne.setMessageName("EventOne");
        messageReferenceDefinitionOne.setTopicName("TopicNameOne");

        processTemplateDefinition.setMessages(List.of(messageReferenceDefinitionOne,
                messageReferenceDefinitionOne));
        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(processTemplateDefinition, "hash");

        TemplateDefinitionException ex = assertThrows(TemplateDefinitionException.class,
                deserializer::toProcessTemplate);

        assertTrue(ex.getMessage().contains("EventOne"), ex::getMessage);
    }

    @Test
    void toTaskType_defaultsAkaMandatory() {
        TaskTypeDefinition taskTypeDefinition = new TaskTypeDefinition("name");
        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(new ProcessTemplateDefinition(), "hash");

        TaskType taskType = deserializer.toTaskType(taskTypeDefinition, 2);
        assertEquals("name", taskType.getName());
        assertSame(STATIC, taskType.getLifecycle());
        assertSame(SINGLE_INSTANCE, taskType.getCardinality());
        assertSame(2, taskType.getIndex());
    }

    @Test
    void toTaskType_explicitCardinalityLegacySingle() {
        TaskTypeDefinition definition = new TaskTypeDefinition("name", null, "SINGLE");
        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(new ProcessTemplateDefinition(), "hash");

        TaskType taskType = deserializer.toTaskType(definition, 0);
        assertSame(STATIC, taskType.getLifecycle());
        assertSame(SINGLE_INSTANCE, taskType.getCardinality());
    }

    @Test
    void toTaskType_explicitCardinalityLegacyDynamic() {
        TaskTypeDefinition definition = new TaskTypeDefinition("name", null, "DYNAMIC");
        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(new ProcessTemplateDefinition(), "hash");

        TaskType taskType = deserializer.toTaskType(definition, 0);
        assertSame(DYNAMIC, taskType.getLifecycle());
        assertSame(MULTI_INSTANCE, taskType.getCardinality());
    }

    @Test
    void toTaskType_explicitLifecycleStatic() {
        TaskTypeDefinition definition = new TaskTypeDefinition("name", STATIC, null);
        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(new ProcessTemplateDefinition(), "hash");

        TaskType taskType = deserializer.toTaskType(definition, 0);
        assertSame(STATIC, taskType.getLifecycle());
        assertSame(SINGLE_INSTANCE, taskType.getCardinality());
    }

    @Test
    void toTaskType_explicitLifecycleDynamic() {
        TaskTypeDefinition definition = new TaskTypeDefinition("name", DYNAMIC, null);
        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(new ProcessTemplateDefinition(), "hash");

        TaskType taskType = deserializer.toTaskType(definition, 0);
        assertSame(DYNAMIC, taskType.getLifecycle());
        assertSame(MULTI_INSTANCE, taskType.getCardinality());
    }

    @Test
    void toTaskType_explicitLifecycleObserved() {
        TaskTypeDefinition definition = new TaskTypeDefinition("name", OBSERVED, null);
        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(new ProcessTemplateDefinition(), "hash");

        TaskType taskType = deserializer.toTaskType(definition, 0);
        assertSame(OBSERVED, taskType.getLifecycle());
        assertSame(MULTI_INSTANCE, taskType.getCardinality());
    }

    @Test
    void toTaskType_deprecatedCardinalityWithLifecycle() {
        TaskTypeDefinition definition = new TaskTypeDefinition("deprecated-with-lifecycle", STATIC, "single");
        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(new ProcessTemplateDefinition(), "hash");

        assertThatThrownBy(() -> deserializer.toTaskType(definition, 0))
            .isInstanceOf(TemplateDefinitionException.class)
            .hasMessageContaining("single")
            .hasMessageContaining("deprecated-with-lifecycle");
    }

    @Test
    void toTaskType_unsupportedCardinalityWithLifecycle() {
        TaskTypeDefinition definition = new TaskTypeDefinition("unsupported-cardinality", STATIC, "multi-instance");
        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(new ProcessTemplateDefinition(), "hash");

        assertThatThrownBy(() -> deserializer.toTaskType(definition, 0))
                .isInstanceOf(TemplateDefinitionException.class)
                .hasMessageContaining(STATIC.name())
                .hasMessageContaining("MULTI_INSTANCE");
    }

    @Test
    void toTaskType_invalidCardinality() {
        TaskTypeDefinition definition = new TaskTypeDefinition("invalid-cardinality", STATIC, "invalid");
        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(new ProcessTemplateDefinition(), "hash");

        assertThatThrownBy(() -> deserializer.toTaskType(definition, 0))
                .isInstanceOf(TemplateDefinitionException.class)
                .hasMessageContaining("invalid");
    }

    @Test
    void toTaskType_domainEventCompletionConditionals() {
        TaskCompletionConditionDefinition completedBy = new TaskCompletionConditionDefinition();
        completedBy.setMessage("domainEvent");
        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(new ProcessTemplateDefinition(), "hash");

        TaskTypeDefinition taskTypeDefinition = new TaskTypeDefinition("name");
        taskTypeDefinition.setCompletedBy(completedBy);

        TaskType taskType = deserializer.toTaskType(taskTypeDefinition, 0);
        assertThat(taskType.getCompletedByDomainEvent()).isEqualTo("domainEvent");
    }

    @Test
    void toTaskType_plannedByDomainEvent() {
        TaskPlannedByConditionDefinition plannedBy = new TaskPlannedByConditionDefinition();
        plannedBy.setMessage("domainEvent");
        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(new ProcessTemplateDefinition(), "hash");

        TaskTypeDefinition taskTypeDefinition = new TaskTypeDefinition("name");
        taskTypeDefinition.setPlannedBy(plannedBy);
        taskTypeDefinition.setLifecycle(DYNAMIC);

        TaskType taskType = deserializer.toTaskType(taskTypeDefinition, 0);
        assertNotNull(taskType.getPlannedByDomainEvent());
    }

    @Test
    void toTaskType_plannedByDomainEventWithCondition() {
        TaskPlannedByConditionDefinition plannedBy = new TaskPlannedByConditionDefinition();
        plannedBy.setMessage("domainEvent");
        plannedBy.setCondition("ch.admin.bit.jeap.processcontext.repository.template.json.stubs.TestTaskInstantiationCondition");
        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(new ProcessTemplateDefinition(), "hash");

        TaskTypeDefinition taskTypeDefinition = new TaskTypeDefinition("name");
        taskTypeDefinition.setLifecycle(DYNAMIC);
        taskTypeDefinition.setPlannedBy(plannedBy);

        TaskType taskType = deserializer.toTaskType(taskTypeDefinition, 0);
        assertNotNull(taskType.getPlannedByDomainEvent());
        assertNotNull(taskType.getInstantiationCondition());
        assertTrue(taskType.getInstantiationCondition() instanceof TaskInstantiationCondition);
    }

    @Test
    void toTaskType_plannedByFailsForStaticLifecycle() {
        TaskPlannedByConditionDefinition plannedBy = new TaskPlannedByConditionDefinition();
        plannedBy.setMessage("domainEvent");
        plannedBy.setCondition("ch.admin.bit.jeap.processcontext.repository.template.json.stubs.TestTaskInstantiationCondition");

        TaskTypeDefinition taskTypeDefinition = new TaskTypeDefinition("name");
        taskTypeDefinition.setLifecycle(STATIC);
        taskTypeDefinition.setPlannedBy(plannedBy);

        ProcessTemplateDefinition processTemplateDefinition = new ProcessTemplateDefinition();
        processTemplateDefinition.setName("name");
        processTemplateDefinition.setTasks(List.of(taskTypeDefinition));

        MessageReferenceDefinition messageReferenceDefinitionOne = new MessageReferenceDefinition();
        messageReferenceDefinitionOne.setMessageName("domainEvent");
        messageReferenceDefinitionOne.setTopicName("TopicNameOne");

        processTemplateDefinition.setMessages(List.of(messageReferenceDefinitionOne));

        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(processTemplateDefinition, "hash");

        // when
        TemplateDefinitionException ex = assertThrows(TemplateDefinitionException.class,
                deserializer::toProcessTemplate);

        // then
        assertTrue(ex.getMessage().contains("A static task cannot have a plannedBy section."), ex::getMessage);
    }

    @Test
    void toTaskType_observed() {
        TaskObservesConditionDefinition observes = new TaskObservesConditionDefinition();
        observes.setMessage("domainEvent");

        TaskTypeDefinition taskTypeDefinition = new TaskTypeDefinition("name");
        taskTypeDefinition.setObserves(observes);

        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(new ProcessTemplateDefinition(), "hash");
        TaskType taskType = deserializer.toTaskType(taskTypeDefinition, 0);
        assertNotNull(taskType.getObservesDomainEvent());
    }

    @Test
    void toTaskType_observedWithCondition() {
        TaskObservesConditionDefinition observes = new TaskObservesConditionDefinition();
        observes.setMessage("domainEvent");
        observes.setCondition("ch.admin.bit.jeap.processcontext.repository.template.json.stubs.TestTaskInstantiationCondition");

        TaskTypeDefinition taskTypeDefinition = new TaskTypeDefinition("name");
        taskTypeDefinition.setObserves(observes);

        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(new ProcessTemplateDefinition(), "hash");
        TaskType taskType = deserializer.toTaskType(taskTypeDefinition, 0);
        assertNotNull(taskType.getObservesDomainEvent());
        assertNotNull(taskType.getInstantiationCondition());
        assertTrue(taskType.getInstantiationCondition() instanceof TaskInstantiationCondition);
    }

    @Test
    void toTaskType_missingSourceMessageInTaskData() {
        TaskTypeDefinition definition = new TaskTypeDefinition("task-data-missing-source-message");
        TaskDataDefinition taskDataDefinition = new TaskDataDefinition();
        taskDataDefinition.setSourceMessage(null);
        taskDataDefinition.setMessageDataKeys(Set.of("foo", "bar"));
        definition.setTaskData(Set.of(taskDataDefinition));
        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(new ProcessTemplateDefinition(), "hash");

        assertThatThrownBy(() -> deserializer.toTaskType(definition, 0))
                .isInstanceOf(TemplateDefinitionException.class)
                .hasMessageContaining("Missing mandatory task data property 'messageSource'");
    }

    @Test
    void toTaskType_ValidateSourceMessageIsPlanningOrCompletingInTaskData() {
        TaskTypeDefinition definition = new TaskTypeDefinition("task-data-planning-or-completing-source-message");
        definition.setLifecycle(DYNAMIC);
        TaskPlannedByConditionDefinition taskPlannedByConditionDefinition = new TaskPlannedByConditionDefinition();
        taskPlannedByConditionDefinition.setMessage("planningMessage");
        definition.setPlannedBy(taskPlannedByConditionDefinition);
        TaskCompletionConditionDefinition taskCompletionConditionDefinition = new TaskCompletionConditionDefinition();
        taskCompletionConditionDefinition.setMessage("completingMessage");
        definition.setCompletedBy(taskCompletionConditionDefinition);

        TaskDataDefinition taskDataDefinition = new TaskDataDefinition();
        taskDataDefinition.setSourceMessage("unrelatedMessage");
        taskDataDefinition.setMessageDataKeys(Set.of("foo", "bar"));
        definition.setTaskData(Set.of(taskDataDefinition));

        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(new ProcessTemplateDefinition(), "hash");

        // source message not planning or completing the task should throw
        assertThatThrownBy(() -> deserializer.toTaskType(definition, 0))
                .isInstanceOf(TemplateDefinitionException.class)
                .hasMessageContaining("""
                        The following messages are declared as sources for a task's data but are not planning or \
                        completing the task: unrelatedMessage""");

        // source message planning the task should not throw
        taskDataDefinition.setSourceMessage("planningMessage");
        assertDoesNotThrow( () -> deserializer.toTaskType(definition, 0));

        // source message completing the task should not throw
        taskDataDefinition.setSourceMessage("completingMessage");
        assertDoesNotThrow( () -> deserializer.toTaskType(definition, 0));
    }

    @Test
    void toTaskType_ValidateSourceMessageIsObservingInTaskData() {
        TaskTypeDefinition definition = new TaskTypeDefinition("task-data-observing-source-message");
        TaskObservesConditionDefinition taskObservesConditionDefinition = new TaskObservesConditionDefinition();
        taskObservesConditionDefinition.setMessage("observingMessage");
        definition.setObserves(taskObservesConditionDefinition);

        TaskDataDefinition taskDataDefinition = new TaskDataDefinition();
        taskDataDefinition.setSourceMessage("unrelatedMessage");
        taskDataDefinition.setMessageDataKeys(Set.of("foo", "bar"));
        definition.setTaskData(Set.of(taskDataDefinition));

        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(new ProcessTemplateDefinition(), "hash");

        // source message not observing the task should throw
        assertThatThrownBy(() -> deserializer.toTaskType(definition, 0))
                .isInstanceOf(TemplateDefinitionException.class)
                .hasMessageContaining("""
                        The following messages are declared as sources for a task's data but are not planning or \
                        completing the task: unrelatedMessage""");

        // source message observing the task should not throw
        taskDataDefinition.setSourceMessage("observingMessage");
        assertDoesNotThrow( () -> deserializer.toTaskType(definition, 0));
    }

    @Test
    void toTaskType_missingOrEmptyMessageDataKeysInTaskData() {
        TaskTypeDefinition definition = new TaskTypeDefinition("task-data-missing-or-empty-message-data-keys");
        TaskPlannedByConditionDefinition taskPlannedByConditionDefinition = new TaskPlannedByConditionDefinition();
        taskPlannedByConditionDefinition.setMessage("planningMessage");
        definition.setPlannedBy(taskPlannedByConditionDefinition);
        TaskDataDefinition taskDataDefinition = new TaskDataDefinition();
        taskDataDefinition.setSourceMessage("planningMessage");
        definition.setTaskData(Set.of(taskDataDefinition));
        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(new ProcessTemplateDefinition(), "hash");

        // missing event data keys should throw
        taskDataDefinition.setMessageDataKeys(null);
        assertThatThrownBy(() -> deserializer.toTaskType(definition, 0))
                .isInstanceOf(TemplateDefinitionException.class)
                .hasMessageContaining("Missing or empty mandatory task data property 'eventDataKeys'");

        // empty event data keys should throw
        taskDataDefinition.setMessageDataKeys(Set.of());
        assertThatThrownBy(() -> deserializer.toTaskType(definition, 0))
                .isInstanceOf(TemplateDefinitionException.class)
                .hasMessageContaining("Missing or empty mandatory task data property 'eventDataKeys'");
    }

    @Test
    void toTaskType_TaskDataNotMandatory() {
        TaskTypeDefinition definition = new TaskTypeDefinition("task-data-not-mandatory");
        definition.setTaskData(null);
        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(new ProcessTemplateDefinition(), "hash");

        TaskType taskType = deserializer.toTaskType(definition, 0);
        assertNotNull(taskType.getTaskData());
        assertTrue(taskType.getTaskData().isEmpty());
    }

    @Test
    void toTaskType_missingMessageDataKeysInTaskData() {
        TaskTypeDefinition definition = new TaskTypeDefinition("task-data-missing-source-message");
        TaskDataDefinition taskDataDefinition = new TaskDataDefinition();
        taskDataDefinition.setSourceMessage(null);
        taskDataDefinition.setMessageDataKeys(Set.of("foo", "bar"));
        definition.setTaskData(Set.of(taskDataDefinition));

        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(new ProcessTemplateDefinition(), "hash");

        assertThatThrownBy(() -> deserializer.toTaskType(definition, 0))
                .isInstanceOf(TemplateDefinitionException.class)
                .hasMessageContaining("Missing mandatory task data property 'messageSource'");
    }

    @Test
    void toTaskType_ValidatePlanningAndCompletingTaskData() {
        TaskTypeDefinition definition = new TaskTypeDefinition("task-data-planning-and-completing");
        definition.setLifecycle(DYNAMIC);
        TaskPlannedByConditionDefinition taskPlannedByConditionDefinition = new TaskPlannedByConditionDefinition();
        taskPlannedByConditionDefinition.setMessage("planningMessage");
        definition.setPlannedBy(taskPlannedByConditionDefinition);
        TaskCompletionConditionDefinition taskCompletionConditionDefinition = new TaskCompletionConditionDefinition();
        taskCompletionConditionDefinition.setMessage("completingMessage");
        definition.setCompletedBy(taskCompletionConditionDefinition);

        TaskDataDefinition taskDataDefinitionPlanning = new TaskDataDefinition();
        taskDataDefinitionPlanning.setSourceMessage("planningMessage");
        taskDataDefinitionPlanning.setMessageDataKeys(Set.of("foo"));
        TaskDataDefinition taskDataDefinitionCompleting = new TaskDataDefinition();
        taskDataDefinitionCompleting.setSourceMessage("completingMessage");
        taskDataDefinitionCompleting.setMessageDataKeys(Set.of("bar"));
        definition.setTaskData(Set.of(taskDataDefinitionPlanning, taskDataDefinitionCompleting));

        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(new ProcessTemplateDefinition(), "hash");

        TaskType taskType = deserializer.toTaskType(definition, 0);

        Set<TaskData> taskData = taskType.getTaskData();
        assertNotNull(taskData);
        assertEquals(2, taskData.size());
        assertEquals(Set.of("planningMessage", "completingMessage"),
                taskData.stream().map(TaskData::getSourceMessage).collect(Collectors.toSet()));
        assertEquals(Set.of("foo", "bar"),
                taskData.stream().map(TaskData::getMessageDataKeys).flatMap(Set::stream).collect(Collectors.toSet()));
    }


    @Test
    void toTemplate_plannedByEventNameNotDefined() {
        // given
        TaskTypeDefinition taskTypeDefinition = new TaskTypeDefinition("task");
        TaskPlannedByConditionDefinition taskPlannedByCondition= new TaskPlannedByConditionDefinition();
        taskPlannedByCondition.setMessage("domainEvent");
        taskTypeDefinition.setPlannedBy(taskPlannedByCondition);
        ProcessTemplateDefinition processTemplateDefinition = new ProcessTemplateDefinition();
        processTemplateDefinition.setName("name");
        processTemplateDefinition.setTasks(List.of(taskTypeDefinition));

        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(processTemplateDefinition, "hash");

        // when
        TemplateDefinitionException ex = assertThrows(TemplateDefinitionException.class,
                deserializer::toProcessTemplate);

        // then
        assertTrue(ex.getMessage().contains("domainEvent"), ex::getMessage);
        assertTrue(ex.getMessage().contains("not contained in event definitions"), ex::getMessage);
    }

    @Test
    void toTemplate_CompletedByEventNameNotDefined() {
        // given
        TaskTypeDefinition taskTypeDefinition = new TaskTypeDefinition("task");
        TaskCompletionConditionDefinition taskCompletionCondition= new TaskCompletionConditionDefinition();
        taskCompletionCondition.setMessage("domainEvent");
        taskTypeDefinition.setCompletedBy(taskCompletionCondition);
        ProcessTemplateDefinition processTemplateDefinition = new ProcessTemplateDefinition();
        processTemplateDefinition.setName("name");
        processTemplateDefinition.setTasks(List.of(taskTypeDefinition));

        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(processTemplateDefinition, "hash");

        // when
        TemplateDefinitionException ex = assertThrows(TemplateDefinitionException.class,
                deserializer::toProcessTemplate);

        // then
        assertTrue(ex.getMessage().contains("domainEvent"), ex::getMessage);
        assertTrue(ex.getMessage().contains("not contained in event definitions"), ex::getMessage);
    }

    @Test
    void toTemplate_ObservesEventNameNotDefined() {
        // given
        TaskTypeDefinition taskTypeDefinition = new TaskTypeDefinition("task");
        TaskObservesConditionDefinition taskObservesCondition = new TaskObservesConditionDefinition();
        taskObservesCondition.setMessage("domainEvent");
        taskTypeDefinition.setObserves(taskObservesCondition);
        ProcessTemplateDefinition processTemplateDefinition = new ProcessTemplateDefinition();
        processTemplateDefinition.setName("name");
        processTemplateDefinition.setTasks(List.of(taskTypeDefinition));

        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(processTemplateDefinition, "hash");

        // when
        TemplateDefinitionException ex = assertThrows(TemplateDefinitionException.class,
                deserializer::toProcessTemplate);

        // then
        assertTrue(ex.getMessage().contains("domainEvent"), ex::getMessage);
        assertTrue(ex.getMessage().contains("not contained in event definitions"), ex::getMessage);
    }

    @Test
    void toTemplate_ObservesAndPlannedByTask() {
        // given
        TaskTypeDefinition taskTypeDefinition = new TaskTypeDefinition("task");
        taskTypeDefinition.setLifecycle(STATIC);
        TaskPlannedByConditionDefinition taskPlannedByConditionDefinition = new TaskPlannedByConditionDefinition();
        taskPlannedByConditionDefinition.setMessage("EventOne");

        TaskObservesConditionDefinition taskObservesCondition = new TaskObservesConditionDefinition();
        taskObservesCondition.setMessage("EventOne");
        taskTypeDefinition.setObserves(taskObservesCondition);
        taskTypeDefinition.setPlannedBy(taskPlannedByConditionDefinition);

        ProcessTemplateDefinition processTemplateDefinition = new ProcessTemplateDefinition();
        processTemplateDefinition.setName("name");
        processTemplateDefinition.setTasks(List.of(taskTypeDefinition));

        MessageReferenceDefinition messageReferenceDefinitionOne = new MessageReferenceDefinition();
        messageReferenceDefinitionOne.setMessageName("EventOne");
        messageReferenceDefinitionOne.setTopicName("TopicNameOne");

        processTemplateDefinition.setMessages(List.of(messageReferenceDefinitionOne));

        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(processTemplateDefinition, "hash");

        // when
        TemplateDefinitionException ex = assertThrows(TemplateDefinitionException.class,
                deserializer::toProcessTemplate);

        // then
        assertTrue(ex.getMessage().contains("Task definition is wrong"), ex::getMessage);
        assertTrue(ex.getMessage().contains("Observed and PlannedBy"), ex::getMessage);

    }

    @Test
    void toTemplate_ObservesAndCompletedByTask() {
        // given
        TaskTypeDefinition taskTypeDefinition = new TaskTypeDefinition("task");
        taskTypeDefinition.setLifecycle(STATIC);
        TaskCompletionConditionDefinition taskCompletionConditionDefinition = new TaskCompletionConditionDefinition();
        taskCompletionConditionDefinition.setMessage("EventOne");

        TaskObservesConditionDefinition taskObservesCondition = new TaskObservesConditionDefinition();
        taskObservesCondition.setMessage("EventOne");
        taskTypeDefinition.setObserves(taskObservesCondition);
        taskTypeDefinition.setCompletedBy(taskCompletionConditionDefinition);

        ProcessTemplateDefinition processTemplateDefinition = new ProcessTemplateDefinition();
        processTemplateDefinition.setName("name");
        processTemplateDefinition.setTasks(List.of(taskTypeDefinition));

        MessageReferenceDefinition messageReferenceDefinitionOne = new MessageReferenceDefinition();
        messageReferenceDefinitionOne.setMessageName("EventOne");
        messageReferenceDefinitionOne.setTopicName("TopicNameOne");

        processTemplateDefinition.setMessages(List.of(messageReferenceDefinitionOne));

        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(processTemplateDefinition, "hash");

        // when
        TemplateDefinitionException ex = assertThrows(TemplateDefinitionException.class,
                deserializer::toProcessTemplate);

        // then
        assertTrue(ex.getMessage().contains("Task definition is wrong"), ex::getMessage);
        assertTrue(ex.getMessage().contains("Observed and CompletedBy "), ex::getMessage);

    }

    @Test
    void toTemplate_eventNameInProcessDetailsNotDefined() {
        // ProcessData
        MessageReferenceDefinition messageReferenceDefinitionOne = new MessageReferenceDefinition();
        messageReferenceDefinitionOne.setMessageName("EventOne");
        messageReferenceDefinitionOne.setTopicName("TopicNameOne");

        MessageReferenceDefinition messageReference2= new MessageReferenceDefinition();
        messageReference2.setMessageName("messageName");
        messageReference2.setTopicName("topic2");

        TaskTypeDefinition taskTypeDefinition = new TaskTypeDefinition("taskName");
        TaskCompletionConditionDefinition completedBy = new TaskCompletionConditionDefinition();
        completedBy.setMessage("messageName");
        taskTypeDefinition.setCompletedBy(completedBy);

        ProcessTemplateDefinition processTemplateDefinition = new ProcessTemplateDefinition();
        processTemplateDefinition.setMessages(List.of(messageReferenceDefinitionOne, messageReference2));
        processTemplateDefinition.setTasks(List.of(taskTypeDefinition));
        processTemplateDefinition.setName("name");

        // ProcessData #1
        ProcessDataSourceDefinition processDataSourceDefinition = new ProcessDataSourceDefinition();
        processDataSourceDefinition.setMessageDataKey("eventDataKey");
        processDataSourceDefinition.setMessage("EventOne");
        ProcessDataDefinition processDataDefinition = new ProcessDataDefinition();
        processDataDefinition.setKey("processDataKey");
        processDataDefinition.setSource(processDataSourceDefinition);

        // ProcessData #2
        ProcessDataSourceDefinition processDataSourceDefinition2 = new ProcessDataSourceDefinition();
        processDataSourceDefinition2.setMessageDataKey("eventDataKey");
        processDataSourceDefinition2.setMessage("EventTwo");
        ProcessDataDefinition processDataDefinition2 = new ProcessDataDefinition();
        processDataDefinition2.setKey("processDataKey");
        processDataDefinition2.setSource(processDataSourceDefinition2);

        processTemplateDefinition.setProcessData(List.of(processDataDefinition, processDataDefinition2));

        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(processTemplateDefinition, "hash");

        TemplateDefinitionException ex = assertThrows(TemplateDefinitionException.class,
                deserializer::toProcessTemplate);
        assertTrue(ex.getMessage().contains("EventTwo"), ex::getMessage);
    }

    @Test
    void toTemplate_conditionCompletionAndDomainEventCompletionBothProvided() {
        TemplateDefinitionException ex = assertThrows(TemplateDefinitionException.class, () ->
               deserializeCompletionCondition("some.condition", "SomeEvent", null, true));
        assertEquals("A process completion must either provide a condition or name a domainevent, but not both at the same time.", ex.getMessage());
    }

    @Test
    void toTemplate_completionWithoutConditionNorDomainEvent() {
        TemplateDefinitionException ex = assertThrows(TemplateDefinitionException.class, () ->
                deserializeCompletionCondition(null, null, null, false));
        assertEquals("A process completion must either provide a condition or name a domainevent, but not both at the same time.", ex.getMessage());
    }

    @Test
    void toTemplate_conditionCompletionWithAdditionalAttributes() {
        TemplateDefinitionException ex = assertThrows(TemplateDefinitionException.class, () ->
                deserializeCompletionCondition("some.condition", null, ProcessCompletionConclusion.SUCCEEDED, false));
        assertEquals("A process completion providing a condition mustn't provide additional attributes.", ex.getMessage());
    }

    @Test
    void toTemplate_domainEvenCompletionWithoutConclusionAndLabel() {
        TemplateDefinitionException ex = assertThrows(TemplateDefinitionException.class, () ->
                deserializeCompletionCondition(null, "SomeEvent", null, true));
        assertTrue(ex.getMessage().contains("SomeEvent"));
        assertTrue(ex.getMessage().contains("A process completion by domain event must provide a conclusion and a name."));
    }

    @Test
    void toTemplate_processCompletionByEventNameNotDefined() {
        TemplateDefinitionException ex = assertThrows(TemplateDefinitionException.class, () ->
                deserializeCompletionCondition(null, "SomeEvent", null, false));
        assertTrue(ex.getMessage().contains("SomeEvent"));
        assertTrue(ex.getMessage().contains("not contained in event definitions"));
    }


    @Test
    void toTemplate_invalidSnapshotCompletion() {
        TemplateDefinitionException ex = assertThrows(TemplateDefinitionException.class, () ->
                deserializeProcessSnapshotDefinition(null, "invalid-completion-conclusion"));
        assertEquals("'invalid-completion-conclusion' is not a valid process snapshot completion value.", ex.getMessage());
    }

    @Test
    void toTemplate_processSnapshotMustBeCreatedEitherByConditionOrCompletion() {
        TemplateDefinitionException ex = assertThrows(TemplateDefinitionException.class, () ->
                deserializeProcessSnapshotDefinition(TestProcessSnapshotCondition.class.getCanonicalName(), "invalid-completion-conclusion"));
        assertEquals("A process snapshot must either specify a condition or a completion, but not both at the same time.", ex.getMessage());
    }


    private void deserializeCompletionCondition(String condition, String message, ProcessCompletionConclusion conclusion, boolean addDomainEventReference) {
        ProcessTemplateDefinition processTemplateDefinition = new ProcessTemplateDefinition();
        processTemplateDefinition.setName("someName");
        if (addDomainEventReference) {
            MessageReferenceDefinition messageReferenceDefinition = new MessageReferenceDefinition();
            messageReferenceDefinition.setMessageName(message);
            messageReferenceDefinition.setTopicName(message + "-topic");
            processTemplateDefinition.setMessages(List.of(messageReferenceDefinition));
        }
        ProcessCompletionDefinition completionDefinition = createProcessCompletionDefinition(condition, message, conclusion, "name");
        processTemplateDefinition.setCompletions(List.of(completionDefinition));
        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(processTemplateDefinition, "hash");
        deserializer.toProcessTemplate();
    }

    private ProcessCompletionDefinition createProcessCompletionDefinition(String condition, String message, ProcessCompletionConclusion conclusion, String name) {
        ProcessCompletionConditionDefinition completionConditionDefinition = new ProcessCompletionConditionDefinition();
        completionConditionDefinition.setCondition(condition);
        completionConditionDefinition.setMessage(message);
        completionConditionDefinition.setConclusion(conclusion);
        completionConditionDefinition.setName(name);
        ProcessCompletionDefinition completionDefinition = new ProcessCompletionDefinition();
        completionDefinition.setCompletedBy(completionConditionDefinition);
        return completionDefinition;
    }

    private ProcessSnapshotDefinition createProcessSnapshotDefinition(String condition, String completion) {
        ProcessSnapshotConditionDefinition createdOn = new ProcessSnapshotConditionDefinition(condition, completion);
        return new ProcessSnapshotDefinition(createdOn);
    }

    @SuppressWarnings("SameParameterValue")
    private void deserializeProcessSnapshotDefinition(String condition, String completionConclusion) {
        ProcessSnapshotConditionDefinition snapshotConditionDefinition = new ProcessSnapshotConditionDefinition(condition, completionConclusion);
        ProcessSnapshotDefinition snapshotDefinition = new ProcessSnapshotDefinition(snapshotConditionDefinition);

        ProcessTemplateDefinition processTemplateDefinition = new ProcessTemplateDefinition();
        processTemplateDefinition.setName("someName");
        processTemplateDefinition.setSnapshots(List.of(snapshotDefinition));
        ProcessTemplateDeserializer deserializer = new ProcessTemplateDeserializer(processTemplateDefinition, "hash");
        deserializer.toProcessTemplate();
    }

}
