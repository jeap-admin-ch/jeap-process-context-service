package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.MutableDomainEntity;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskCardinality;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskType;
import com.fasterxml.uuid.Generators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static lombok.AccessLevel.PACKAGE;
import static lombok.AccessLevel.PROTECTED;

@SuppressWarnings({"FieldMayBeFinal", "JpaDataSourceORMInspection"}) // JPA spec mandates non-final fields
@NoArgsConstructor(access = PROTECTED) // for JPA
@ToString
@Entity
public class TaskInstance extends MutableDomainEntity {

    @Id
    @NotNull
    @Getter(PACKAGE)
    private UUID id = Generators.timeBasedEpochGenerator().generate();

    @NotNull
    @Column(name = "task_type")
    @Getter
    private String taskTypeName;

    @Transient
    @Getter
    private Optional<TaskType> taskType = Optional.empty();

    @NotNull
    @Enumerated(EnumType.STRING)
    @Getter
    private TaskState state;

    @Getter
    private String originTaskId;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_instance_id")
    //@Getter(value = PACKAGE)
    private ProcessInstance processInstance;

    ProcessInstance getProcessInstance() {
        return processInstance;
    }

    @Override
    public ZonedDateTime getCreatedAt() {
        return super.getCreatedAt();
    }

    @Getter
    private ZonedDateTime plannedAt;

    @Getter
    private UUID plannedBy;

    @Getter
    private UUID completedBy;

    @Getter
    private ZonedDateTime completedAt;

    private TaskInstance(TaskType taskType, ProcessInstance owner, TaskState state, String originTaskId, ZonedDateTime timestamp, UUID messageId) {
        this.taskType = Optional.of(taskType);
        this.taskTypeName = taskType.getName();
        this.processInstance = owner;
        this.state = state;
        this.originTaskId = originTaskId;

        if (TaskState.PLANNED.equals(state)) {
            this.plannedAt = timestamp;
            this.plannedBy = messageId;
        } else if (TaskState.COMPLETED.equals(state)) {
            this.plannedAt = timestamp;
            this.plannedBy = messageId;
            this.completedAt = timestamp;
            this.completedBy = messageId;
        }
    }

    static TaskInstance createTaskInstanceWithOriginTaskId(TaskType taskType, ProcessInstance owner, String originTaskId, ZonedDateTime timestamp, UUID messageId) {
        return createTaskInstanceWithOriginTaskIdAndState(taskType, owner, originTaskId, TaskState.PLANNED, timestamp, messageId);
    }

    static TaskInstance createTaskInstanceWithOriginTaskIdAndState(TaskType taskType, ProcessInstance owner, String originTaskId, TaskState state, ZonedDateTime timestamp, UUID messageId) {
        return new TaskInstance(taskType, owner, state, originTaskId, timestamp, messageId);
    }

    static TaskInstance createInitialTaskInstance(TaskType taskType, ProcessInstance owner, ZonedDateTime timestamp) {
        return new TaskInstance(taskType, owner, TaskState.PLANNED, null, timestamp, null);
    }

    static TaskInstance createUnknownTaskInstance(TaskType taskType, ProcessInstance owner, ZonedDateTime timestamp) {
        return new TaskInstance(taskType, owner, TaskState.UNKNOWN, null, timestamp, null);
    }

    void notRequired() {
        state = TaskState.NOT_REQUIRED;
    }

    void complete(ZonedDateTime timestamp) {
        state = TaskState.COMPLETED;
        completedAt = timestamp;
    }

    void delete() {
        state = TaskState.DELETED;
    }

    public TaskType requireTaskType() {
        return this.getTaskType().orElseThrow(() -> TaskTypeException.createTaskTypeDeleted(this.getTaskTypeName()));
    }

    /**
     * Set task type from template after re-loading the domain object from persistent state
     */
    void setTaskTypeFromTemplate(ProcessTemplate processTemplate) {
        if (this.taskType.isPresent()) {
            throw new IllegalStateException("Cannot set task type - already set for task " + taskTypeName + " in process " + processInstance.getOriginProcessId());
        }
        taskType = processTemplate.getTaskTypeByName(taskTypeName);
    }

    void evaluateIfCompleted(MessageReferenceMessageDTO messageReference) {
        if (!state.isFinalState() && taskType.isPresent() && taskType.get().getCompletedByDomainEvent() != null && isCompleted(messageReference)) {
            completeByMessage(messageReference.getMessageId(), messageReference.getMessageCreatedAt());
        }
    }

    void completeByMessage(UUID messageId, ZonedDateTime messageCreatedAt) {
        state = TaskState.COMPLETED;
        completedAt = messageCreatedAt;
        completedBy = messageId;
    }

    private boolean isCompleted(MessageReferenceMessageDTO messageReference) {
        TaskType type = taskType.orElseThrow();
        boolean completedByThisMessage = messageReference.getMessageName().equals(type.getCompletedByDomainEvent());
        if (TaskCardinality.SINGLE_INSTANCE == type.getCardinality()) {
            return completedByThisMessage;
        } else {
            return completedByThisMessage && messageReference.getRelatedOriginTaskIds().contains(originTaskId);
        }
    }

    /**
     * @return the domain event name this task instance is waiting for to be completed, or empty if the task is in a
     * final state or if the task type does not configure completion by domain event.
     */
    Optional<TaskWaitingToBeCompletedByMessage> waitingToBeCompletedByDomainEvent() {
        if (state.isFinalState() || taskType.isEmpty()) {
            return Optional.empty();
        }
        return TaskWaitingToBeCompletedByMessage.of(this, taskType.get(), originTaskId);
    }

}
