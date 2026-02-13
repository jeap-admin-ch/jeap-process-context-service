package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.message.MessageData;
import ch.admin.bit.jeap.processcontext.domain.message.OriginTaskId;
import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextFactory;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskCardinality;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskLifecycle;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskType;
import com.fasterxml.uuid.Generators;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final String ORIGIN_PROCESS_ID = "originProcessId";
    private static final String TEMPLATE_NAME = "templateName";
    private static final String DOMAIN_EVENT_NAME = "myDomainEvent";

    @Mock
    private ProcessInstanceRepository processInstanceRepository;
    @Mock
    private TaskInstanceRepository taskInstanceRepository;

    private TaskService target;
    private ProcessContextFactory processContextFactory;

    @BeforeEach
    void setUp() {
        processContextFactory = new ProcessContextFactory(new ProcessContextRepositoryFacadeStub());
        target = new TaskService(processInstanceRepository, taskInstanceRepository);
    }

    @Test
    void planDomainEventTasks_singleInstanceTask_plansTask() {
        TaskType dynamicTaskType = TaskType.builder()
                .name("dynamicTask")
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .lifecycle(TaskLifecycle.DYNAMIC)
                .plannedByDomainEvent(DOMAIN_EVENT_NAME)
                .build();
        ProcessTemplate processTemplate = createProcessTemplate(List.of(dynamicTaskType));
        ProcessInstance processInstance = createProcessInstance(processTemplate);
        Message message = createMessage();
        MessageReferenceMessageDTO msgRef = createMessageReferenceDTO(message);

        when(taskInstanceRepository.save(any(TaskInstance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<TaskInstance> result = target.planDomainEventTasks(processInstance, msgRef, message);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTaskTypeName()).isEqualTo("dynamicTask");
    }

    @Test
    void planDomainEventTasks_multiInstanceTask_plansTaskForEachOriginTaskId() {
        TaskType multiTaskType = TaskType.builder()
                .name("multiTask")
                .cardinality(TaskCardinality.MULTI_INSTANCE)
                .lifecycle(TaskLifecycle.DYNAMIC)
                .plannedByDomainEvent(DOMAIN_EVENT_NAME)
                .build();
        ProcessTemplate processTemplate = createProcessTemplate(List.of(multiTaskType));
        ProcessInstance processInstance = createProcessInstance(processTemplate);
        Message message = createMessageWithOriginTaskIds(Set.of("taskId1", "taskId2"));
        MessageReferenceMessageDTO msgRef = createMessageReferenceDTO(message);

        when(taskInstanceRepository.save(any(TaskInstance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<TaskInstance> result = target.planDomainEventTasks(processInstance, msgRef, message);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(TaskInstance::getOriginTaskId)
                .containsExactlyInAnyOrder("taskId1", "taskId2");
    }

    @Test
    void planDomainEventTasks_instantiationConditionFalse_doesNotPlanTask() {
        TaskType conditionalTask = TaskType.builder()
                .name("conditionalTask")
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .lifecycle(TaskLifecycle.DYNAMIC)
                .plannedByDomainEvent(DOMAIN_EVENT_NAME)
                .instantiationCondition(msg -> false)
                .build();
        ProcessTemplate processTemplate = createProcessTemplate(List.of(conditionalTask));
        ProcessInstance processInstance = createProcessInstance(processTemplate);
        Message message = createMessage();
        MessageReferenceMessageDTO msgRef = createMessageReferenceDTO(message);

        List<TaskInstance> result = target.planDomainEventTasks(processInstance, msgRef, message);

        assertThat(result).isEmpty();
        verify(taskInstanceRepository, never()).save(any());
    }

    @Test
    void planDomainEventTasks_nonMatchingDomainEvent_doesNotPlanTask() {
        TaskType dynamicTaskType = TaskType.builder()
                .name("dynamicTask")
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .lifecycle(TaskLifecycle.DYNAMIC)
                .plannedByDomainEvent("otherEvent")
                .build();
        ProcessTemplate processTemplate = createProcessTemplate(List.of(dynamicTaskType));
        ProcessInstance processInstance = createProcessInstance(processTemplate);
        Message message = createMessage();
        MessageReferenceMessageDTO msgRef = createMessageReferenceDTO(message);

        List<TaskInstance> result = target.planDomainEventTasks(processInstance, msgRef, message);

        assertThat(result).isEmpty();
    }

    @Test
    void planDomainEventTasks_singleInstanceWithMultipleOriginTaskIds_throws() {
        TaskType singleInstanceTask = TaskType.builder()
                .name("singleTask")
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .lifecycle(TaskLifecycle.DYNAMIC)
                .plannedByDomainEvent(DOMAIN_EVENT_NAME)
                .build();
        ProcessTemplate processTemplate = createProcessTemplate(List.of(singleInstanceTask));
        ProcessInstance processInstance = createProcessInstance(processTemplate);
        Message message = createMessageWithOriginTaskIds(Set.of("taskId1", "taskId2"));
        MessageReferenceMessageDTO msgRef = createMessageReferenceDTO(message);

        assertThatThrownBy(() -> target.planDomainEventTasks(processInstance, msgRef, message))
                .isInstanceOf(TaskPlanningException.class);
    }

    @Test
    void completeObservationTasks_matchingObservedTask_createsTask() {
        TaskType observedTaskType = TaskType.builder()
                .name("observedTask")
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .lifecycle(TaskLifecycle.OBSERVED)
                .observesMessage(DOMAIN_EVENT_NAME)
                .build();
        ProcessTemplate processTemplate = createProcessTemplate(List.of(observedTaskType));
        ProcessInstance processInstance = createProcessInstance(processTemplate);
        Message message = createMessage();
        MessageReferenceMessageDTO msgRef = createMessageReferenceDTO(message);

        when(taskInstanceRepository.save(any(TaskInstance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        target.completeObservationTasks(processInstance, msgRef, message);

        verify(taskInstanceRepository).save(argThat(task ->
                task.getTaskTypeName().equals("observedTask") && task.getState() == TaskState.COMPLETED));
    }

    @Test
    void completeObservationTasks_instantiationConditionFalse_doesNotCreateTask() {
        TaskType observedTaskType = TaskType.builder()
                .name("conditionalObservedTask")
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .lifecycle(TaskLifecycle.OBSERVED)
                .observesMessage(DOMAIN_EVENT_NAME)
                .instantiationCondition(msg -> false)
                .build();
        ProcessTemplate processTemplate = createProcessTemplate(List.of(observedTaskType));
        ProcessInstance processInstance = createProcessInstance(processTemplate);
        Message message = createMessage();
        MessageReferenceMessageDTO msgRef = createMessageReferenceDTO(message);

        target.completeObservationTasks(processInstance, msgRef, message);

        verify(taskInstanceRepository, never()).save(any());
    }

    @Test
    void completeObservationTasks_nonObservedLifecycle_doesNotCreateTask() {
        TaskType dynamicTask = TaskType.builder()
                .name("dynamicTask")
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .lifecycle(TaskLifecycle.DYNAMIC)
                .plannedByDomainEvent(DOMAIN_EVENT_NAME)
                .build();
        ProcessTemplate processTemplate = createProcessTemplate(List.of(dynamicTask));
        ProcessInstance processInstance = createProcessInstance(processTemplate);
        Message message = createMessage();
        MessageReferenceMessageDTO msgRef = createMessageReferenceDTO(message);

        target.completeObservationTasks(processInstance, msgRef, message);

        verify(taskInstanceRepository, never()).save(any());
    }

    @Test
    void evaluateCompletedTasks_matchingTaskType_evaluatesOpenTasks() {
        TaskType staticTask = TaskType.builder()
                .name("staticTask")
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .lifecycle(TaskLifecycle.STATIC)
                .completedByDomainEvent(DOMAIN_EVENT_NAME)
                .build();
        ProcessTemplate processTemplate = createProcessTemplate(List.of(staticTask));
        ProcessInstance processInstance = createProcessInstance(processTemplate);

        TaskInstance openTask = TaskInstance.createInitialTaskInstance(staticTask, processInstance, ZonedDateTime.now());
        when(taskInstanceRepository.getTaskInstancesInNonFinalStateOfTypes(
                processInstance.getProcessTemplate(), processInstance.getId(), Set.of("staticTask")))
                .thenReturn(List.of(openTask));

        UUID messageReferenceId = UUID.randomUUID();
        MessageReferenceMessageDTO msgRef = MessageReferenceMessageDTO.builder()
                .messageReferenceId(messageReferenceId)
                .messageId(UUID.randomUUID())
                .messageName(DOMAIN_EVENT_NAME)
                .messageReceivedAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .messageData(List.of())
                .relatedOriginTaskIds(Set.of())
                .build();

        target.evaluateCompletedTasks(processInstance, msgRef);

        verify(taskInstanceRepository).getTaskInstancesInNonFinalStateOfTypes(
                processInstance.getProcessTemplate(), processInstance.getId(), Set.of("staticTask"));
        assertThat(openTask.getState()).isEqualTo(TaskState.COMPLETED);
    }

    @Test
    void evaluateCompletedTasks_noMatchingTaskType_skips() {
        TaskType staticTask = TaskType.builder()
                .name("staticTask")
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .lifecycle(TaskLifecycle.STATIC)
                .completedByDomainEvent("otherEvent")
                .build();
        ProcessTemplate processTemplate = createProcessTemplate(List.of(staticTask));
        ProcessInstance processInstance = createProcessInstance(processTemplate);

        MessageReferenceMessageDTO msgRef = MessageReferenceMessageDTO.builder()
                .messageReferenceId(UUID.randomUUID())
                .messageId(UUID.randomUUID())
                .messageName(DOMAIN_EVENT_NAME)
                .messageReceivedAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .messageData(List.of())
                .relatedOriginTaskIds(Set.of())
                .build();

        target.evaluateCompletedTasks(processInstance, msgRef);

        verify(taskInstanceRepository, never()).getTaskInstancesInNonFinalStateOfTypes(any(), any(), any());
    }

    @Test
    void evaluatePlannedTasksCompletedByExistingMessages_emptyList_returnsEarly() {
        target.evaluatePlannedTasksCompletedByExistingMessages(List.of());

        verify(processInstanceRepository, never()).findLatestMessageReferenceByMessageType(any(), any());
        verify(taskInstanceRepository, never()).flush();
    }

    @Test
    void evaluatePlannedTasksCompletedByExistingMessages_taskWaitingForMessage_completesIfMessageExists() {
        TaskType dynamicTaskType = TaskType.builder()
                .name("dynamicTask")
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .lifecycle(TaskLifecycle.DYNAMIC)
                .plannedByDomainEvent("planEvent")
                .completedByDomainEvent(DOMAIN_EVENT_NAME)
                .build();
        ProcessTemplate processTemplate = createProcessTemplate(List.of(dynamicTaskType));
        ProcessInstance processInstance = createProcessInstance(processTemplate);

        TaskInstance plannedTask = TaskInstance.createInitialTaskInstance(dynamicTaskType, processInstance, ZonedDateTime.now());

        ZonedDateTime completedAt = ZonedDateTime.now();
        UUID completedByMessageId = UUID.randomUUID();
        MessageReference existingMsgRef = mock(MessageReference.class);
        when(existingMsgRef.getMessageId()).thenReturn(completedByMessageId);
        when(existingMsgRef.getCreatedAt()).thenReturn(completedAt);
        when(processInstanceRepository.findLatestMessageReferenceByMessageType(processInstance, DOMAIN_EVENT_NAME))
                .thenReturn(Optional.of(existingMsgRef));

        target.evaluatePlannedTasksCompletedByExistingMessages(List.of(plannedTask));

        assertThat(plannedTask.getState()).isEqualTo(TaskState.COMPLETED);
        assertThat(plannedTask.getCompletedBy()).isEqualTo(completedByMessageId);
    }

    private ProcessTemplate createProcessTemplate(List<TaskType> taskTypes) {
        return ProcessTemplate.builder()
                .name(TEMPLATE_NAME)
                .templateHash("hash")
                .taskTypes(taskTypes)
                .processRelationPatterns(List.of())
                .build();
    }

    private ProcessInstance createProcessInstance(ProcessTemplate processTemplate) {
        return ProcessInstance.createProcessInstance(ORIGIN_PROCESS_ID, processTemplate, processContextFactory);
    }

    private Message createMessage() {
        return Message.messageBuilder()
                .messageId(Generators.timeBasedEpochGenerator().generate().toString())
                .idempotenceId(Generators.timeBasedEpochGenerator().generate().toString())
                .messageName(DOMAIN_EVENT_NAME)
                .messageCreatedAt(ZonedDateTime.now())
                .createdAt(ZonedDateTime.now())
                .messageData(Set.of(MessageData.builder()
                        .key("dataKey")
                        .value("dataValue")
                        .templateName(TEMPLATE_NAME)
                        .build()))
                .build();
    }

    private Message createMessageWithOriginTaskIds(Set<String> originTaskIds) {
        return Message.messageBuilder()
                .messageId(Generators.timeBasedEpochGenerator().generate().toString())
                .idempotenceId(Generators.timeBasedEpochGenerator().generate().toString())
                .messageName(DOMAIN_EVENT_NAME)
                .messageCreatedAt(ZonedDateTime.now())
                .createdAt(ZonedDateTime.now())
                .originTaskIds(OriginTaskId.from(TEMPLATE_NAME, originTaskIds))
                .messageData(Set.of(MessageData.builder()
                        .key("dataKey")
                        .value("dataValue")
                        .templateName(TEMPLATE_NAME)
                        .build()))
                .build();
    }

    private MessageReferenceMessageDTO createMessageReferenceDTO(Message message) {
        return MessageReferenceMessageDTO.of(TEMPLATE_NAME, UUID.randomUUID(), message);
    }
}
