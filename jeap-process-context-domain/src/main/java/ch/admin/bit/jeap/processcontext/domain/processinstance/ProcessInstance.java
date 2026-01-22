package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.MutableDomainEntity;
import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.message.MessageData;
import ch.admin.bit.jeap.processcontext.domain.message.OriginTaskId;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessDataTemplate;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessRelationPattern;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskType;
import ch.admin.bit.jeap.processcontext.plugin.api.condition.ProcessCompletionConditionResult;
import ch.admin.bit.jeap.processcontext.plugin.api.condition.ProcessSnapshotConditionResult;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext;
import com.fasterxml.uuid.Generators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessCompletionConclusion.SUCCEEDED;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toSet;
import static lombok.AccessLevel.PROTECTED;
import static org.springframework.util.StringUtils.hasText;

@SuppressWarnings({"FieldMayBeFinal", "JpaDataSourceORMInspection"}) // JPA spec mandates non-final fields
@NoArgsConstructor(access = PROTECTED) // for JPA
@ToString
@Entity
@Slf4j
public class ProcessInstance extends MutableDomainEntity {

    @Id
    @NotNull
    @Getter
    private UUID id = Generators.timeBasedEpochGenerator().generate();

    @Column(unique = true)
    @NotNull
    @Getter
    private String originProcessId;

    @NotNull
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "processInstance")
    private Set<ProcessData> processData;

    @NotNull
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "processInstance")
    private Set<Relation> relations;

    @NotNull
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "processInstance")
    private Set<ProcessRelation> processRelations;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Getter
    private ProcessState state;

    @Embedded
    ProcessCompletion processCompletion;

    @NotNull
    @Column(name = "template_name")
    @Getter
    private String processTemplateName;

    @Column(name = "template_hash")
    @Getter
    private String processTemplateHash;

    @Transient
    @Getter
    private ProcessTemplate processTemplate;

    @Getter
    private ZonedDateTime lastCorrelationAt;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "processInstance")
    private List<TaskInstance> tasks = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "processInstance")
    private List<MessageReference> messageReferences = new ArrayList<>();

    @Setter
    @Transient
    private List<MessageReferenceMessageDTO> messageReferenceMessageDTOS = new ArrayList<>();

    @Getter
    private int latestSnapshotVersion = 0;

    @Getter
    @Convert(converter = SnapshotNameSetConverter.class)
    private LinkedHashSet<String> snapshotNames = new LinkedHashSet<>(); // could be general type SequencedSet with Java 21

    private ProcessInstance(String originProcessId, ProcessTemplate processTemplate, Set<ProcessData> processData) {
        Objects.requireNonNull(originProcessId, "Origin process ID is mandatory");
        Objects.requireNonNull(processTemplate, "Process template is mandatory");
        this.originProcessId = originProcessId;
        this.processData = new HashSet<>();
        addProcessData(processData);
        this.relations = new HashSet<>();
        this.processRelations = new HashSet<>();
        this.processTemplate = processTemplate;
        this.processTemplateName = processTemplate.getName();
        this.processTemplateHash = processTemplate.getTemplateHash();
        this.state = ProcessState.STARTED;
    }

    public static ProcessInstance startProcess(String originProcessId, ProcessTemplate processTemplate, Set<ProcessData> processData) {
        ProcessInstance processInstance = new ProcessInstance(originProcessId, processTemplate, processData);
        processInstance.planInitialTasks();
        return processInstance;
    }

    public int nextSnapshotVersion() {
        latestSnapshotVersion++;
        return latestSnapshotVersion;
    }

    public void registerSnapshot(String snapshotName) {
        snapshotNames.add(snapshotName);
    }

    private void planInitialTasks() {
        Set<TaskType> taskTypesWithoutTaskInstance = TaskUtils.taskTypesWithoutTaskInstance(tasks, processTemplate);
        taskTypesWithoutTaskInstance.stream()
                .filter(TaskType::isPlannedAtProcessStart)
                .map(type -> TaskInstance.createInitialTaskInstance(type, this, ZonedDateTime.now()))
                .forEach(tasks::add);
        updateProcessState();
    }

    void registerNewTaskInUnknownState(TaskType taskType, ZonedDateTime timestamp) {
        tasks.add(TaskInstance.createUnknownTaskInstance(taskType, this, timestamp));
        updateProcessState();
    }

    void planDomainEventTask(TaskType taskType, String originTaskId, ZonedDateTime timestamp, UUID messageId) {
        tasks.add(TaskInstance.createTaskInstanceWithOriginTaskId(taskType, this, originTaskId, timestamp, messageId));
    }

    void addObservationTask(TaskType taskType, String messageId, ZonedDateTime timestamp, UUID messageUuid) {
        tasks.add(TaskInstance.createTaskInstanceWithOriginTaskIdAndState(taskType, this, messageId, TaskState.COMPLETED, timestamp, messageUuid));
    }

    void evaluateCompletedTasks(ZonedDateTime timestamp) {
        for (MessageReferenceMessageDTO messageReference : this.getMessageReferences()) {
            evaluateCompletedTasks(messageReference, timestamp);
        }
        updateProcessState();
    }

    void evaluateCompletedTasks(MessageReferenceMessageDTO messageReference, ZonedDateTime timestamp) {
        this.getTasks().forEach(task -> task.evaluateIfCompleted(messageReference, timestamp));
    }

    /**
     * Evaluate the snapshot conditions defined for this process and for every condition that
     * triggers a new snapshot return the name of the snapshot triggered.
     * @return The names of newly triggered snapshots as determined by the snapshot conditions.
     */
    Set<String> evaluateSnapshotConditions() {
        if ((getProcessTemplate() == null) || getProcessTemplate().getProcessSnapshotConditions().isEmpty()) {
            return emptySet();
        }
        ProcessContext processContext = ProcessContextFactory.createProcessContext(this);
        return getProcessTemplate().getProcessSnapshotConditions().stream().
                map(snapshotCondition -> snapshotCondition.triggerSnapshot(processContext)).
                filter(ProcessSnapshotConditionResult::isSnapShotTriggered).
                map(ProcessSnapshotConditionResult::getSnapshotName).
                filter(snapshotName -> !snapshotNames.contains(snapshotName)).
                collect(toSet());
    }

    /**
     * @return An (unmodifiable) list of Task instances in this process instance
     */
    public List<TaskInstance> getTasks() {
        return unmodifiableList(tasks);
    }

    public List<TaskInstance> getOpenTasks() {
        return getTasks().stream().filter(t -> !t.getState().isFinalState()).toList();
    }

    public List<MessageReferenceMessageDTO> getMessageReferences() {
        return unmodifiableList(messageReferenceMessageDTOS);
    }

    public MessageReferenceMessageDTO addMessage(Message message) {
        MessageReference messageReference = MessageReference.from(message);
        messageReference.setOwner(this);
        copyMessageDataToProcessData(message);
        messageReferences.add(messageReference);
        MessageReferenceMessageDTO messageReferenceMessageDTO = toMessageReferenceMessageDTO(messageReference.getId(), message);
        messageReferenceMessageDTOS.add(messageReferenceMessageDTO);
        return messageReferenceMessageDTO;
    }

    private MessageReferenceMessageDTO toMessageReferenceMessageDTO(UUID messageReferenceId, Message message) {
        return MessageReferenceMessageDTO.builder()
                .messageReferenceId(messageReferenceId)
                .messageId(message.getId())
                .messageName(message.getMessageName())
                .messageCreatedAt(message.getMessageCreatedAt())
                .messageReceivedAt(message.getReceivedAt())
                .messageData(toDto(message.getMessageData(processTemplateName)))
                .relatedOriginTaskIds(toRelatedOriginTaskIds(message.getOriginTaskIds(processTemplateName)))
                .build();
    }

    private static Set<MessageReferenceMessageDataDTO> toDto(Set<MessageData> messageData) {
        return messageData.stream().map(MessageReferenceMessageDataDTO::from).collect(toSet());
    }

    private static Set<String> toRelatedOriginTaskIds(Set<OriginTaskId> originTaskIds) {
        return originTaskIds.stream().map(OriginTaskId::getOriginTaskId).collect(toSet());
    }

    public Optional<ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessCompletion> getProcessCompletion() {
        ProcessCompletion reportedCompletion = processCompletion;
        if ((state==ProcessState.COMPLETED) && (reportedCompletion==null)) {
            // this is an 'old' process instance that completed without setting completion data -> create derived completion data
            reportedCompletion = new ProcessCompletion(SUCCEEDED, "All tasks completed.", getModifiedAt());
        }
        return Optional.ofNullable(reportedCompletion).
                map(completion -> ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessCompletion.builder()
                        .conclusion(ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessCompletionConclusion.valueOf(completion.getConclusion().name()))
                        .name(completion.getName())
                        .completedAt(completion.getCompletedAt())
                        .build());
    }

    /**
     * Set process template after re-loading the domain object from persistent state
     */
    public void setProcessTemplate(ProcessTemplate processTemplate) {
        if (this.processTemplate != null) {
            throw new IllegalStateException("Cannot set process template - already set for process " + originProcessId);
        }
        this.processTemplate = processTemplate;
        tasks.forEach(task -> task.setTaskTypeFromTemplate(processTemplate));
    }

    private void updateProcessState() {
        if (state == ProcessState.COMPLETED) {
            // final state
            return;
        }
        final ProcessContext processContext = ProcessContextFactory.createProcessContext(this);
        processTemplate.getProcessCompletionConditions().stream()
                .map(condition -> condition.isProcessCompleted(processContext))
                .filter(ProcessCompletionConditionResult::isCompleted)
                .findFirst()
                .ifPresent( result -> {
                    this.state = ProcessState.COMPLETED;
                    this.processCompletion = new ProcessCompletion(
                            ProcessCompletionConclusion.valueOf(result.getConclusion().get().name()),
                            result.getName().orElse(null),
                            ZonedDateTime.now());
                });
    }

    @Override
    public ZonedDateTime getCreatedAt() {
        return super.getCreatedAt();
    }

    @Override
    public ZonedDateTime getModifiedAt() {
        return super.getModifiedAt();
    }

    /**
     * @return The (unmodifiable) set of process data associated with this process instance.
     */
    public Set<ProcessData> getProcessData() {
        return unmodifiableSet(processData);
    }

    /**
     * Copies the EventData to the ProcessData, if the Template defines this
     */
    private void copyMessageDataToProcessData(Message message) {
        String messageName = message.getMessageName();
        Set<MessageData> messageData = message.getMessageData(processTemplateName);
        List<ProcessDataTemplate> processDataTemplates = processTemplate.getProcessDataTemplatesBySourceMessageName(messageName);
        processDataTemplates.forEach(template -> applyProcessDataTemplate(messageName, messageData, template));
    }

    private void applyProcessDataTemplate(String messageName, Set<MessageData> messageDataSet, ProcessDataTemplate processDataTemplate) {
        String sourceKey = processDataTemplate.getSourceMessageDataKey();
        String targetKey = processDataTemplate.getKey();
        messageDataSet.forEach(messageData -> {
            if (sourceKey.equals(messageData.getKey())) {
                addProcessData(messageName, targetKey, messageData);
            }
        });
    }

    private void addProcessData(String messageName, String targetKey, MessageData messageData) {
        ProcessData processDataItem = new ProcessData(targetKey, messageData.getValue(), messageData.getRole());
        if (processData.add(processDataItem)) {
            processDataItem.setProcessInstance(this);
            log.debug("Added process data to process instance {}: messageName: {}, key: {}, value: {}, role: {}",
                    id, messageName, targetKey, messageData.getValue(), messageData.getRole());
        }
    }

    private void addProcessData(Set<ProcessData> processData) {
        if (processData != null) {
            processData.forEach(item -> item.setProcessInstance(this));
            this.processData.addAll(processData);
        }
    }

    /**
     * @return The (unmodifiable) set of relations inferred by this process instance so far.
     */
    public Set<Relation> getRelations() {
        return unmodifiableSet(relations);
    }

    void evaluateRelations() {
        if (processTemplate.getRelationPatterns().isEmpty()) {
            // No relation patterns - no need to evaluate relations
            return;
        }
        ProcessDataWrapper processDataWrapper = ProcessDataWrapper.of(processData);
        String systemId = processTemplate.getRelationSystemId();
        processTemplate.getRelationPatterns().stream()
                .flatMap(pattern -> RelationFactory.createMatchingRelations(systemId, pattern, processDataWrapper).stream())
                .forEach(this::addRelation);
    }

    private void addRelation(Relation relation) {
        if (relations.add(relation)) {
            relation.setProcessInstance(this);
            relation.onPrePersist();
            log.debug("Added relation to process instance {}: {}", id, relation);
        }
    }

    public Set<ProcessRelation> getProcessRelations() {
        return unmodifiableSet(processRelations);
    }

    private void addProcessRelation(ProcessRelation processRelation) {
        if (processRelations.add(processRelation)) {
            processRelation.setProcessInstance(this);
            processRelation.onPrePersist();
            log.debug("Added process relation to process instance {}: {}", id, processRelation);
        }
    }

    void evaluateProcessRelations(Message message) {
        if (processTemplate.getProcessRelationPatterns().isEmpty()) {
            // No processRelation pattern - no need to evaluate processRelations
            return;
        }

        List<ProcessRelationPattern> patterns = processTemplate.getProcessRelationPatterns();
        patterns.forEach(processRelationPattern -> {
            String messageName = processRelationPattern.getSource().getMessageName();
            String messageKey = processRelationPattern.getSource().getMessageDataKey();
            if (messageName.equals(message.getMessageName())) {
                Set<MessageData> messageDataSet = message.getMessageData(processTemplateName);
                messageDataSet.forEach(messageData -> {
                    if (messageKey.equals(messageData.getKey())) {
                        //now we have a hit
                        String msgDataValue = messageData.getValue();
                        // create ProcessRelation
                        ProcessRelation processRelation = ProcessRelation.createMatchingProcessRelation(processRelationPattern, msgDataValue);
                        this.addProcessRelation(processRelation);
                    }
                });
            }

        });
    }

    /**
     * Evaluates the Date/Time of the newest (last) Message
     *
     * @return ZoneDateTime
     */
    public Optional<ZonedDateTime> getLastMessageDateTime() {
        MessageReferenceMessageDTO lastMessage = this.getMessageReferences().stream()
                .max(Comparator.comparing(MessageReferenceMessageDTO::getMessageReceivedAt))
                .orElse(null);
        if (lastMessage != null) {
            return Optional.of(lastMessage.getMessageReceivedAt());
        }
        return Optional.empty();
    }

    void correlatedAt(ZonedDateTime lastCorrelationAt) {
        this.lastCorrelationAt = lastCorrelationAt;
    }

    public void applyTemplateMigrationIfChanged() {
        if (this.isTemplateChanged()) {
            log.info("Applying template migrations to process {}", this.getOriginProcessId());

            this.deleteTaskInstancesForDeletedTaskTypes();
            this.planTaskInstancesForNewTaskTypes();

            this.updateTemplateHash();

            this.evaluateCompletedTasks(ZonedDateTime.now());
        }
    }

    private boolean isTemplateChanged() {
        return processTemplateHash != null &&
                !processTemplate.getTemplateHash().equals(processTemplateHash);
    }

    private void updateTemplateHash() {
        this.processTemplateHash = processTemplate.getTemplateHash();
    }

    private void deleteTaskInstancesForDeletedTaskTypes() {
        final List<TaskInstance> openTasks = this.getOpenTasks();
        log.debug("Migration - Found {} open tasks in processInstance '{}'", openTasks.size(), this.getOriginProcessId());
        final Set<String> allTaskNames = this.getProcessTemplate().getTaskNames();
        final List<TaskInstance> taskToDelete = openTasks.stream().filter(openTask -> !allTaskNames.contains(openTask.getTaskTypeName())).toList();
        log.debug("Migration - Found {} open tasks to set as deleted", taskToDelete.size());
        for (TaskInstance task : taskToDelete) {
            log.info("Migration - Set State DELETED for TaskInstance '{}'", task.getTaskTypeName());
            task.delete();
        }
    }

    private void planTaskInstancesForNewTaskTypes() {
        final Set<String> taskInstanceNames = this.getTasks().stream().map(TaskInstance::getTaskTypeName).collect(Collectors.toUnmodifiableSet());
        final Set<String> allTaskNames = new HashSet<>(this.getProcessTemplate().getTaskNames());
        log.debug("Migration - Found {} task types in process template '{}'", allTaskNames.size(), this.getProcessTemplate().getName());
        allTaskNames.removeAll(taskInstanceNames);
        log.debug("Migration - Found {} new task types", allTaskNames.size());
        for (String taskName : allTaskNames) {
            TaskType taskType = processTemplate.getTaskTypeByName(taskName).orElseThrow(TaskPlanningException.invalidTaskType(taskName, originProcessId));
            if (taskType.isPlannedAtProcessStart()) {
                log.info("Migration - Plan new instance of task '{}'", taskName);
                this.registerNewTaskInUnknownState(taskType, ZonedDateTime.now());
            } else {
                log.info("Migration - Task '{}' does not need upfront planning.", taskName);
            }
        }
    }

    @Converter
    public static class SnapshotNameSetConverter implements AttributeConverter<LinkedHashSet<String>, String> {
        private static final String SEPARATOR_CHAR = " ";

        @Override
        public String convertToDatabaseColumn(LinkedHashSet<String> snapshotNames) {
            return snapshotNames != null ? String.join(SEPARATOR_CHAR, snapshotNames) : "";
        }

        @Override
        public LinkedHashSet<String> convertToEntityAttribute(String snapshotNamesString) {
            return hasText(snapshotNamesString) ? new LinkedHashSet<>(Arrays.asList(snapshotNamesString.split(SEPARATOR_CHAR))) : new LinkedHashSet<>();
        }
    }
}
