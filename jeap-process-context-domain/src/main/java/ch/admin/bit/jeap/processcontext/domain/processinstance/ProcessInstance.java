package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.MutableDomainEntity;
import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.message.MessageData;
import ch.admin.bit.jeap.processcontext.domain.message.OriginTaskId;
import ch.admin.bit.jeap.processcontext.domain.processinstance.snapshot.ProcessSnapshotConditionResult;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessDataTemplate;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessRelationPattern;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskType;
import ch.admin.bit.jeap.processcontext.plugin.api.condition.ProcessCompletionCondition;
import ch.admin.bit.jeap.processcontext.plugin.api.condition.ProcessCompletionConditionResult;
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

    @Transient
    private ProcessContextFactory processContextFactory;

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
    private SequencedSet<String> snapshotNames = new LinkedHashSet<>();

    private ProcessInstance(String originProcessId, ProcessTemplate processTemplate, ProcessContextFactory processContextFactory) {
        Objects.requireNonNull(originProcessId, "Origin process ID is mandatory");
        Objects.requireNonNull(processTemplate, "Process template is mandatory");
        Objects.requireNonNull(processContextFactory, "Process context factory is mandatory");
        this.originProcessId = originProcessId;
        this.processData = new HashSet<>();
        this.processRelations = new HashSet<>();
        this.processTemplate = processTemplate;
        this.processTemplateName = processTemplate.getName();
        this.processTemplateHash = processTemplate.getTemplateHash();
        this.processContextFactory = processContextFactory;
        this.state = ProcessState.STARTED;
    }

    /**
     * Creates a new ProcessInstance based on the given processTemplate. Callers must persist the returned
     * ProcessInstance first, then call {@link #start()} to plan initial tasks.
     * The reason the process instance needs to be persisted first before calling start is that conditions may need
     * to access persisted data (e.g. messages) when evaluating process and task states.
     *
     * @param originProcessId       origin process id
     * @param processTemplate       the process template to base the process instance on
     * @param processContextFactory the process context factory to create process contexts for PCS API calls (conditions etc.)
     * @return A new ProcessInstance
     */
    public static ProcessInstance createProcessInstance(String originProcessId, ProcessTemplate processTemplate, ProcessContextFactory processContextFactory) {
        return new ProcessInstance(originProcessId, processTemplate, processContextFactory);
    }

    /**
     * Start the process instance by planning initial tasks as defined in the process template. This needs to be invoked
     * after persisting a new process instance created using {@link #createProcessInstance(String, ProcessTemplate, ProcessContextFactory)}.
     * If the process is not in STARTED state, this method does nothing. As this method invokes {@link #updateProcessState()}
     * internally, the process instance must be persisted first in case task or process conmpletion conditions require
     * access to persisted data.
     */
    void start() {
        if (state != ProcessState.STARTED) {
            return;
        }

        Set<TaskType> taskTypesWithoutTaskInstance = TaskUtils.taskTypesWithoutTaskInstance(tasks, processTemplate);
        taskTypesWithoutTaskInstance.stream()
                .filter(TaskType::isPlannedAtProcessStart)
                .map(type -> TaskInstance.createInitialTaskInstance(type, this, ZonedDateTime.now()))
                .forEach(tasks::add);
        updateProcessState();
    }

    public int nextSnapshotVersion() {
        latestSnapshotVersion++;
        return latestSnapshotVersion;
    }

    public void registerSnapshot(String snapshotName) {
        snapshotNames.add(snapshotName);
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

    void evaluateCompletedTasks(MessageReferenceMessageDTO messageReference) {
        getTasks().forEach(task -> task.evaluateIfCompleted(messageReference));
        updateProcessState();
    }

    /**
     * Evaluate the snapshot conditions defined for this process and for every condition that
     * triggers a new snapshot return the name of the snapshot triggered.
     *
     * @return The names of newly triggered snapshots as determined by the snapshot conditions.
     */
    Set<String> evaluateSnapshotConditions() {
        if ((getProcessTemplate() == null) || getProcessTemplate().getProcessSnapshotConditions().isEmpty()) {
            return emptySet();
        }
        return getProcessTemplate().getProcessSnapshotConditions().stream().
                map(snapshotCondition -> snapshotCondition.triggerSnapshot(this)).
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

    public AddedMessage addMessage(Message message) {
        List<ProcessData> newProcessProcessData = copyMessageDataToProcessData(message);
        MessageReference messageReference = MessageReference.from(message);
        messageReference.setOwner(this);
        messageReferences.add(messageReference);
        MessageReferenceMessageDTO messageReferenceMessageDTO = toMessageReferenceMessageDTO(messageReference.getId(), message);
        messageReferenceMessageDTOS.add(messageReferenceMessageDTO);
        return new AddedMessage(messageReferenceMessageDTO, newProcessProcessData);
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

    public Optional<ProcessCompletion> getProcessCompletion() {
        ProcessCompletion reportedCompletion = processCompletion;
        if ((state == ProcessState.COMPLETED) && (reportedCompletion == null)) {
            // this is an 'old' process instance that completed without setting completion data -> create derived completion data
            reportedCompletion = new ProcessCompletion(SUCCEEDED, "All tasks completed.", getModifiedAt());
        }
        return Optional.ofNullable(reportedCompletion);
    }

    /**
     * Set process template and process context factory after re-loading the domain object from persistent state
     */
    public void onAfterLoadFromPersistentState(ProcessTemplate processTemplate, ProcessContextFactory processContextFactory) {
        this.processContextFactory = processContextFactory;
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
        ProcessContext processContext = processContextFactory.createProcessContext(this);
        for (ProcessCompletionCondition condition : processTemplate.getProcessCompletionConditions()) {
            ProcessCompletionConditionResult processCompleted = condition.isProcessCompleted(processContext);
            if (processCompleted.isCompleted()) {
                this.state = ProcessState.COMPLETED;
                this.processCompletion = new ProcessCompletion(
                        ProcessCompletionConclusion.valueOf(processCompleted.getConclusion().orElseThrow().name()),
                        processCompleted.getName().orElse(null),
                        ZonedDateTime.now());
                return;
            }
        }
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
     *
     * @param message the incoming message containing data to copy
     * @return list of newly created ProcessData entries
     */
    private List<ProcessData> copyMessageDataToProcessData(Message message) {
        String messageName = message.getMessageName();
        Set<MessageData> messageData = message.getMessageData(processTemplateName);
        List<ProcessDataTemplate> processDataTemplates = processTemplate.getProcessDataTemplatesBySourceMessageName(messageName);
        List<ProcessData> addedProcessData = new ArrayList<>();
        processDataTemplates.forEach(template ->
                applyProcessDataTemplate(addedProcessData, messageName, messageData, template));
        return addedProcessData;
    }

    private void applyProcessDataTemplate(List<ProcessData> addedProcessData,
                                          String messageName, Set<MessageData> messageDataSet,
                                          ProcessDataTemplate processDataTemplate) {
        String sourceKey = processDataTemplate.getSourceMessageDataKey();
        String targetKey = processDataTemplate.getKey();
        messageDataSet.forEach(messageData -> {
            if (sourceKey.equals(messageData.getKey())) {
                ProcessData data = addProcessData(messageName, targetKey, messageData);
                if (data != null) {
                    addedProcessData.add(data);
                }
            }
        });
    }

    private ProcessData addProcessData(String messageName, String targetKey, MessageData messageData) {
        ProcessData processDataItem = new ProcessData(targetKey, messageData.getValue(), messageData.getRole());
        if (processData.add(processDataItem)) {
            processDataItem.setProcessInstance(this);
            log.debug("Added process data to process instance {}: messageName: {}, key: {}, value: {}, role: {}",
                    id, messageName, targetKey, messageData.getValue(), messageData.getRole());
            return processDataItem;
        }
        return null;
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
        if (isTemplateChanged()) {
            log.info("Applying template migrations to process {}", this.getOriginProcessId());

            deleteTaskInstancesForDeletedTaskTypes();
            planTaskInstancesForNewTaskTypes();

            updateTemplateHash();

            for (MessageReferenceMessageDTO messageReference : messageReferenceMessageDTOS) {
                evaluateCompletedTasks(messageReference);
            }
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
