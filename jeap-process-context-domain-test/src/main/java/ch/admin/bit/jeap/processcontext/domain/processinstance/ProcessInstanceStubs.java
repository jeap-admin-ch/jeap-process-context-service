package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.message.MessageData;
import ch.admin.bit.jeap.processcontext.domain.message.MessageRepository;
import ch.admin.bit.jeap.processcontext.domain.message.OriginTaskId;
import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextRepositoryFacade;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.*;
import com.fasterxml.uuid.Generators;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertSame;

@UtilityClass
@SuppressWarnings("java:S115") // constant names deliberately kept lowercase
public class ProcessInstanceStubs {
    public final String task = "task";
    public final String task2 = "task2";
    public final String event = "event";

    public ProcessInstance createProcessWithSingleTaskInstance() {
        return createProcessWithSingleTaskInstance("template");
    }

    public ProcessInstance createProcessWithSingleTaskInstance(String templateName) {
        return createProcessWithSingleTaskInstance(templateName, Set.of());
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
                .build();
        ProcessContextRepositoryFacadeStub repositoryFacadeStub = new ProcessContextRepositoryFacadeStub();
        ProcessContextFactory processContextFactory = createProcessContextFactory(repositoryFacadeStub);
        ProcessInstance processInstance = ProcessInstance.createProcessInstance(Generators.timeBasedEpochGenerator().generate().toString(), processTemplate, processContextFactory);
        processInstance.start();
        repositoryFacadeStub.setProcessInstance(processInstance);
        setProcessData(processInstance, processData);
        return processInstance;
    }

    public ProcessInstance createProcessWithoutTaskInstance() {
        TaskType taskType = TaskType.builder()
                .name(task)
                .lifecycle(TaskLifecycle.OBSERVED)
                .cardinality(TaskCardinality.MULTI_INSTANCE)
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name("template")
                .templateHash("hash")
                .taskTypes(singletonList(
                        taskType))
                .build();
        ProcessContextRepositoryFacadeStub repositoryFacadeStub = new ProcessContextRepositoryFacadeStub();
        ProcessContextFactory processContextFactory = createProcessContextFactory(repositoryFacadeStub);
        ProcessInstance processInstance = ProcessInstance.createProcessInstance(Generators.timeBasedEpochGenerator().generate().toString(), processTemplate, processContextFactory);
        processInstance.start();
        repositoryFacadeStub.setProcessInstance(processInstance);
        return processInstance;
    }

    public ProcessInstance createProcessWithTwoPlannedTaskInstances() {
        TaskType taskType1 = TaskType.builder()
                .name(task)
                .lifecycle(TaskLifecycle.STATIC)
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .build();
        TaskType taskType2 = TaskType.builder()
                .name(task2)
                .lifecycle(TaskLifecycle.STATIC)
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name("template")
                .templateHash("hash")
                .taskTypes(List.of(taskType1, taskType2))
                .build();
        ProcessContextRepositoryFacadeStub repositoryFacadeStub = new ProcessContextRepositoryFacadeStub();
        ProcessContextFactory processContextFactory = createProcessContextFactory(repositoryFacadeStub);
        ProcessInstance processInstance = ProcessInstance.createProcessInstance(Generators.timeBasedEpochGenerator().generate().toString(), processTemplate, processContextFactory);
        processInstance.start();
        repositoryFacadeStub.setProcessInstance(processInstance);
        return processInstance;
    }

    public ProcessInstance createProcessWithTwoTaskInstancesPlannedAndCompleted() {
        ProcessInstance processInstance = createProcessWithTwoPlannedTaskInstances();
        processInstance.getTasks().getFirst().complete(ZonedDateTime.now());
        return processInstance;
    }

    public ProcessInstance createProcessWithTwoCompletedTaskInstances() {
        ProcessInstance processInstance = createProcessWithTwoTaskInstancesPlannedAndCompleted();
        processInstance.getTasks().get(1).complete(ZonedDateTime.now());
        return processInstance;
    }

    public ProcessInstance createProcessWithTwoTaskInstancesNotRequiredAndDeleted() {
        ProcessInstance processInstance = createProcessWithTwoPlannedTaskInstances();
        processInstance.getTasks().getFirst().notRequired();
        processInstance.getTasks().get(1).delete();
        return processInstance;
    }

    private static ProcessContextFactory createProcessContextFactory(ProcessContextRepositoryFacade repositoryFacade) {
        return new ProcessContextFactory(repositoryFacade);
    }

    public ProcessInstance createProcessWithSingleTaskInstanceAndEvent() {
        return createProcessWithSingleTaskInstanceAndEventWithAdditionalMessages("template", Set.of(), Set.of(), List.of());
    }

    public ProcessInstance createProcessWithSingleTaskInstanceAndEventWithAdditionalMessages(
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
                .build();
        ProcessContextRepositoryFacadeStub repositoryFacadeStub = new ProcessContextRepositoryFacadeStub();
        ProcessContextFactory processContextFactory = createProcessContextFactory(repositoryFacadeStub);
        ProcessInstance processInstance = ProcessInstance.createProcessInstance(Generators.timeBasedEpochGenerator().generate().toString(), processTemplate, processContextFactory);
        processInstance.start();
        repositoryFacadeStub.setProcessInstance(processInstance);
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
        setProcessData(processInstance, processData);
        additionalMessages.forEach(processInstance::addMessage);
        return processInstance;
    }

    @SneakyThrows
    private static void setProcessData(ProcessInstance processInstance, Set<ProcessData> processData) {
        processData.forEach(pd -> pd.setProcessInstance(processInstance));
        Field processDataField = ProcessInstance.class.getDeclaredField("processData");
        processDataField.setAccessible(true);
        processDataField.set(processInstance, new HashSet<>(processData));
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
    public static ProcessInstance createProcessWithEventDataProcessData(MessageRepository messageRepository) {
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

        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name("template")
                .templateHash("hash")
                .taskTypes(List.of(firstTask))
                .processDataTemplates(List.of(processDataTemplate1, processDataTemplate2, processDataTemplate3))
                .relationSystemId("ch.admin.test.System")
                .build();

        ProcessContextRepositoryFacadeStub repositoryFacadeStub = new ProcessContextRepositoryFacadeStub();
        ProcessContextFactory processContextFactory = createProcessContextFactory(repositoryFacadeStub);
        ProcessInstance processInstance = ProcessInstance.createProcessInstance(Generators.timeBasedEpochGenerator().generate().toString(), processTemplate, processContextFactory);
        processInstance.start();
        repositoryFacadeStub.setProcessInstance(processInstance);

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
        return processInstance;
    }

    @SuppressWarnings("java:S5960") // this module provides test code to be used in tests
    public static ProcessInstance createCompletedProcessInstance() {
        ProcessInstance processInstance = createProcessWithSingleTaskInstance("template");
        processInstance.getTasks().getFirst().complete(ZonedDateTime.now());
        processInstance.start(); // Force state re-evaluation of the process instance stub after completing the task
        assertSame(ProcessState.COMPLETED, processInstance.getState());
        return processInstance;
    }
}
