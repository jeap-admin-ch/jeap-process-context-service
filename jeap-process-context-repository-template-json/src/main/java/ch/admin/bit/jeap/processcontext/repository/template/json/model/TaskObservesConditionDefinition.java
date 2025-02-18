package ch.admin.bit.jeap.processcontext.repository.template.json.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class TaskObservesConditionDefinition {

    @JsonAlias("domainEvent")
    private String message;

    private String condition;
}
