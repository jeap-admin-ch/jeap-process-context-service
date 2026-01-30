package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.message.MessageData;
import ch.admin.bit.jeap.processcontext.domain.message.OriginTaskId;
import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextRepositoryFacade;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskType;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessCompletionConclusion;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.*;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class ProcessContextFactoryTest {

    @Test
    void createProcessContext() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleDynamicTaskInstance();
        String templateName = processInstance.getProcessTemplateName();
        Set<String> taskNames = Set.of("taskId1", "taskId2");
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

        ProcessContextFactory processContextFactory = new ProcessContextFactory(mock(ProcessContextRepositoryFacade.class));
        ProcessContext processContext = processContextFactory.createProcessContext(processInstance);

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
    void allProcessConclusionsMappedInApi() {
        Set<String> processCompletionConclusionNames = Arrays.stream(ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessCompletionConclusion.values())
                .map(Enum::name)
                .collect(toSet());

        processCompletionConclusionNames.forEach(ProcessCompletionConclusion::valueOf);
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

        ProcessContextFactory processContextFactory = new ProcessContextFactory(mock(ProcessContextRepositoryFacade.class));

        ProcessContext processContext = processContextFactory.createProcessContext(processInstance);
        assertEquals(processInstance.getOriginProcessId(), processContext.getOriginProcessId());
        assertEquals(processInstance.getProcessTemplate().getName(), processContext.getProcessName());
        assertEquals(processInstance.getState().name(), processContext.getProcessState().name());
        assertEquals(2, processInstance.getTasks().size());
    }

    @Test
    void createProcessContextWithMessageDataWithRole() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleDynamicTaskInstance();
        String templateName = processInstance.getProcessTemplateName();
        Set<String> taskNames = Set.of("taskId1", "taskId2");
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
        ProcessContextFactory processContextFactory = new ProcessContextFactory(mock(ProcessContextRepositoryFacade.class));

        ProcessContext processContext = processContextFactory.createProcessContext(processInstance);

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
