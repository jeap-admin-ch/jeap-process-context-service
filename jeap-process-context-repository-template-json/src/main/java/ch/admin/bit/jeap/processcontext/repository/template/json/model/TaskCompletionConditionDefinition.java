package ch.admin.bit.jeap.processcontext.repository.template.json.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class TaskCompletionConditionDefinition {

    @JsonAlias("domainEvent")
    private String message;

}
