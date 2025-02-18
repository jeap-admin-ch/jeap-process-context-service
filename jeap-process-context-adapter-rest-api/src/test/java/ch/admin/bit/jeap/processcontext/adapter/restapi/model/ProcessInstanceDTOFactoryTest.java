package ch.admin.bit.jeap.processcontext.adapter.restapi.model;

import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessSnapshot;
import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.Task;
import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.User;
import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.UserData;
import ch.admin.bit.jeap.processcontext.domain.TranslateService;
import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.message.MessageData;
import ch.admin.bit.jeap.processcontext.domain.message.MessageRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.*;
import ch.admin.bit.jeap.processcontext.domain.processrelation.ProcessRelationsService;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskData;
import com.fasterxml.uuid.Generators;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessInstanceDTOFactoryTest {

    private MessageRepository messageRepository;
    private TranslateService translateService;

    @BeforeEach
    void setUp() {
        messageRepository = mock(MessageRepository.class);
        translateService = mock(TranslateService.class);
        when(translateService.translateUserDataKey(anyString())).thenAnswer(invocation -> createLabels(invocation.getArgument(0)));
        when(translateService.translateUserDataKey(anyString(), anyString())).thenAnswer(invocation -> createLabels(invocation.getArgument(1)));
        when(translateService.translateUserDataKey(anyString(), eq(null))).thenAnswer(invocation -> createLabels(invocation.getArgument(0)));
        when(translateService.translateProcessCompletionName(anyString(), anyString())).thenAnswer(invocation -> createLabels(invocation.getArgument(1)));
        when(translateService.translateProcessTemplateName(anyString())).thenAnswer(invocation -> createLabels(invocation.getArgument(0)));
        when(translateService.translateTaskTypeName(anyString(), anyString())).thenAnswer(invocation -> createLabels(invocation.getArgument(1)));
        when(translateService.translateTaskTypeName(anyString(), anyString(), anyString())).thenAnswer(invocation -> createLabels(invocation.getArgument(1)));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void createFromProcessInstance() {
        final String templateName = "template";
        Set<ProcessData> processData = Set.of(
                new ProcessData("some-name-1", "some-value-1"),
                new ProcessData("some-name-2", "some-value-2")
        );
        final List<Message> messages = createMessages(templateName);
        Set<TaskData> taskData = messages.stream().
                map(message -> TaskData.builder().
                        sourceMessage(message.getMessageName()).
                        messageDataKeys(message.getMessageData().stream().map(MessageData::getKey).collect(Collectors.toSet())).
                        build()).
                collect(Collectors.toSet());
        ProcessInstance processInstance = ProcessInstanceStubs.
                createProcessWithSingleTaskInstanceAndReachedMilestoneAndEventWithAdditionalMessages(templateName, taskData, processData, messages);
        ReflectionTestUtils.setField(processInstance, "processCompletion", new ProcessCompletion(
                ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessCompletionConclusion.SUCCEEDED, "all good", ZonedDateTime.now()));
        TaskInstance expectedTask = processInstance.getTasks().getFirst();
        ReflectionTestUtils.setField(expectedTask, "plannedBy",  messages.get(0).getId());
        ReflectionTestUtils.setField(expectedTask, "completedBy",  messages.get(1).getId());

        ProcessRelationsService processRelationsService = mock(ProcessRelationsService.class);
        ProcessInstanceDTO processInstanceDTO = ProcessInstanceDTOFactory.createFromProcessInstance(processInstance, translateService, processRelationsService, messageRepository);

        assertEquals(processInstance.getState().name(), processInstanceDTO.getState());
        assertEquals(processInstance.getOriginProcessId(), processInstanceDTO.getOriginProcessId());
        assertThat(processInstanceDTO.getProcessData()).containsOnly(toProcessDataDTO(processData));
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
        final Set<TaskDataDTO> expectedTaskDataDTOs = messages.stream().
                flatMap(message -> message.getMessageData().stream()).
                map(messageData -> TaskDataDTO.builder().
                        key(messageData.getKey()).
                        value(messageData.getValue()).
                        labels(createLabels(messageData.getKey())).
                        build()).
                collect(Collectors.toSet());
        assertThat(taskDTO.getTaskData()).isEqualTo(expectedTaskDataDTOs);
        assertEquals(3, processInstanceDTO.getMessages().size());
        assertEquals(ProcessInstanceStubs.event, processInstanceDTO.getMessages().getFirst().getName());
        assertEquals(2, processInstanceDTO.getMilestones().size());
        MilestoneDTO reachedMilestone = processInstanceDTO.getMilestones().stream()
                .filter(ms -> ms.getState().equals("REACHED")).findFirst().orElseThrow();
        MilestoneDTO notReachedMilestone = processInstanceDTO.getMilestones().stream()
                .filter(ms -> !ms.getState().equals("REACHED")).findFirst().orElseThrow();
        assertEquals(ProcessInstanceStubs.milestone, reachedMilestone.getName());
        assertEquals(ProcessInstanceStubs.neverReachedMilestone, notReachedMilestone.getName());
        ProcessCompletionDTO processCompletion = processInstanceDTO.getProcessCompletion();
        assertNotNull(processCompletion);
        assertEquals(processInstance.getProcessCompletion().get().getConclusion().name(), processCompletion.getConclusion());
        assertEquals("all good", processCompletion.getReason().get("de"));
        assertEquals(processInstance.getProcessCompletion().get().getCompletedAt(), processCompletion.getCompletedAt());
    }

    private ProcessDataDTO[] toProcessDataDTO(Set<ProcessData> processData) {
        return processData.stream()
                .map(pd -> new ProcessDataDTO(pd.getKey(), pd.getValue(), pd.getRole()))
                .toArray(ProcessDataDTO[]::new);
    }

    @Test
    void createMilestones_sortByReachedAtThenByName() {
        List<Milestone> milestones = List.of(
                ProcessInstanceStubs.createMilestone("notReachedB", false),
                ProcessInstanceStubs.createMilestone("reachedB", true),
                ProcessInstanceStubs.createMilestone("notReachedA", false),
                ProcessInstanceStubs.createMilestone("reachedA", true)
        );

        List<MilestoneDTO> milestoneDTOs = ProcessInstanceDTOFactory.createMilestones(milestones, "process", translateService);

        assertEquals("reachedA", milestoneDTOs.get(0).getName());
        assertEquals("reachedB", milestoneDTOs.get(1).getName());
        assertEquals("notReachedA", milestoneDTOs.get(2).getName());
        assertEquals("notReachedB", milestoneDTOs.get(3).getName());
    }

    @Test
    void createEvents_sortByReceivedAtThenName() {
        ZonedDateTime olderTimestamp = ZonedDateTime.now();
        ZonedDateTime newerTimestamp = olderTimestamp.plusDays(1);
        List<MessageReferenceMessageDTO> eventReferences = List.of(
                createEventReferenceEventDTO(newerTimestamp, "newerB"),
                createEventReferenceEventDTO(newerTimestamp, "newerA"),
                createEventReferenceEventDTO(olderTimestamp, "olderB"),
                createEventReferenceEventDTO(olderTimestamp, "olderA")
        );
        List<MessageDTO> messageDTOS = ProcessInstanceDTOFactory.createMessages(eventReferences);

        assertEquals("newerA", messageDTOS.get(0).getName());
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
                .build();
        List<String[]> completingEventUserData = new ArrayList<>();
        completingEventUserData.add(new String[]{"completing-userdata-key1", "completing-userdata-value-1"});
        completingEventUserData.add(new String[]{"completing-userdata-key2", "completing-userdata-value-2"});
        when(messageRepository.findMessageUserDataByMessageId(completingMessage.getId())).thenReturn(completingEventUserData);

        return List.of(planningMessage, completingMessage);
    }

    private MessageReferenceMessageDTO createEventReferenceEventDTO(ZonedDateTime receivedAt, String eventName) {
        return MessageReferenceMessageDTO.builder()
                .messageReferenceId(Generators.timeBasedEpochGenerator().generate())
                .messageId(Generators.timeBasedEpochGenerator().generate())
                .messageReceivedAt(receivedAt)
                .messageName(eventName)
                .messageData(emptySet())
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
        List<TaskInstanceDTO> taskDTOs = ProcessInstanceDTOFactory.createTasks(tasks, List.of(), "process", translateService, messageRepository);

        assertEquals("taskA", taskDTOs.get(0).getName().get("de"));
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
        assertThat(dto.getProcessData())
                .isNotEmpty();
    }

    private Map<String, String> createLabels(String value) {
        return Map.of("de", value);
    }
}
