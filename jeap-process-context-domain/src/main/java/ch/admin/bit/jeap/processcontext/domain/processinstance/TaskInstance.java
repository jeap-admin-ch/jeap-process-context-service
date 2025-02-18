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
import java.util.Set;
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
    @ManyToOne
    @JoinColumn(name = "process_instance_id")
    @Getter(value = PACKAGE)
    private ProcessInstance processInstance;

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

    void plan(String originTaskId, ZonedDateTime timestamp) {
        this.originTaskId = originTaskId;
        state = TaskState.PLANNED;
        plannedAt = timestamp;
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

    void evaluateIfCompleted(MessageReferenceMessageDTO messageReference, ZonedDateTime timestamp) {
        if (Set.of(TaskState.PLANNED, TaskState.NOT_PLANNED).contains(state)) {
            if (taskType.isPresent() && taskType.get().getCompletedByDomainEvent() != null && isCompleted(messageReference)) {
                state = TaskState.COMPLETED;
                completedAt = timestamp;
                completedBy = messageReference.getMessageId();
            }
        }
    }

    private boolean isCompleted(MessageReferenceMessageDTO messageReference) {
        if (taskType.isPresent() && TaskCardinality.SINGLE_INSTANCE == taskType.get().getCardinality()) {
            return messageReference.getMessageName().equals(taskType.get().getCompletedByDomainEvent());
        } else {
            return taskType.isPresent() && messageReference.getMessageName().equals(taskType.get().getCompletedByDomainEvent())
                    && messageReference.getRelatedOriginTaskIds().contains(originTaskId);
        }
    }

}
