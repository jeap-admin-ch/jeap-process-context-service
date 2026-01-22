package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.message.MessageData;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.*;
import ch.admin.bit.jeap.processcontext.plugin.api.condition.ProcessSnapshotCondition;
import com.fasterxml.uuid.Generators;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.Collections.emptySet;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProcessInstanceStubs {

    public static final String template = "template";

    static final String singleTaskName = "single";
    static final String dynamicTaskName = "dynamic";
    static final String task1 = "task1";
    static final String task2 = "task2";

    public static ProcessInstance createSimpleProcess() {
        ProcessTemplate processTemplate = createSimpleProcessTemplate();
        return ProcessInstance.startProcess(Generators.timeBasedEpochGenerator().generate().toString(), processTemplate, Set.of());
    }

    public static ProcessTemplate createSimpleProcessTemplate() {
        TaskType taskType = TaskType.builder()
                .name(singleTaskName)
                .lifecycle(TaskLifecycle.STATIC)
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .build();
        return ProcessTemplate.builder()
                .name(template)
                .templateHash("hash")
                .taskTypes(List.of(taskType))
                .build();
    }

    public static ProcessInstance createSimpleCompletedProcess() {
        TaskType taskType = TaskType.builder()
                .name(singleTaskName)
                .lifecycle(TaskLifecycle.STATIC)
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name("template")
                .templateHash("hash")
                .taskTypes(List.of(taskType))
                .build();
        ProcessInstance processInstance = ProcessInstance.startProcess(Generators.timeBasedEpochGenerator().generate().toString(), processTemplate, Set.of());
        processInstance.getTasks().getFirst().complete(ZonedDateTime.now());
        processInstance.evaluateCompletedTasks(ZonedDateTime.now());
        return processInstance;
    }

    static ProcessInstance createProcessWithSingleDynamicTaskInstance() {
        TaskType taskType = TaskType.builder()
                .name(singleTaskName)
                .lifecycle(TaskLifecycle.DYNAMIC)
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .plannedByDomainEvent("messageName1")
                .completedByDomainEvent("messageName")
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name("template")
                .templateHash("hash")
                .taskTypes(List.of(taskType))
                .build();
        ProcessInstance processInstance = ProcessInstance.startProcess(Generators.timeBasedEpochGenerator().generate().toString(), processTemplate, Set.of());
        processInstance.addMessage(Message.messageBuilder()
                .messageId(UUID.randomUUID().toString())
                .idempotenceId("idempotenceId")
                .messageName("messageName1")
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .build());
        processInstance.planDomainEventTask(taskType, "taskId", ZonedDateTime.now(), null);
        return processInstance;
    }

    static ProcessInstance createProcessWithSingleStaticTaskInstance() {
        TaskType taskType = TaskType.builder()
                .name(singleTaskName)
                .lifecycle(TaskLifecycle.STATIC)
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .completedByDomainEvent("messageName")
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name("template")
                .templateHash("hash")
                .taskTypes(List.of(taskType))
                .build();
        return ProcessInstance.startProcess(Generators.timeBasedEpochGenerator().generate().toString(), processTemplate, Set.of());
    }

    public static ProcessInstance createProcessWithRelation() {
        TaskType taskType = TaskType.builder()
                .name(singleTaskName)
                .lifecycle(TaskLifecycle.STATIC)
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name("template")
                .templateHash("hash")
                .taskTypes(List.of(taskType))
                .processDataTemplates(List.of(ProcessDataTemplate.builder()
                        .key("targetKeyName")
                        .sourceMessageName("sourceEventName")
                        .sourceMessageDataKey("sourceEventDataKey")
                        .build()))
                .relationSystemId("ch.admin.test.System")
                .relationPatterns(List.of(RelationPattern.builder()
                        .objectSelector(RelationNodeSelector.builder()
                                .processDataKey("targetKeyName")
                                .type("ch.admin.bit.entity.Foo")
                                .build())
                        .subjectSelector(RelationNodeSelector.builder()
                                .processDataKey("targetKeyName")
                                .type("ch.admin.bit.entity.Bar")
                                .build())
                        .predicateType("ch.admin.bit.test.predicate.Knows")
                        .build()))
                .build();
        ProcessInstance processInstance = ProcessInstance.startProcess(Generators.timeBasedEpochGenerator().generate().toString(), processTemplate, Set.of());
        String templateName = processInstance.getProcessTemplateName();
        MessageData messageData = new MessageData(templateName, "sourceEventDataKey", "someValue", "someRole");
        processInstance.addMessage(Message.messageBuilder()
                .messageId(Generators.timeBasedEpochGenerator().generate().toString())
                .idempotenceId(Generators.timeBasedEpochGenerator().generate().toString())
                .messageData(Set.of(messageData))
                .messageName("sourceEventName")
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .build());
        processInstance.evaluateRelations();
        return processInstance;
    }

    static ProcessInstance createProcessWithSingleAndDynamicTaskInstances() {
        TaskType mandatoryTaskType = TaskType.builder()
                .name(singleTaskName)
                .lifecycle(TaskLifecycle.STATIC)
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .build();
        TaskType dynamicTaskType = TaskType.builder()
                .name(dynamicTaskName)
                .lifecycle(TaskLifecycle.DYNAMIC)
                .cardinality(TaskCardinality.MULTI_INSTANCE)
                .plannedByDomainEvent("messageNamePlan")
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name("template")
                .templateHash("hash")
                .taskTypes(List.of(mandatoryTaskType, dynamicTaskType))
                .build();
        ProcessInstance processInstance = ProcessInstance.startProcess(Generators.timeBasedEpochGenerator().generate().toString(), processTemplate, emptySet());
        processInstance.addMessage(Message.messageBuilder()
                .messageId(UUID.randomUUID().toString())
                .idempotenceId("idempotenceId")
                .messageName("messageNamePlan")
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .build());
        processInstance.planDomainEventTask(dynamicTaskType, "taskId", ZonedDateTime.now(), null);
        return processInstance;
    }

    static ProcessInstance createProcessWithSingleAndDynamicInstanceTasksBothNotYetInstantiated() {
        TaskType mandatoryTaskType = TaskType.builder()
                .name(singleTaskName)
                .lifecycle(TaskLifecycle.STATIC)
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .plannedByDomainEvent("messageName")
                .completedByDomainEvent("messageName2")
                .build();
        TaskType dynamicTaskType = TaskType.builder()
                .name(dynamicTaskName)
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
        processInstance.planDomainEventTask(mandatoryTaskType, "mandatory-id", ZonedDateTime.now(), null);
        processInstance.planDomainEventTask(dynamicTaskType, "multiple-id-1", ZonedDateTime.now(), null);
        processInstance.planDomainEventTask(dynamicTaskType, "multiple-id-2", ZonedDateTime.now(), null);
        return processInstance;
    }

    /**
     * Creates a process instance stub with
     * <pre>
     *     Task task1
     *     ProcessData
     *          key: targetkeyName
     *          source:
     *              eventName: sourceEventName
     *              eventDataKey: sourceEventDataKey
     *     RelationPattern
     *          targetKeyName:someRole --Declares--> targetKeyName:someOtherRole
     *          targetKeyName          --Knows--> anotherTargetKeyName
     *
     * </pre>
     */
    public static ProcessInstance createProcessWithEventData() {
        TaskType firstTask = TaskType.builder()
                .name(task1)
                .lifecycle(TaskLifecycle.STATIC)
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .build();

        ProcessDataTemplate processDataTemplate1 = ProcessDataTemplate.builder()
                .key("targetKeyName")
                .sourceMessageName("sourceEventName")
                .sourceMessageDataKey("sourceEventDataKey").build();
        ProcessDataTemplate processDataTemplate2 = ProcessDataTemplate.builder()
                .key("targetKeyName")
                .sourceMessageName("sourceEventName")
                .sourceMessageDataKey("sourceEventDataKey").build();
        ProcessDataTemplate processDataTemplate3 = ProcessDataTemplate.builder()
                .key("anotherTargetKeyName")
                .sourceMessageName("anotherSourceEventName")
                .sourceMessageDataKey("anotherSourceEventDataKey").build();

        RelationPattern relationPattern1 = RelationPattern.builder()
                .objectSelector(RelationNodeSelector.builder()
                        .processDataKey("targetKeyName")
                        .processDataRole("someRole")
                        .type("ch.admin.bit.entity.Some")
                        .build())
                .subjectSelector(RelationNodeSelector.builder()
                        .processDataKey("targetKeyName")
                        .processDataRole("someOtherRole")
                        .type("ch.admin.bit.entity.Other")
                        .build())
                .predicateType("ch.admin.bit.test.predicate.Declares")
                .build();
        RelationPattern relationPattern2 = RelationPattern.builder()
                .objectSelector(RelationNodeSelector.builder()
                        .processDataKey("targetKeyName")
                        .type("ch.admin.bit.entity.Foo")
                        .build())
                .subjectSelector(RelationNodeSelector.builder()
                        .processDataKey("anotherTargetKeyName")
                        .type("ch.admin.bit.entity.Bar")
                        .build())
                .predicateType("ch.admin.bit.test.predicate.Knows")
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name("template")
                .templateHash("hash")
                .taskTypes(List.of(firstTask))
                .processDataTemplates(List.of(processDataTemplate1, processDataTemplate2, processDataTemplate3))
                .relationSystemId("ch.admin.test.System")
                .relationPatterns(List.of(relationPattern1, relationPattern2))
                .build();

        return ProcessInstance.startProcess(Generators.timeBasedEpochGenerator().generate().toString(), processTemplate, emptySet());
    }

    /**
     * Creates a process instance stub with
     * <pre>
     *     Task task1
     *     ProcessData
     *          key: targetKeyName
     *          source:
     *              eventName: sourceEventName
     *              eventDataKey: sourceEventDataKey
     *          key: anotherTargetKeyName
     *          source:
     *              eventName: anotherSourceEventName
     *              eventDataKey: anotherSourceEventDataKey
     *     RelationPattern (joinType: byRole)
     *          targetKeyName          --Knows--> anotherTargetKeyName
     *
     * </pre>
     */
    public static ProcessInstance createProcessWithEventDataAndJoinByRole() {
        TaskType firstTask = TaskType.builder()
                .name(task1)
                .lifecycle(TaskLifecycle.STATIC)
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .build();

        ProcessDataTemplate processDataTemplate1 = ProcessDataTemplate.builder()
                .key("targetKeyName")
                .sourceMessageName("sourceEventName")
                .sourceMessageDataKey("sourceEventDataKey").build();
        ProcessDataTemplate processDataTemplate2 = ProcessDataTemplate.builder()
                .key("anotherTargetKeyName")
                .sourceMessageName("anotherSourceEventName")
                .sourceMessageDataKey("anotherSourceEventDataKey").build();

        RelationPattern relationPattern = RelationPattern.builder()
                .objectSelector(RelationNodeSelector.builder()
                        .processDataKey("targetKeyName")
                        .type("ch.admin.bit.entity.Foo")
                        .build())
                .subjectSelector(RelationNodeSelector.builder()
                        .processDataKey("anotherTargetKeyName")
                        .type("ch.admin.bit.entity.Bar")
                        .build())
                .predicateType("ch.admin.bit.test.predicate.Knows")
                .joinType("byRole")
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name("template")
                .templateHash("hash")
                .taskTypes(List.of(firstTask))
                .processDataTemplates(List.of(processDataTemplate1, processDataTemplate2))
                .relationSystemId("ch.admin.test.System")
                .relationPatterns(List.of(relationPattern))
                .build();

        return ProcessInstance.startProcess(Generators.timeBasedEpochGenerator().generate().toString(), processTemplate, emptySet());
    }

    /**
     * Creates a process instance stub with
     * <pre>
     *     Task task1
     *     ProcessData
     *          key: targetKeyName
     *          source:
     *              eventName: sourceEventName
     *              eventDataKey: sourceEventDataKey
     *          key: anotherTargetKeyName
     *          source:
     *              eventName: anotherSourceEventName
     *              eventDataKey: anotherSourceEventDataKey
     *     RelationPattern (joinType: byValue)
     *          targetKeyName          --Knows--> anotherTargetKeyName
     *
     * </pre>
     */
    public static ProcessInstance createProcessWithEventDataAndJoinByValue() {
        TaskType firstTask = TaskType.builder()
                .name(task1)
                .lifecycle(TaskLifecycle.STATIC)
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .build();

        ProcessDataTemplate processDataTemplate1 = ProcessDataTemplate.builder()
                .key("targetKeyName")
                .sourceMessageName("sourceEventName")
                .sourceMessageDataKey("sourceEventDataKey").build();
        ProcessDataTemplate processDataTemplate2 = ProcessDataTemplate.builder()
                .key("anotherTargetKeyName")
                .sourceMessageName("anotherSourceEventName")
                .sourceMessageDataKey("anotherSourceEventDataKey").build();

        RelationPattern relationPattern = RelationPattern.builder()
                .objectSelector(RelationNodeSelector.builder()
                        .processDataKey("targetKeyName")
                        .type("ch.admin.bit.entity.Foo")
                        .build())
                .subjectSelector(RelationNodeSelector.builder()
                        .processDataKey("anotherTargetKeyName")
                        .type("ch.admin.bit.entity.Bar")
                        .build())
                .predicateType("ch.admin.bit.test.predicate.Knows")
                .joinType("byValue")
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name("template")
                .templateHash("hash")
                .taskTypes(List.of(firstTask))
                .processDataTemplates(List.of(processDataTemplate1, processDataTemplate2))
                .relationSystemId("ch.admin.test.System")
                .relationPatterns(List.of(relationPattern))
                .build();

        return ProcessInstance.startProcess(Generators.timeBasedEpochGenerator().generate().toString(), processTemplate, emptySet());
    }

    static ProcessInstance createProcessWithProcessSnapshotCondition(ProcessSnapshotCondition processSnapshotCondition) {
        TaskType taskType = TaskType.builder()
                .name(singleTaskName)
                .lifecycle(TaskLifecycle.STATIC)
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name("template")
                .templateHash("hash")
                .taskTypes(List.of(taskType))
                .processSnapshotConditions(List.of(processSnapshotCondition))
                .build();
        return ProcessInstance.startProcess(Generators.timeBasedEpochGenerator().generate().toString(), processTemplate, Set.of());
    }

}
