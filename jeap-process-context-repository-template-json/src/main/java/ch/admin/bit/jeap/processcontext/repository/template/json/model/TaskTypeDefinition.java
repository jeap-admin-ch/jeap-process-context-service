package ch.admin.bit.jeap.processcontext.repository.template.json.model;

import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskCardinality;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskLifecycle;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Set;

@Data
public class TaskTypeDefinition {

    private static final String LEGACY_CARDINALITY_SINGLE = "SINGLE";
    private static final String LEGACY_CARDINALITY_DYNAMIC = "DYNAMIC";

    private String name;
    private String cardinality;
    private TaskLifecycle lifecycle;

    private TaskPlannedByConditionDefinition plannedBy;
    private TaskCompletionConditionDefinition completedBy;
    private TaskObservesConditionDefinition observes;
    private Set<TaskDataDefinition> taskData;

    public TaskTypeDefinition(String name) {
        this.name = name;
        applyDefaultsAndMigrateTaskCardinality();
    }

    @JsonCreator
    public TaskTypeDefinition(@JsonProperty("name") String name,
                              @JsonProperty("lifecycle") TaskLifecycle lifecycle,
                              @JsonProperty("cardinality") String cardinality) {
        this.name = name;
        this.lifecycle = lifecycle;
        this.cardinality = cardinality;
        applyDefaultsAndMigrateTaskCardinality();
    }

    /**
     * When the TaskObservesConditionDefinition is defined, set the Lifecyle to OBSERVED
     * and the Cardinality to MULTI_INSTANCE
     * @param observes ConditionDefiniton for TaskObserved
     */
    public void setObserves(TaskObservesConditionDefinition observes) {
        this.observes = observes;
        this.lifecycle = TaskLifecycle.OBSERVED;
        this.cardinality = TaskCardinality.MULTI_INSTANCE.name();
    }

    void applyDefaultsAndMigrateTaskCardinality() {
        if ((cardinality == null) && (lifecycle == null)) {
            lifecycle = TaskLifecycle.STATIC;
            cardinality = TaskCardinality.SINGLE_INSTANCE.name();
            return;
        }
        if (cardinality == null) {
            cardinality = lifecycle.getDefaultCardinality().name();
        }
        if (lifecycle == null) {
            if (cardinality.equalsIgnoreCase(LEGACY_CARDINALITY_SINGLE)) {
                lifecycle = TaskLifecycle.STATIC;
                cardinality = TaskCardinality.SINGLE_INSTANCE.name();
            } else if (cardinality.equalsIgnoreCase(LEGACY_CARDINALITY_DYNAMIC)) {
                lifecycle = TaskLifecycle.DYNAMIC;
                cardinality = TaskCardinality.MULTI_INSTANCE.name();
            }
        }
    }

    public static boolean isLegacyCardinalityName(String cardinalityStr) {
        return LEGACY_CARDINALITY_SINGLE.equalsIgnoreCase(cardinalityStr) ||
               LEGACY_CARDINALITY_DYNAMIC.equalsIgnoreCase(cardinalityStr);
    }

}
