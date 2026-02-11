package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.message.MessageData;
import ch.admin.bit.jeap.processcontext.domain.message.MessageRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextFactory;
import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextRepositoryFacade;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.*;
import com.fasterxml.uuid.Generators;
import lombok.experimental.UtilityClass;

import java.time.ZonedDateTime;
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

    public ProcessInstance createProcessWithSingleTaskInstance(String processTemplateName) {
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
        return processInstance;
    }

    public ProcessInstance createProcessWithSingleTaskInstanceSavingProcessData(String processTemplateName, List<ProcessData> processData,
                                                                                ProcessInstanceRepository processInstanceRepository,
                                                                                ProcessDataRepository processDataRepository) {
        ProcessInstance processInstance = createProcessWithSingleTaskInstance(processTemplateName);
        return saveProcessData(processInstance, processData, processInstanceRepository, processDataRepository);
    }

    public ProcessInstance createProcessWithSingleTaskInstanceSavingProcessAndTaskData(String processTemplateName, Set<TaskData> taskData,
                                                                                       List<ProcessData> processData,
                                                                                       ProcessInstanceRepository processInstanceRepository,
                                                                                       ProcessDataRepository processDataRepository) {
        TaskType taskType = TaskType.builder()
                .name(task)
                .lifecycle(TaskLifecycle.STATIC)
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .taskData(taskData)
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name(processTemplateName)
                .templateHash("hash")
                .taskTypes(singletonList(taskType))
                .build();
        ProcessContextRepositoryFacadeStub repositoryFacadeStub = new ProcessContextRepositoryFacadeStub();
        ProcessContextFactory processContextFactory = createProcessContextFactory(repositoryFacadeStub);
        ProcessInstance processInstance = ProcessInstance.createProcessInstance(Generators.timeBasedEpochGenerator().generate().toString(), processTemplate, processContextFactory);
        return saveProcessData(processInstance, processData, processInstanceRepository, processDataRepository);
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
        return ProcessInstance.createProcessInstance(Generators.timeBasedEpochGenerator().generate().toString(), processTemplate, processContextFactory);
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
        return processInstance;
    }

    public ProcessInstance createProcessWithTwoTaskInstancesPlannedAndCompleted() {
        return createProcessWithTwoPlannedTaskInstances();
    }

    public ProcessInstance createProcessWithTwoCompletedTaskInstances() {
        return createProcessWithTwoPlannedTaskInstances();
    }

    public ProcessInstance createProcessWithTwoTaskInstancesNotRequiredAndDeleted() {
        return createProcessWithTwoPlannedTaskInstances();
    }

    private static ProcessContextFactory createProcessContextFactory(ProcessContextRepositoryFacade repositoryFacade) {
        return new ProcessContextFactory(repositoryFacade);
    }

    private static ProcessInstance saveProcessData(ProcessInstance processInstance, List<ProcessData> processData, ProcessInstanceRepository processInstanceRepository, ProcessDataRepository processDataRepository) {
        ProcessInstance savedProcessInstance = processInstanceRepository.save(processInstance);
        processData.forEach(pd -> pd.setProcessInstance(savedProcessInstance));
        processData.forEach(processDataRepository::saveIfNew);
        return savedProcessInstance;
    }

    public TaskInstance createPlannedTaskInstance(ProcessInstance processInstance) {
        TaskType taskType = processInstance.getProcessTemplate().getTaskTypeByName(task).orElseThrow();
        return TaskInstance.createInitialTaskInstance(taskType, processInstance, ZonedDateTime.now());
    }

    public TaskInstance createCompletedTaskInstance(ProcessInstance processInstance) {
        TaskInstance taskInstance = createPlannedTaskInstance(processInstance);
        taskInstance.complete(ZonedDateTime.now());
        return taskInstance;
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
    public static ProcessInstance createProcessWithEventDataProcessData(MessageRepository messageRepository, ProcessInstanceRepository processInstanceRepository, ProcessDataRepository processDataRepository) {
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

        String templateName = processInstance.getProcessTemplateName();
        MessageData messageData1 = new MessageData(templateName, "sourceEventDataKey", "someValue", "someRole");
        MessageData messageData2 = new MessageData(templateName, "sourceEventDataKey", "someValueOtherValue", "someOtherRole");
        MessageData messageData3 = new MessageData(templateName, "anotherSourceEventDataKey", "anotherValue");
        messageRepository.save(Message.messageBuilder()
                .messageName("sourceEventName")
                .messageId(Generators.timeBasedEpochGenerator().generate().toString())
                .idempotenceId(Generators.timeBasedEpochGenerator().generate().toString())
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .messageData(Set.of(messageData1, messageData2))
                .traceId("traceId1")
                .build());
        messageRepository.save(Message.messageBuilder()
                .messageName("anotherSourceEventName")
                .messageId(Generators.timeBasedEpochGenerator().generate().toString())
                .idempotenceId(Generators.timeBasedEpochGenerator().generate().toString())
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .messageData(Set.of(messageData3))
                .traceId("traceId2")
                .build());

        // Create ProcessData corresponding to what would be derived from message data via ProcessDataTemplates
        // targetKeyName <- sourceEventDataKey (from sourceEventName message)
        // anotherTargetKeyName <- anotherSourceEventDataKey (from anotherSourceEventName message)
        List<ProcessData> processData = List.of(
                new ProcessData("targetKeyName", "someValue", "someRole"),
                new ProcessData("targetKeyName", "someValueOtherValue", "someOtherRole"),
                new ProcessData("anotherTargetKeyName", "anotherValue")
        );
        return saveProcessData(processInstance, processData, processInstanceRepository, processDataRepository);
    }

    @SuppressWarnings("java:S5960") // this module provides test code to be used in tests
    public static ProcessInstance createCompletedProcessInstance() {
        ProcessContextRepositoryFacadeStub repositoryFacadeStub = new ProcessContextRepositoryFacadeStub();
        repositoryFacadeStub.setAllTasksInFinalState(true);
        ProcessContextFactory processContextFactory = createProcessContextFactory(repositoryFacadeStub);
        TaskType taskType = TaskType.builder()
                .name(task)
                .lifecycle(TaskLifecycle.STATIC)
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name("template")
                .templateHash("hash")
                .taskTypes(singletonList(taskType))
                .build();
        ProcessInstance processInstance = ProcessInstance.createProcessInstance(Generators.timeBasedEpochGenerator().generate().toString(), processTemplate, processContextFactory);
        processInstance.updateState();
        assertSame(ProcessState.COMPLETED, processInstance.getState());
        return processInstance;
    }
}
