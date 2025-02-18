package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessRelation;
import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.*;
import ch.admin.bit.jeap.processcontext.domain.PcsConfigProperties;
import ch.admin.bit.jeap.processcontext.domain.TranslateService;
import ch.admin.bit.jeap.processcontext.domain.message.MessageRepository;
import ch.admin.bit.jeap.processcontext.domain.processrelation.ProcessRelationView;
import ch.admin.bit.jeap.processcontext.domain.processrelation.ProcessRelationsService;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskType;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessCompletion;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class ProcessSnapshotService {

    private final TranslateService translateService;
    private final PcsConfigProperties pcsConfigProperties;
    private final Optional<ProcessSnapshotRepository> processSnapshotRepository;
    private final ProcessTemplateRepository processTemplateRepository;
    private final ProcessRelationsService processRelationsService;
    private final MessageRepository messageRepository;

    @SuppressWarnings("java:S112")
    @PostConstruct
    void validate() {
        if (processTemplateRepository.hasProcessSnapshotsConfigured() && processSnapshotRepository.isEmpty()) {
            throw new RuntimeException("There are conditions configured to create process snapshots but no storage to " +
                                       "store the snapshots has been configured. Did you forget to configure the S3 storage snapshot bucket name?");
        }
    }

    public void createAndStoreSnapshot(ProcessInstance processInstance) {
        ProcessSnapshotArchiveData processSnapshotArchiveData = createProcessSnapshotArchiveData(processInstance);
        processSnapshotRepository.get().storeSnapshot(processSnapshotArchiveData);
    }

    ProcessSnapshotArchiveData createProcessSnapshotArchiveData(ProcessInstance processInstance) {
        ProcessSnapshot processSnapshot = new ProcessSnapshot();
        processSnapshot.setSnapshotDateTimeCreated(Instant.now());
        processSnapshot.setOriginProcessId(processInstance.getOriginProcessId());
        processSnapshot.setTemplateLabel(getProcessTemplateDescription(processInstance));
        processSnapshot.setTemplateName(processInstance.getProcessTemplateName());
        processSnapshot.setState(processInstance.getState().name());
        processSnapshot.setDateTimeCreated(processInstance.getCreatedAt().toInstant());
        if (processInstance.getModifiedAt() != null) {
            processSnapshot.setDateTimeModified(processInstance.getModifiedAt().toInstant());
        }
        Optional<ProcessCompletion> processCompletionOptional = processInstance.getProcessCompletion();
        if (processCompletionOptional.isPresent()) {
            ProcessCompletion processCompletion = processCompletionOptional.get();
            processSnapshot.setCompletionName(processCompletion.getName());
            processSnapshot.setDateTimeCompleted(processCompletion.getCompletedAt().toInstant());
            processSnapshot.setCompletionReasonLabel("label");
            processSnapshot.setCompletionConclusion(String.valueOf(processCompletion.getConclusion()));
        }
        processSnapshot.setProcessData(processInstance.getProcessData().stream()
                .map(ProcessSnapshotService::toProcessData).toList());
        processSnapshot.setTasks(processInstance.getTasks().stream()
                .map(task -> toTask(processInstance, task)).toList());
        List<ProcessRelationView> processRelations = processRelationsService.createProcessRelations(processInstance);
        processSnapshot.setProcessRelations(processRelations.stream()
                .map(this::toRelation).toList());
        int snapshotVersion = processInstance.nextSnapshotVersion();
        return ProcessSnapshotArchiveData.from(processSnapshot, snapshotVersion, pcsConfigProperties.getProcessSnapshotArchiveRetentionPeriodMonths());
    }

    private static ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessData toProcessData(ProcessData processData) {
        return new ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessData(
                processData.getKey(), processData.getValue(), processData.getRole());
    }

    private Task toTask(ProcessInstance processInstance, TaskInstance task) {
        return new Task(
                task.getTaskTypeName(),
                getTaskTypeDescription(processInstance.getProcessTemplateName(), task),
                task.getOriginTaskId(),
                task.getState().name(),
                task.getCreatedAt().toInstant(),
                task.getPlannedAt() != null ? task.getPlannedAt().toInstant() : null,
                getUser(task.getPlannedBy()),
                task.getCompletedAt() != null ? task.getCompletedAt().toInstant() : null,
                getUser(task.getCompletedBy()),
                getTaskData(task, processInstance.getMessageReferences()));
    }

    private User getUser(UUID messageId) {
        if (messageId == null) {
            return null;
        }
        List<UserData> userData = getUserData(messageId);
        if (userData.isEmpty()) {
            return null;
        }
        return new User(userData);
    }

    private List<UserData> getUserData(UUID messageId) {
        return messageRepository.findMessageUserDataByMessageId(messageId).stream()
                .map(data -> UserData.newBuilder().
                        setKey(data[0]).
                        setValue(data[1]).
                        setLabel(getUserDataLabel(data[0], defaultLanguage())).
                        build()).
                toList();
    }

    private String getUserDataLabel(String userDataKey, String language) {
        return translateService.translateUserDataKey(userDataKey).get(language);
    }

    private List<TaskData> getTaskData(TaskInstance taskInstance, List<MessageReferenceMessageDTO> messages) {
        Optional<TaskType> optionalTaskType = taskInstance.getTaskType();
        if (optionalTaskType.isEmpty() || (messages == null)) {
            return List.of();
        }
        TaskType taskType = optionalTaskType.get();
        List<MessageReferenceMessageDTO> planningOrCompletingMessages = getPlanningOrCompletingMessages(taskInstance, messages);
        String processTemplateName = taskInstance.getProcessInstance().getProcessTemplateName();
        String taskTypeName = taskType.getName();
        return taskType.getTaskData().stream().
                flatMap(taskDataDeclaration ->
                        getTaskData(taskDataDeclaration, planningOrCompletingMessages, processTemplateName, taskTypeName)).
                toList();
    }

    Stream<TaskData> getTaskData(ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskData taskDataDeclaration,
                         List<MessageReferenceMessageDTO> planningOrCompletingMessages,
                         String processTemplateName, String taskTypeName) {
        return planningOrCompletingMessages.stream().
                filter(message -> taskDataDeclaration.getSourceMessage().equals(message.getMessageName())).
                findFirst().
                map(message -> message.getMessageData().stream().
                        filter(messageData -> taskDataDeclaration.getMessageDataKeys().contains(messageData.getMessageDataKey())).
                        map(messageDataMatchingTaskDataKey ->
                                TaskData.newBuilder().
                                    setKey(messageDataMatchingTaskDataKey.getMessageDataKey()).
                                    setValue(messageDataMatchingTaskDataKey.getMessageDataValue()).
                                    setLabel(getTaskDataLabel(processTemplateName, taskTypeName,
                                            messageDataMatchingTaskDataKey.getMessageDataKey(), defaultLanguage())).
                                build())).
                orElse(Stream.empty());
    }

    private String getTaskDataLabel(String processTemplate, String taskTypeName, String taskDataKey, String language) {
        return translateService.translateTaskDataKey(processTemplate, taskTypeName, taskDataKey).get(language);
    }

    private static List<MessageReferenceMessageDTO> getPlanningOrCompletingMessages(TaskInstance taskInstance, List<MessageReferenceMessageDTO> messages) {
        return messages.stream().
                filter(message ->
                        message.getMessageId().equals(taskInstance.getPlannedBy()) ||
                        message.getMessageId().equals(taskInstance.getCompletedBy())).
                toList();
    }

    private ProcessRelation toRelation(ProcessRelationView relation) {
        return ProcessRelation.newBuilder()
                .setRelationRole(ProcessRelationRole.valueOf(relation.getRelationRole().name()))
                .setRelationName(relation.getRelationName())
                .setOriginRole(relation.getOriginRole())
                .setTargetRole(relation.getTargetRole())
                .setProcessName(relation.getProcessTemplateName())
                .setProcessLabel(relation.getProcessName().getOrDefault(defaultLanguage(), relation.getProcessTemplateName()))
                .setOriginProcessId(relation.getProcessId())
                .setProcessState(relation.getProcessState())
                .build();
    }

    private String getProcessTemplateDescription(ProcessInstance processInstance) {
        Map<String, String> descriptionsByLanguage = translateService.translateProcessTemplateName(processInstance.getProcessTemplateName());
        return descriptionsByLanguage.get(defaultLanguage());
    }

    private String getTaskTypeDescription(String processTemplateName, TaskInstance taskInstance) {
        Map<String, String> descriptionsByLanguage = translateService.translateTaskTypeName(processTemplateName, taskInstance.getTaskTypeName());
        return descriptionsByLanguage.get(defaultLanguage());
    }

    private String defaultLanguage() {
        return pcsConfigProperties.getProcessSnapshotLanguage().name().toLowerCase();
    }

}
