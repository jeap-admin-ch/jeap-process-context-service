package ch.admin.bit.jeap.processcontext.domain.processtemplate;

import ch.admin.bit.jeap.processcontext.plugin.api.condition.TaskInstantiationCondition;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Slf4j
public class TaskType {
    @EqualsAndHashCode.Include
    String name;

    /**
     * Position in the definition of the template
     */
    int index;

    TaskLifecycle lifecycle;
    TaskCardinality cardinality;

    String plannedByDomainEvent;
    String observesDomainEvent;
    String completedByDomainEvent;
    TaskInstantiationCondition instantiationCondition;
    Set<TaskData> taskData;

    @SuppressWarnings("java:S107")
    @Builder
    private TaskType(@NonNull String name, int index,
                     @NonNull TaskLifecycle lifecycle, @NonNull TaskCardinality cardinality,
                     String plannedByDomainEvent, String completedByDomainEvent,
                     String observesMessage, TaskInstantiationCondition instantiationCondition,
                     Set<TaskData> taskData) {
        this.name = name;
        this.index = index;
        this.lifecycle = lifecycle;
        this.cardinality = cardinality;
        this.completedByDomainEvent = completedByDomainEvent;
        this.plannedByDomainEvent = plannedByDomainEvent;
        this.observesDomainEvent = observesMessage;
        this.instantiationCondition = instantiationCondition;
        this.taskData = taskData != null ? Set.copyOf(taskData) : Set.of();
    }

    public boolean isValidInstanceCount(int count) {
        if (count < 0) {
            // there is no such thing as a negative number of task instances
            return false;
        }
        if (count == 0) {
            // optional tasks are only allowed for non-static task types
            return lifecycle != TaskLifecycle.STATIC;
        }
        return ((cardinality == TaskCardinality.SINGLE_INSTANCE) && count == 1) ||
                (cardinality == TaskCardinality.MULTI_INSTANCE);
    }

    /**
     * Does this task type need to be planned at process start?
     */
    public boolean isPlannedAtProcessStart() {
        // static tasks are mandatory (planned at design time) and thus are planned at process start
        return getLifecycle() == TaskLifecycle.STATIC;
    }

}
