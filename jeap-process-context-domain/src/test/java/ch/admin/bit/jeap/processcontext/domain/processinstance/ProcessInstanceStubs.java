package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextFactory;
import ch.admin.bit.jeap.processcontext.domain.processinstance.snapshot.ProcessSnapshotCondition;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.*;
import com.fasterxml.uuid.Generators;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProcessInstanceStubs {

    public static final String template = "template";

    static final String singleTaskName = "single";
    static final String dynamicTaskName = "dynamic";
    static final String task1 = "task1";

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

    public static ProcessInstance createProcessWithSingleDynamicTask() {
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
        return startProcessInstance(processTemplate);
    }

    static ProcessInstance createProcessWithSingleStaticTask() {
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
        return startProcessInstance(processTemplate);
    }

    static ProcessInstance createProcessWithSingleAndDynamicTasks() {
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
        ProcessInstance processInstance = startProcessInstance(processTemplate);
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
    public static ProcessInstance createProcessWithProcessData() {
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

        return startProcessInstance(processTemplate);
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
        return startProcessInstance(processTemplate);
    }

    private static ProcessInstance startProcessInstance(ProcessTemplate processTemplate) {
        ProcessContextRepositoryFacadeStub repositoryFacade = new ProcessContextRepositoryFacadeStub();
        ProcessContextFactory processContextFactory = new ProcessContextFactory(repositoryFacade);
        return ProcessInstance.createProcessInstance(Generators.timeBasedEpochGenerator().generate().toString(), processTemplate, processContextFactory);
    }
}
