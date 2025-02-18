package ch.admin.bit.jeap.processcontext.repository.template.json.model;

import lombok.Data;

import java.util.Set;

@Data
public class MilestoneConditionDefinition {

    private String condition;
    private Set<String> tasksCompleted;
}
