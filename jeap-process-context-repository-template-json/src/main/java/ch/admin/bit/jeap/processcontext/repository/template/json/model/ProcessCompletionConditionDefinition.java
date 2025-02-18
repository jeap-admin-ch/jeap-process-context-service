package ch.admin.bit.jeap.processcontext.repository.template.json.model;

import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessCompletionConclusion;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class ProcessCompletionConditionDefinition {
    private String condition;
    @JsonAlias("domainEvent")
    private String message;
    private ProcessCompletionConclusion conclusion;
    private String name;
}
