package ch.admin.bit.jeap.processcontext.adapter.restapi.model;

import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.Task;
import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.User;
import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.UserData;
import ch.admin.bit.jeap.processcontext.domain.TranslateService;
import ch.admin.bit.jeap.processcontext.domain.message.MessageRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.TaskInstance;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskData;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ch.admin.bit.jeap.processcontext.adapter.restapi.model.ProcessInstanceDTOFactory.toZonedDateTime;

@Value
@AllArgsConstructor
@Builder(access = AccessLevel.PRIVATE)
public class TaskInstanceDTO {

    public static final String UNKNOWN = "UNKNOWN";

    String originTaskId;
    Map<String, String> name;
    String lifecycle;
    String cardinality;
    String state;
    ZonedDateTime plannedAt;
    ZonedDateTime completedAt;
    Set<MessageUserDataDTO> plannedBy;
    Set<MessageUserDataDTO> completedBy;
    Set<TaskDataDTO> taskData;

    static TaskInstanceDTO create(TaskInstance taskInstance, List<MessageDTO> messages, String processTemplate,
                                  TranslateService translateService, MessageRepository messageRepository) {
        final Optional<TaskType> taskType = taskInstance.getTaskType();
        return TaskInstanceDTO.builder()
                .name(translateService.translateTaskTypeName(processTemplate, taskInstance.getTaskTypeName()))
                .originTaskId(taskInstance.getOriginTaskId())
                .lifecycle(taskType.map(t -> t.getLifecycle().name()).orElse(UNKNOWN))
                .cardinality(taskType.map(t -> t.getCardinality().name()).orElse(UNKNOWN))
                .state(taskInstance.getState().name())
                .plannedAt(taskInstance.getPlannedAt())
                .completedAt(taskInstance.getCompletedAt())
                .plannedBy(getMessageUserDataDTOSet(taskInstance.getPlannedBy(), messageRepository, translateService))
                .completedBy(getMessageUserDataDTOSet(taskInstance.getCompletedBy(), messageRepository, translateService))
                .taskData(getTaskDataDTOs(taskInstance, messages, processTemplate, translateService))
                .build();
    }

    static TaskInstanceDTO create(Task task, String processTemplate, TranslateService translateService) {
        return TaskInstanceDTO.builder()
                .name(translateService.translateTaskTypeName(processTemplate, task.getTaskType(), task.getTaskTypeLabel()))
                .originTaskId(task.getOriginTaskId())
                .lifecycle(UNKNOWN)
                .cardinality(UNKNOWN)
                .state(task.getState())
                .plannedAt(toZonedDateTime(task.getOptionalDateTimePlanned()))
                .plannedBy(toMessageUserDataDTOSet(task.getPlannedBy(), translateService))
                .completedAt(toZonedDateTime(task.getOptionalDateTimeCompleted()))
                .completedBy(toMessageUserDataDTOSet(task.getCompletedBy(), translateService))
                .taskData(toTaskDataDTOs(processTemplate, task.getTaskType(), task.getTaskData(), translateService))
                .build();
    }

    private static Set<MessageUserDataDTO> getMessageUserDataDTOSet(UUID messageId, MessageRepository messageRepository, TranslateService translateService) {
        if (messageId == null) {
            return Set.of();
        }
        return messageRepository.findMessageUserDataByMessageId(messageId).stream()
                .map(data -> MessageUserDataDTO.builder().
                        key(data[0]).
                        value(data[1]).
                        label(getUserDataLabel(data[0], translateService)).
                        build()).
                collect(Collectors.toSet());
    }

    private static Set<TaskDataDTO> toTaskDataDTOs(String processTemplate, String taskTypeName,List<ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.TaskData> taskData, TranslateService translateService) {
        if (taskData == null) {
            return Set.of();
        }
        return taskData.stream().
                map(td -> toTaskDataDTO(processTemplate, taskTypeName, td, translateService)).
                collect(Collectors.toSet());
    }

    private static TaskDataDTO toTaskDataDTO(String processTemplate, String taskTypeName, ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.TaskData taskData, TranslateService translateService) {
        return TaskDataDTO.builder().
                key(taskData.getKey()).
                value(taskData.getValue()).
                labels(translateService.translateTaskDataKey(processTemplate, taskTypeName, taskData.getKey(), taskData.getLabel())).
                build();
    }

    private static Map<String, String> getUserDataLabel(String userDataKey, TranslateService translateService) {
        return translateService.translateUserDataKey(userDataKey);
    }

    private static Set<MessageUserDataDTO> toMessageUserDataDTOSet(User user, TranslateService translateService) {
        if ((user == null) || (user.getUserData() == null)) {
            return Set.of();
        }
        return user.getUserData().stream().
                map(ud -> toMessageUserData(ud, translateService))
                .collect(Collectors.toSet());
    }

    private static MessageUserDataDTO toMessageUserData(UserData userData, TranslateService translateService) {
        return MessageUserDataDTO.builder().
                key(userData.getKey()).
                value(userData.getValue()).
                label(translateService.translateUserDataKey(userData.getKey(), userData.getLabel())).
                build();
    }

    static Set<TaskDataDTO> getTaskDataDTOs(TaskInstance taskInstance, List<MessageDTO> messages,
                                            String processTemplate, TranslateService translateService) {
        TaskType taskType = taskInstance.getTaskType().orElse(null);
        if (taskType == null) {
            return Set.of();
        }
        List<MessageDTO> planningOrCompletingMessages = getPlanningOrCompletingMessages(taskInstance, messages);
        return taskType.getTaskData().stream().
                flatMap(taskData -> getTaskDataDTOs(taskData, planningOrCompletingMessages, taskType, processTemplate, translateService)).
                collect(Collectors.toSet());
    }

    private static List<MessageDTO> getPlanningOrCompletingMessages(TaskInstance taskInstance, List<MessageDTO> messages) {
        return messages.stream().
                filter(message ->
                        message.getId().equals(taskInstance.getPlannedBy()) ||
                        message.getId().equals(taskInstance.getCompletedBy())).
                toList();
    }

    private static Stream<TaskDataDTO> getTaskDataDTOs(final TaskData taskData, List<MessageDTO> planningOrCompletingMessages,
                                                       TaskType taskType, String processTemplate, TranslateService translateService) {
        return planningOrCompletingMessages.stream().
                filter(message -> taskData.getSourceMessage().equals(message.getName())).
                findFirst().
                map(message -> message.getMessageData().stream().
                        filter(messageData -> taskData.getMessageDataKeys().contains(messageData.getKey())).
                        map(messageDataMatchingTaskDataKey ->
                                TaskDataDTO.builder().
                                        key(messageDataMatchingTaskDataKey.getKey()).
                                        value(messageDataMatchingTaskDataKey.getValue()).
                                        labels(getTaskDataLabels(taskType.getName(), messageDataMatchingTaskDataKey.getKey(),
                                                processTemplate, translateService)).
                                        build())).
                orElse(Stream.empty());
    }

    private static Map<String, String> getTaskDataLabels(String taskTypeName, String taskDataKey,
                                                         String processTemplate, TranslateService translateService) {
        return translateService.translateTaskDataKey(processTemplate, taskTypeName, taskDataKey);
    }

}
