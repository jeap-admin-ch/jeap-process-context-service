package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.message.MessageData;
import ch.admin.bit.jeap.processcontext.domain.message.OriginTaskId;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskType;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessCompletionConclusion;
import ch.admin.bit.jeap.processcontext.plugin.api.context.TaskState;
import ch.admin.bit.jeap.processcontext.plugin.api.context.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.*;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.*;

class ProcessContextFactoryTest {

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void createProcessContext() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleDynamicTaskInstance();
        String templateName = processInstance.getProcessTemplateName();
        Set<String> taskNames =  Set.of("taskId1", "taskId2");
        TaskInstance taskInstance = processInstance.getTasks().getFirst();
        Message domainMessage = Message.messageBuilder()
                .messageName("event")
                .messageId("eventId")
                .idempotenceId("idempotenceId")
                .originTaskIds(OriginTaskId.from(templateName, taskNames))
                .messageData(Set.of(new MessageData(templateName, "myKey", "myValue")))
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .build();
        processInstance.addMessage(domainMessage);
        ReflectionTestUtils.setField(processInstance, "processCompletion", new ProcessCompletion(
                ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessCompletionConclusion.SUCCEEDED, "all good", ZonedDateTime.now()));

        ProcessContext processContext = ProcessContextFactory.createProcessContext(processInstance);

        assertEquals(processInstance.getOriginProcessId(), processContext.getOriginProcessId());
        assertEquals(processInstance.getProcessTemplate().getName(), processContext.getProcessName());
        assertEquals(processInstance.getState().name(), processContext.getProcessState().name());
        assertEquals(2, processContext.getMessages().size());
        ch.admin.bit.jeap.processcontext.plugin.api.context.Message processContextEvent = processContext.getMessages().get(1);

        assertEquals(domainMessage.getMessageName(), processContextEvent.getName());
        assertEquals(taskNames, processContextEvent.getRelatedOriginTaskIds());
        MessageData messageData = domainMessage.getMessageData(templateName).iterator().next();
        assertEquals(1, processContextEvent.getMessageData().size());
        ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData eventDataFromAPI = processContextEvent.getMessageData().iterator().next();
        assertEquals(messageData.getKey(), eventDataFromAPI.getKey());
        assertEquals(messageData.getValue(), eventDataFromAPI.getValue());
        assertNull(messageData.getRole());

        assertEquals(1, processContext.getTasks().size());
        Task processContextTask = processContext.getTasks().getFirst();
        assertEquals(taskInstance.requireTaskType().getName(), processContextTask.getType().getName());
        assertEquals(taskInstance.requireTaskType().getCardinality().name(), processContextTask.getType().getCardinality().name());
        assertEquals(taskInstance.getState().name(), processContextTask.getState().name());
        assertEquals(taskInstance.getOriginTaskId(), processContextTask.getOriginTaskId().orElseThrow());

        assertNotNull(processContext.getProcessCompletion());
        assertEquals(processInstance.getProcessCompletion().get().getConclusion(), processContext.getProcessCompletion().getConclusion());
        assertEquals(processInstance.getProcessCompletion().get().getCompletedAt(), processContext.getProcessCompletion().getCompletedAt());
    }

    @Test
    void allTaskStatesMappedInApi() {
        Set<String> domainStateNames = Arrays.stream(ch.admin.bit.jeap.processcontext.domain.processinstance.TaskState.values())
                .map(Enum::name)
                .collect(toSet());

        domainStateNames.forEach(TaskState::valueOf);

        assertEquals(TaskState.values().length, domainStateNames.size());
    }

    @Test
    void allProcessStatesMappedInApi() {
        Set<String> domainStateNames = Arrays.stream(ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessState.values())
                .map(Enum::name)
                .collect(toSet());

        domainStateNames.forEach(ProcessState::valueOf);

        assertEquals(ProcessState.values().length, domainStateNames.size());
    }

    @Test
    void allTaskCardinalitiesMappedInApi() {
        Set<String> domainCardinalityNames = Arrays.stream(ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskCardinality.values())
                .map(Enum::name)
                .collect(toSet());

        domainCardinalityNames.forEach(TaskCardinality::valueOf);

        assertEquals(TaskCardinality.values().length, domainCardinalityNames.size());
    }

    @Test
    void allTaskLifecyclesMappedInApi() {
        Set<String> domainLifecycleNames = Arrays.stream(ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskLifecycle.values())
                .map(Enum::name)
                .collect(toSet());

        domainLifecycleNames.forEach(TaskLifecycle::valueOf);

        assertEquals(TaskLifecycle.values().length, domainLifecycleNames.size());
    }

    @Test
    void allProcessConclusionsMappedInApi() {
        Set<String> processCompletionConclusionNames = Arrays.stream(ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessCompletionConclusion.values())
                .map(Enum::name)
                .collect(toSet());

        processCompletionConclusionNames.forEach(ProcessCompletionConclusion::valueOf);

        assertEquals(TaskLifecycle.values().length, processCompletionConclusionNames.size());
    }

    @Test
    void createProcessContextWithDeletedTasks() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleDynamicTaskInstance();
        TaskInstance deletedTaskInstance = TaskInstance.createUnknownTaskInstance(TaskType.builder().name("deleted-task")
                .cardinality(ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskCardinality.SINGLE_INSTANCE)
                .lifecycle(ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskLifecycle.STATIC).build(), processInstance, ZonedDateTime.now());
        ReflectionTestUtils.setField(deletedTaskInstance, "taskType", Optional.empty());
        final List<TaskInstance> tasks = new ArrayList<>(processInstance.getTasks());
        tasks.add(deletedTaskInstance);
        ReflectionTestUtils.setField(processInstance, "tasks", tasks);

        ProcessContext processContext = ProcessContextFactory.createProcessContext(processInstance);
        assertEquals(processInstance.getOriginProcessId(), processContext.getOriginProcessId());
        assertEquals(processInstance.getProcessTemplate().getName(), processContext.getProcessName());
        assertEquals(processInstance.getState().name(), processContext.getProcessState().name());
        assertEquals(2, processInstance.getTasks().size());
        assertEquals(1, processContext.getTasks().size());
    }

    @Test
    void createProcessContextWithMessageDataWithRole() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleDynamicTaskInstance();
        String templateName = processInstance.getProcessTemplateName();
        Set<String> taskNames =  Set.of("taskId1", "taskId2");
        Message domainMessage = Message.messageBuilder()
                .messageName("event")
                .messageId("eventId")
                .idempotenceId("idempotenceId")
                .originTaskIds(OriginTaskId.from(templateName, taskNames))
                .messageData(Set.of(new MessageData(templateName, "myKey", "myValue", "myRole")))
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .build();
        processInstance.addMessage(domainMessage);

        ProcessContext processContext = ProcessContextFactory.createProcessContext(processInstance);

        assertEquals(2, processContext.getMessages().size());
        ch.admin.bit.jeap.processcontext.plugin.api.context.Message processContextEvent = processContext.getMessages().get(1);

        assertEquals(domainMessage.getMessageName(), processContextEvent.getName());
        assertEquals(taskNames, processContextEvent.getRelatedOriginTaskIds());
        MessageData messageData = domainMessage.getMessageData(templateName).iterator().next();
        assertEquals(1, processContextEvent.getMessageData().size());
        ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData eventDataFromAPI = processContextEvent.getMessageData().iterator().next();
        assertEquals(messageData.getKey(), eventDataFromAPI.getKey());
        assertEquals(messageData.getValue(), eventDataFromAPI.getValue());
        assertEquals(messageData.getRole(), eventDataFromAPI.getRole());
    }

}
