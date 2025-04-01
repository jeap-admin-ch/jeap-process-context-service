package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.message.MessageData;
import ch.admin.bit.jeap.processcontext.domain.message.MessageRepository;
import ch.admin.bit.jeap.processcontext.domain.message.OriginTaskId;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.*;
import ch.admin.bit.jeap.processcontext.plugin.api.condition.MilestoneCondition;
import com.fasterxml.uuid.Generators;
import lombok.experimental.UtilityClass;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.*;
import static org.junit.jupiter.api.Assertions.assertSame;

@UtilityClass
@SuppressWarnings("java:S115") // constant names deliberately kept lowercase
public class ProcessInstanceStubs {
    public final String task = "task";
    public final String milestone = "milestone";
    public final String neverReachedMilestone = "neverReachedMilestone";
    public final String event = "event";

    private final MilestoneCondition alwaysTrueMilestoneCondition = processContext -> true;
    private final MilestoneCondition neverTrueMilestoneCondition = processContext -> false;

    public ProcessInstance createProcessWithSingleTaskInstance() {
        return createProcessWithSingleTaskInstance("template", emptySet());
    }

    public ProcessInstance createProcessWithSingleTaskInstance(String processTemplateName, Set<ProcessData> processData) {
        TaskType taskType = TaskType.builder()
                .name(task)
                .lifecycle(TaskLifecycle.STATIC)
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name(processTemplateName)
                .templateHash("hash")
                .taskTypes(singletonList(
                        taskType))
                .milestones(emptyMap())
                .build();
        return ProcessInstance.startProcess(Generators.timeBasedEpochGenerator().generate().toString(), processTemplate, processData);
    }

    public ProcessInstance createProcessWithSingleTaskInstanceAndReachedMilestoneAndEvent() {
        return createProcessWithSingleTaskInstanceAndReachedMilestoneAndEvent(emptySet());
    }

    public ProcessInstance createProcessWithSingleTaskInstanceAndReachedMilestoneAndEvent(Set<ProcessData> processData) {
        return createProcessWithSingleTaskInstanceAndReachedMilestoneAndEventWithAdditionalMessages("template", Set.of(), processData, List.of());
    }

    public ProcessInstance createProcessWithSingleTaskInstanceAndReachedMilestoneAndEventWithAdditionalMessages(
            String templateName, Set<TaskData> taskData, Set<ProcessData> processData, List<Message> additionalMessages) {
        TaskType taskType = TaskType.builder()
                .name(task)
                .lifecycle(TaskLifecycle.STATIC)
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .taskData(taskData)
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name(templateName)
                .templateHash("hash")
                .taskTypes(singletonList(
                        taskType))
                .milestones(Map.of(
                        milestone, alwaysTrueMilestoneCondition,
                        neverReachedMilestone, neverTrueMilestoneCondition))
                .build();
        ProcessInstance processInstance = ProcessInstance.startProcess(Generators.timeBasedEpochGenerator().generate().toString(), processTemplate, processData);
        Message message = Message.messageBuilder()
                .messageName(event)
                .messageId("eventId")
                .idempotenceId("idempotenceId")
                .originTaskIds(OriginTaskId.from(templateName, Set.of("taskId1", "taskId2")))
                .messageData(Set.of(new MessageData(templateName, "myKey", "myValue")))
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .build();
        processInstance.addMessage(message);
        additionalMessages.forEach(processInstance::addMessage);
        processInstance.evaluateReachedMilestones();
        return processInstance;
    }

    public Milestone createMilestone(String name, boolean reached) {
        Milestone milestone = Milestone.createNew(name, alwaysTrueMilestoneCondition, createProcessWithSingleTaskInstance());
        if (reached) {
            milestone.evaluateIfReached(null);
        }
        return milestone;
    }

    public TaskInstance createTaskInstance(String name, int index, String originTaskId) {
        TaskType taskType = TaskType.builder()
                .name(name)
                .lifecycle(TaskLifecycle.DYNAMIC)
                .cardinality(TaskCardinality.MULTI_INSTANCE)
                .index(index)
                .build();

        return TaskInstance.createTaskInstanceWithOriginTaskId(taskType, null, originTaskId, ZonedDateTime.now(), null);
    }

    // Will persist the created events referenced by the created process instance to allow the process instance to be persisted and read back.
    public static ProcessInstance createProcessWithEventDataProcessDataAndRelations(MessageRepository messageRepository) {
        TaskType firstTask = TaskType.builder()
                .name(task)
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

        ProcessInstance processInstance = ProcessInstance.startProcess(Generators.timeBasedEpochGenerator().generate().toString(), processTemplate, emptySet());

        String templateName = processInstance.getProcessTemplateName();
        MessageData messageData1 = new MessageData(templateName, "sourceEventDataKey", "someValue", "someRole");
        MessageData messageData2 = new MessageData(templateName, "sourceEventDataKey", "someValueOtherValue", "someOtherRole");
        MessageData messageData3 = new MessageData(templateName, "anotherSourceEventDataKey", "anotherValue");
        Message message = messageRepository.save(Message.messageBuilder()
                .messageName("sourceEventName")
                .messageId(Generators.timeBasedEpochGenerator().generate().toString())
                .idempotenceId(Generators.timeBasedEpochGenerator().generate().toString())
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .messageData(Set.of(messageData1, messageData2))
                .traceId("traceId1")
                .build());
        Message anotherMessage = messageRepository.save(Message.messageBuilder()
                .messageName("anotherSourceEventName")
                .messageId(Generators.timeBasedEpochGenerator().generate().toString())
                .idempotenceId(Generators.timeBasedEpochGenerator().generate().toString())
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .messageData(Set.of(messageData3))
                .traceId("traceId2")
                .build());
        processInstance.addMessage(message);
        processInstance.addMessage(anotherMessage);
        processInstance.evaluateRelations();
        return processInstance;
    }


    @SuppressWarnings("java:S5960") // this module provides test code to be used in tests
    public static ProcessInstance createCompletedProcessInstance() {
        ProcessInstance processInstance = createProcessWithSingleTaskInstance("template", emptySet());
        processInstance.getTasks().get(0).complete(ZonedDateTime.now());
        processInstance.evaluateCompletedTasks(ZonedDateTime.now());
        assertSame(ProcessState.COMPLETED, processInstance.getState());
        return processInstance;
    }
}
