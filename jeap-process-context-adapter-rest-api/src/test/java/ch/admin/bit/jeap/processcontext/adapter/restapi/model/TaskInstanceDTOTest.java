package ch.admin.bit.jeap.processcontext.adapter.restapi.model;

import ch.admin.bit.jeap.processcontext.domain.TranslateService;
import ch.admin.bit.jeap.processcontext.domain.message.MessageRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.*;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskCardinality;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskData;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskLifecycle;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskInstanceDTOTest {

    private MessageRepository messageRepository;

    @BeforeEach
    void setUp() {
        messageRepository = mock(MessageRepository.class);
    }

    @Test
    void create() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        TaskInstance taskInstance = processInstance.getTasks().getFirst();

        TranslateService translateService = mock(TranslateService.class);
        when(translateService.translateProcessTemplateName(anyString())).thenAnswer(invocation -> createLabels(invocation.getArgument(1)));
        when(translateService.translateTaskTypeName(anyString(), anyString())).thenAnswer(invocation -> createLabels(invocation.getArgument(1)));
        when(translateService.translateTaskTypeName(anyString(), anyString())).thenAnswer(invocation -> createLabels(invocation.getArgument(1)));
        TaskInstanceDTO taskInstanceDTO = TaskInstanceDTO.create(taskInstance, List.of(), "process", translateService, messageRepository);
        assertNull(taskInstanceDTO.getOriginTaskId());
        assertEquals("task", taskInstanceDTO.getName().get("de"));
        assertEquals("STATIC", taskInstanceDTO.getLifecycle());
        assertEquals("SINGLE_INSTANCE", taskInstanceDTO.getCardinality());
        assertEquals("PLANNED", taskInstanceDTO.getState());
    }

    @Test
    void createWithoutTaskType() {
        TaskInstance taskInstance = mock(TaskInstance.class);
        when(taskInstance.getTaskType()).thenReturn(Optional.empty());
        when(taskInstance.getOriginTaskId()).thenReturn("1");
        when(taskInstance.getTaskTypeName()).thenReturn("taskTypeName");
        when(taskInstance.getState()).thenReturn(TaskState.DELETED);

        TranslateService translateService = mock(TranslateService.class);
        when(translateService.translateTaskTypeName(anyString(), anyString())).thenAnswer(invocation -> createLabels(invocation.getArgument(1)));

        TaskInstanceDTO taskInstanceDTO = TaskInstanceDTO.create(taskInstance, List.of(), "process", translateService, messageRepository);
        when(translateService.translateProcessTemplateName(anyString())).thenAnswer(invocation -> createLabels(invocation.getArgument(1)));
        when(translateService.translateTaskTypeName(anyString(), anyString())).thenAnswer(invocation -> createLabels(invocation.getArgument(1)));

        assertEquals("1", taskInstanceDTO.getOriginTaskId());
        assertEquals("taskTypeName", taskInstanceDTO.getName().get("de"));
        assertEquals("UNKNOWN", taskInstanceDTO.getLifecycle());
        assertEquals("UNKNOWN", taskInstanceDTO.getCardinality());
        assertEquals("DELETED", taskInstanceDTO.getState());
        assertTrue(taskInstanceDTO.getTaskData().isEmpty());
    }

    @Test
    void testGetTaskDataDTOs() {
        // Setting up three messages with message data:
        // "other" message does not plan or complete the test task
        // "planning" message plans the test task and contains message data that is referenced as task data
        // "completing" message completes the test task and contains message data that is referenced as task data
        final UUID otherMessageId = UUID.randomUUID();
        final UUID plannedByMessageId = UUID.randomUUID();
        final UUID completedByMessageId = UUID.randomUUID();
        final String otherMessageName = "other";
        final String planningMessageName = "planning";
        final String completingMessageName = "completing";
        final String otherKey = "other-key";
        final String planningKey = "planning-key";
        final String completingKey = "completing-key";
        final String planningValue = "planning-value";
        final String completingValue = "completing-value";
        final MessageReferenceMessageDataDTO otherMessageData = MessageReferenceMessageDataDTO.builder().
                messageDataKey(otherKey).
                messageDataValue("other-value").
                build();
        final MessageReferenceMessageDataDTO planningMessageData = MessageReferenceMessageDataDTO.builder().
                messageDataKey(planningKey).
                messageDataValue(planningValue).
                build();
        final MessageReferenceMessageDataDTO completingMessageData = MessageReferenceMessageDataDTO.builder().
                messageDataKey(completingKey).
                messageDataValue(completingValue).
                build();
        final MessageDTO otherMessageDTO = createMessageDto(otherMessageId, otherMessageName, Set.of(otherMessageData, completingMessageData, planningMessageData));
        final MessageDTO planningMessageDTO = createMessageDto(plannedByMessageId, planningMessageName, Set.of(planningMessageData));
        final MessageDTO completingMessageDTO = createMessageDto(completedByMessageId, completingMessageName, Set.of(completingMessageData));
        final TaskData planningTaskData = TaskData.builder().
                sourceMessage(planningMessageName).
                messageDataKeys(Set.of(planningKey)).
                build();
        final TaskData completingTaskData = TaskData.builder().
                sourceMessage(completingMessageName).
                messageDataKeys(Set.of(completingKey)).
                build();
        final String taskTypeName = "test-task-type";
        final TaskType taskType = TaskType.builder().
                name(taskTypeName).
                lifecycle(TaskLifecycle.DYNAMIC).
                cardinality(TaskCardinality.SINGLE_INSTANCE).
                plannedByDomainEvent(planningMessageName).
                completedByDomainEvent(completingMessageName).
                taskData(Set.of(planningTaskData, completingTaskData)).
                build();
        final TaskInstance taskInstance = mock(TaskInstance.class);
        when(taskInstance.getPlannedBy()).thenReturn(plannedByMessageId);
        when(taskInstance.getCompletedBy()).thenReturn(completedByMessageId);
        when(taskInstance.getTaskType()).thenReturn(Optional.of(taskType));
        final String processTemplateName = "test-process-template";
        final TranslateService translateService = mock(TranslateService.class);
        final Map<String, String> planningLabels = Map.of("de", "planning");
        final Map<String, String> completingLabels = Map.of("de", "completing");
        when(translateService.translateTaskDataKey(processTemplateName, taskTypeName, planningKey)).
                thenReturn(planningLabels);
        when(translateService.translateTaskDataKey(processTemplateName, taskTypeName, completingKey)).
                thenReturn(completingLabels);

        // no messages received yet -> no task data
        Set<TaskDataDTO> taskDataDTONoMessages = TaskInstanceDTO.getTaskDataDTOs(
                taskInstance, List.of(), processTemplateName, translateService);
        assertThat(taskDataDTONoMessages).isEmpty();

        // only other message received yet -> no task data
        Set<TaskDataDTO>  taskDataDTOOtherMessage = TaskInstanceDTO.getTaskDataDTOs(
                taskInstance, List.of(otherMessageDTO), processTemplateName, translateService);
        assertThat(taskDataDTOOtherMessage).isEmpty();

        // only other and planning messages received yet -> only planning task data present
        Set<TaskDataDTO> taskDataDTOOtherAndPlanningMessages = TaskInstanceDTO.getTaskDataDTOs(
                taskInstance, List.of(otherMessageDTO, planningMessageDTO), processTemplateName, translateService);
        final TaskDataDTO expectedPlanningTaskDataDTO = TaskDataDTO.builder().
                key(planningKey).
                value(planningValue).
                labels(planningLabels).
                build();
        assertThat(taskDataDTOOtherAndPlanningMessages).containsOnly(expectedPlanningTaskDataDTO);

        // only other and completing messages received yet -> only completing task data present
        Set<TaskDataDTO> taskDataDTOOtherAndCompletingMessages = TaskInstanceDTO.getTaskDataDTOs(
                taskInstance, List.of(otherMessageDTO, completingMessageDTO), processTemplateName, translateService);
        final TaskDataDTO expectedCompletingTaskDataDTO = TaskDataDTO.builder().
                key(completingKey).
                value(completingValue).
                labels(completingLabels).
                build();
        assertThat(taskDataDTOOtherAndCompletingMessages).containsOnly(expectedCompletingTaskDataDTO);

        // other, completing and planning messages received -> planning and completing task data present
        Set<TaskDataDTO>  taskDataDTOOtherAndPlanningAndCommpletingMessages = TaskInstanceDTO.getTaskDataDTOs(
                taskInstance, List.of(otherMessageDTO, planningMessageDTO, completingMessageDTO), processTemplateName, translateService);
        assertThat(taskDataDTOOtherAndPlanningAndCommpletingMessages).
                containsOnly(expectedPlanningTaskDataDTO, expectedCompletingTaskDataDTO);

        // if task type is unknown -> no task data
        when(taskInstance.getTaskType()).thenReturn(Optional.empty());
        Set<TaskDataDTO>  taskDataDTONoTaskType = TaskInstanceDTO.getTaskDataDTOs(
                taskInstance, List.of(otherMessageDTO, planningMessageDTO, completingMessageDTO), processTemplateName, translateService);
        assertThat(taskDataDTONoTaskType).isEmpty();
    }

    private MessageDTO createMessageDto(UUID messageId, String messageName, Set<MessageReferenceMessageDataDTO> messageData) {
        MessageReferenceMessageDTO messageReferenceMessageDTO = MessageReferenceMessageDTO.builder().
                messageReferenceId(UUID.randomUUID()).
                messageId(messageId).
                messageName(messageName).
                messageReceivedAt(ZonedDateTime.now()).
                messageData(messageData).
                relatedOriginTaskIds(Set.of()).
                build();
        return MessageDTO.create(messageReferenceMessageDTO);
    }

    private Map<String, String> createLabels(String value) {
        return Map.of("de", value);
    }
}
