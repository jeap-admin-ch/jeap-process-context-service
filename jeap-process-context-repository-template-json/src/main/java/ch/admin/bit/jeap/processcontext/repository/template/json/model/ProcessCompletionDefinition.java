package ch.admin.bit.jeap.processcontext.repository.template.json.model;

import lombok.Data;

@Data
public class ProcessCompletionDefinition {
    private ProcessCompletionConditionDefinition completedBy;
}
