package ch.admin.bit.jeap.processcontext.adapter.restapi.model;

import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessSnapshot;
import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.Task;
import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.User;
import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.UserData;
import ch.admin.bit.jeap.processcontext.domain.TranslateService;
import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.message.MessageData;
import ch.admin.bit.jeap.processcontext.domain.message.MessageReferenceRepository;
import ch.admin.bit.jeap.processcontext.domain.message.MessageRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.*;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskData;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessInstanceDTOFactoryTest {

    private MessageRepository messageRepository;
    private MessageReferenceRepository messageReferenceRepository;
    private TranslateService translateService;
    private ProcessDataRepository processDataRepository;
    private TaskInstanceRepository taskInstanceRepository;

    @BeforeEach
    void setUp() {
        messageRepository = mock(MessageRepository.class);
        messageReferenceRepository = mock(MessageReferenceRepository.class);
        translateService = mock(TranslateService.class);
        processDataRepository = mock(ProcessDataRepository.class);
        taskInstanceRepository = mock(TaskInstanceRepository.class);
        when(translateService.translateUserDataKey(anyString())).thenAnswer(invocation -> createLabels(invocation.getArgument(0)));
        when(translateService.translateUserDataKey(anyString(), anyString())).thenAnswer(invocation -> createLabels(invocation.getArgument(1)));
        when(translateService.translateUserDataKey(anyString(), eq(null))).thenAnswer(invocation -> createLabels(invocation.getArgument(0)));
        when(translateService.translateProcessCompletionName(anyString(), anyString())).thenAnswer(invocation -> createLabels(invocation.getArgument(1)));
        when(translateService.translateProcessTemplateName(anyString())).thenAnswer(invocation -> createLabels(invocation.getArgument(0)));
        when(translateService.translateTaskTypeName(anyString(), anyString())).thenAnswer(invocation -> createLabels(invocation.getArgument(1)));
        when(translateService.translateTaskTypeName(anyString(), anyString(), anyString())).thenAnswer(invocation -> createLabels(invocation.getArgument(1)));
    }

    // java:S5961: number of assertions
    @SuppressWarnings({"OptionalGetWithoutIsPresent", "java:S5961"})
    @Test
    void createFromProcessInstance() {
        String templateName = "template";
        List<ProcessData> processData = List.of(
                new ProcessData("some-name-1", "some-value-1"),
                new ProcessData("some-name-2", "some-value-2")
        );
        List<Message> messages = createMessages(templateName);
        Set<TaskData> taskData = messages.stream().
                map(message -> TaskData.builder().
                        sourceMessage(message.getMessageName()).
                        messageDataKeys(message.getMessageData().stream().map(MessageData::getKey).collect(toSet())).
                        build()).
                collect(toSet());
        ProcessInstanceRepository processInstanceRepository = mock(ProcessInstanceRepository.class);
        when(processInstanceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstanceSavingProcessAndTaskData(templateName, taskData, processData, processInstanceRepository, processDataRepository);
        when(processDataRepository.findByProcessInstanceId(processInstance.getId())).thenReturn(processData);
        ReflectionTestUtils.setField(processInstance, "processCompletion", new ProcessCompletion(
                ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessCompletionConclusion.SUCCEEDED, "all good", ZonedDateTime.now()));
        TaskInstance expectedTask = ProcessInstanceStubs.createPlannedTaskInstance(processInstance);
        UUID plannedByMessageId = messages.getFirst().getId();
        UUID completedByMessageId = messages.get(1).getId();
        ReflectionTestUtils.setField(expectedTask, "plannedBy", plannedByMessageId);
        ReflectionTestUtils.setField(expectedTask, "completedBy", completedByMessageId);

        TimeBasedEpochGenerator uuidGenerator = Generators.timeBasedEpochGenerator();
        when(messageReferenceRepository.findByProcessInstanceIdAndMessageId(processInstance.getId(), plannedByMessageId))
                .thenReturn(toMessageReferenceDTO(uuidGenerator, templateName, messages.getFirst()));
        when(messageReferenceRepository.findByProcessInstanceIdAndMessageId(processInstance.getId(), completedByMessageId))
                .thenReturn(toMessageReferenceDTO(uuidGenerator, templateName, messages.get(1)));

        when(taskInstanceRepository.findByProcessInstanceId(processInstance.getProcessTemplate(), processInstance.getId())).thenReturn(List.of(expectedTask));

        ProcessInstanceDTOFactory processInstanceDTOFactory = createFactory();
        ProcessInstanceDTO processInstanceDTO = processInstanceDTOFactory.createFromProcessInstance(processInstance);

        assertEquals(processInstance.getState().name(), processInstanceDTO.getState());
        assertEquals(processInstance.getOriginProcessId(), processInstanceDTO.getOriginProcessId());
        assertEquals(processInstance.getCreatedAt(), processInstanceDTO.getCreatedAt());
        assertEquals(processInstance.getModifiedAt(), processInstanceDTO.getModifiedAt());
        assertEquals(1, processInstanceDTO.getTasks().size());
        TaskInstanceDTO taskDTO = processInstanceDTO.getTasks().getFirst();
        assertEquals("task", taskDTO.getName().get("de"));
        assertEquals(expectedTask.getState().name(), taskDTO.getState());
        assertEquals(expectedTask.getOriginTaskId(), taskDTO.getOriginTaskId());
        assertEquals(expectedTask.requireTaskType().getCardinality().name(), taskDTO.getCardinality());
        assertEquals(expectedTask.requireTaskType().getCardinality().name(), taskDTO.getCardinality());
        assertThat(taskDTO.getPlannedBy()).containsExactlyInAnyOrder(
                new MessageUserDataDTO("planning-userdata-key1", "planning-userdata-value-1", createLabels("planning-userdata-key1")));
        assertThat(taskDTO.getCompletedBy()).containsExactlyInAnyOrder(
                new MessageUserDataDTO("completing-userdata-key1", "completing-userdata-value-1", createLabels("completing-userdata-key1")),
                new MessageUserDataDTO("completing-userdata-key2", "completing-userdata-value-2", createLabels("completing-userdata-key2")));
        Set<TaskDataDTO> expectedTaskDataDTOs = messages.stream().
                flatMap(message -> message.getMessageData().stream()).
                map(messageData -> TaskDataDTO.builder().
                        key(messageData.getKey()).
                        value(messageData.getValue()).
                        labels(createLabels(messageData.getKey())).
                        build()).
                collect(toSet());
        assertThat(taskDTO.getTaskData()).isEqualTo(expectedTaskDataDTOs);
        ProcessCompletionDTO processCompletion = processInstanceDTO.getProcessCompletion();
        assertNotNull(processCompletion);
        assertEquals(processInstance.getProcessCompletion().get().getConclusion().name(), processCompletion.getConclusion());
        assertEquals("all good", processCompletion.getReason().get("de"));
        assertEquals(processInstance.getProcessCompletion().get().getCompletedAt(), processCompletion.getCompletedAt());
    }

    private ProcessInstanceDTOFactory createFactory() {
        return new ProcessInstanceDTOFactory(translateService, messageRepository, messageReferenceRepository, taskInstanceRepository);
    }

    @Test
    void createEvents_sortByCreatedAtThenName() {
        // This message is older but it was received now
        ZonedDateTime olderMessageReceivedTimestamp = ZonedDateTime.now();
        ZonedDateTime olderMessageCreatedTimestamp = olderMessageReceivedTimestamp.minusDays(1);

        // This message is newer, but it was received before the previous message
        ZonedDateTime newerMessageReceivedTimestamp = olderMessageReceivedTimestamp.minusMinutes(1);
        ZonedDateTime newerMessageCreatedTimestamp = newerMessageReceivedTimestamp.minusSeconds(10);
        List<MessageReferenceMessageDTO> eventReferences = List.of(
                createEventReferenceEventDTO(newerMessageReceivedTimestamp, newerMessageCreatedTimestamp, "newerB"),
                createEventReferenceEventDTO(newerMessageReceivedTimestamp, newerMessageCreatedTimestamp, "newerA"),
                createEventReferenceEventDTO(olderMessageReceivedTimestamp, olderMessageCreatedTimestamp, "olderB"),
                createEventReferenceEventDTO(olderMessageReceivedTimestamp, olderMessageCreatedTimestamp, "olderA")
        );
        List<MessageDTO> messageDTOS = ProcessInstanceDTOFactory.createMessages(eventReferences);

        assertEquals("newerA", messageDTOS.getFirst().getName());
        assertEquals("newerB", messageDTOS.get(1).getName());
        assertEquals("olderA", messageDTOS.get(2).getName());
        assertEquals("olderB", messageDTOS.get(3).getName());
    }

    @SuppressWarnings("SameParameterValue")
    private List<Message> createMessages(String templateName) {
        Message planningMessage = Message.messageBuilder()
                .messageName("planningEvent")
                .messageId("planningEvent-id")
                .idempotenceId("planningEvent-idempotenceId")
                .messageData(Set.of(
                        new MessageData(templateName, "planningEvent-message-data-key-1", "planningEvent-message-data-value-1")))
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .build();
        List<String[]> planningEventUserData = new ArrayList<>();
        planningEventUserData.add(new String[]{"planning-userdata-key1", "planning-userdata-value-1"});
        when(messageRepository.findMessageUserDataByMessageId(planningMessage.getId())).thenReturn(planningEventUserData);

        Message completingMessage = Message.messageBuilder()
                .messageName("completingEvent")
                .messageId("completingEvent-id")
                .idempotenceId("completingEvent-idempotenceId")
                .messageData(Set.of(
                        new MessageData(templateName, "completingEvent-message-data-key-1", "completingEvent-message-data-value-1")))
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .build();
        List<String[]> completingEventUserData = new ArrayList<>();
        completingEventUserData.add(new String[]{"completing-userdata-key1", "completing-userdata-value-1"});
        completingEventUserData.add(new String[]{"completing-userdata-key2", "completing-userdata-value-2"});
        when(messageRepository.findMessageUserDataByMessageId(completingMessage.getId())).thenReturn(completingEventUserData);

        return List.of(planningMessage, completingMessage);
    }

    private MessageReferenceMessageDTO createEventReferenceEventDTO(ZonedDateTime receivedAt, ZonedDateTime createdAt, String eventName) {
        return MessageReferenceMessageDTO.builder()
                .messageReferenceId(Generators.timeBasedEpochGenerator().generate())
                .messageId(Generators.timeBasedEpochGenerator().generate())
                .messageReceivedAt(receivedAt)
                .messageCreatedAt(createdAt)
                .messageName(eventName)
                .messageData(emptyList())
                .relatedOriginTaskIds(emptySet())
                .build();
    }

    @Test
    void createTasks_sortByIndexThenById() {
        List<TaskInstance> tasks = List.of(
                createTaskInstance("taskC", 2, "idB"),
                createTaskInstance("taskC", 2, "idA"),
                createTaskInstance("taskD", 3, null),
                createTaskInstance("taskD", 3, null),
                createTaskInstance("taskA", 0, "idA"),
                createTaskInstance("taskB", 1, null)
        );
        List<TaskInstanceDTO> taskDTOs = createFactory().createTasks(tasks, "process", translateService, messageRepository);

        assertEquals("taskA", taskDTOs.getFirst().getName().get("de"));
        assertEquals("taskB", taskDTOs.get(1).getName().get("de"));
        assertEquals("taskC", taskDTOs.get(2).getName().get("de"));
        assertEquals("idA", taskDTOs.get(2).getOriginTaskId());
        assertEquals("taskC", taskDTOs.get(3).getName().get("de"));
        assertEquals("idB", taskDTOs.get(3).getOriginTaskId());
        assertEquals("taskD", taskDTOs.get(4).getName().get("de"));
    }

    private TaskInstance createTaskInstance(String name, int index, String originTaskId) {
        return ProcessInstanceStubs.createTaskInstance(name, index, originTaskId);
    }

    @Test
    void createFromSnapshot() {
        Instant modified = Instant.ofEpochSecond(0);
        Instant created = Instant.ofEpochSecond(1);
        Instant snapCreated = Instant.ofEpochSecond(1);
        ProcessSnapshot snapshot = ProcessSnapshot.newBuilder()
                .setTemplateName("templateName")
                .setTemplateLabel("templateLabel")
                .setOriginProcessId("originId")
                .setState("STATE")
                .setTasks(List.of(Task.newBuilder()
                        .setState("IN_PROGRESS")
                        .setDateTimeCreated(Instant.now())
                        .setDateTimePlanned(Instant.now())
                        .setTaskType("taskType")
                        .setTaskTypeLabel("taskTypeLabel")
                        .setPlannedBy(User.newBuilder().
                                setUserData(List.of(
                                        UserData.newBuilder().
                                                setKey("planning-user-key").
                                                setValue("planning-user-value").
                                                setLabel("planning-user-key-translation").
                                                build())).
                                build())
                        .setCompletedBy(User.newBuilder().
                                setUserData(List.of(
                                        UserData.newBuilder().
                                                setKey("completing-user-key").
                                                setValue("completing-user-value").
                                                setLabel(null).
                                                build())).
                                build())
                        .setTaskData(List.of(
                                ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.TaskData.newBuilder().
                                        setKey("task-data-key-1").
                                        setValue("task-data-value-1").
                                        setLabel("task-data-translation-1").
                                        build(),
                                ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.TaskData.newBuilder().
                                        setKey("task-data-key-2").
                                        setValue("task-data-value-2").
                                        setLabel(null).
                                        build()))
                        .build()))
                .setProcessData(List.of(ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessData.newBuilder()
                        .setKey("key")
                        .setValue("value")
                        .build()))
                .setDateTimeCreated(created)
                .setDateTimeModified(modified)
                .setSnapshotDateTimeCreated(snapCreated)
                .build();

        ProcessInstanceDTO dto = ProcessInstanceDTOFactory.createFromSnapshot(snapshot, translateService);

        assertThat(dto.isSnapshot())
                .isTrue();
        assertThat(dto.getState())
                .isEqualTo("STATE");
        assertThat(dto.getOriginProcessId())
                .isEqualTo("originId");
        assertThat(dto.getOriginProcessId())
                .isEqualTo("originId");
        assertThat(dto.getName()).isEqualTo(Map.of("de", "templateName"));
        assertThat(dto.getName()).containsEntry("de", "templateName");
        assertThat(dto.getCreatedAt())
                .isEqualTo(ZonedDateTime.ofInstant(created, ZoneId.systemDefault()));
        assertThat(dto.getModifiedAt())
                .isEqualTo(ZonedDateTime.ofInstant(modified, ZoneId.systemDefault()));
        assertThat(dto.getSnapshotCreatedAt())
                .isEqualTo(ZonedDateTime.ofInstant(snapCreated, ZoneId.systemDefault()));
        assertThat(dto.getTasks()).hasSize(1);
        TaskInstanceDTO task = dto.getTasks().getFirst();
        assertThat(task.getName()).isEqualTo(Map.of(
                "de", "taskType"));
        assertThat(task.getPlannedBy()).isEqualTo(Set.of(
                MessageUserDataDTO.builder().
                        key("planning-user-key").
                        value("planning-user-value").
                        label(Map.of(
                                "de", "planning-user-key-translation")).
                        build()));
        assertThat(task.getCompletedBy()).isEqualTo(Set.of(
                MessageUserDataDTO.builder().
                        key("completing-user-key").
                        value("completing-user-value").
                        label(Map.of(
                                "de", "completing-user-key")).
                        build()));
        assertThat(task.getTaskData()).containsExactly(
                TaskDataDTO.builder().
                        key("task-data-key-1").
                        value("task-data-value-1").
                        labels(Map.of(
                                "de", "task-data-translation-1")).
                        build(),
                TaskDataDTO.builder().
                        key("task-data-key-2").
                        value("task-data-value-2").
                        labels(Map.of(
                                "de", "task-data-key-2")).
                        build()
        );
    }

    private Map<String, String> createLabels(String value) {
        return Map.of("de", value);
    }

    private MessageReferenceMessageDTO toMessageReferenceDTO(TimeBasedEpochGenerator uuidGenerator, String templateName, Message message) {
        return MessageReferenceMessageDTO.builder()
                .messageReferenceId(uuidGenerator.generate())
                .messageId(message.getId())
                .messageName(message.getMessageName())
                .messageCreatedAt(message.getMessageCreatedAt())
                .messageReceivedAt(message.getReceivedAt())
                .messageData(message.getMessageData(templateName).stream()
                        .map(md -> MessageReferenceMessageDataDTO.builder()
                                .messageDataKey(md.getKey())
                                .messageDataValue(md.getValue())
                                .messageDataRole(md.getRole())
                                .build())
                        .toList())
                .relatedOriginTaskIds(message.getOriginTaskIds(templateName).stream()
                        .map(ch.admin.bit.jeap.processcontext.domain.message.OriginTaskId::getOriginTaskId)
                        .collect(toSet()))
                .build();
    }
}
