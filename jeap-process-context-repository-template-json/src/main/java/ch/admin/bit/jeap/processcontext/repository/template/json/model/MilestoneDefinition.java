package ch.admin.bit.jeap.processcontext.repository.template.json.model;

import lombok.Data;

@Data
public class MilestoneDefinition {

    private String name;
    private MilestoneConditionDefinition reachedWhen;
}

