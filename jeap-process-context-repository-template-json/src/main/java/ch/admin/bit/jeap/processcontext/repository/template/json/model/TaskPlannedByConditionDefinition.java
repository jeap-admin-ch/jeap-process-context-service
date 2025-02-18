package ch.admin.bit.jeap.processcontext.repository.template.json.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class TaskPlannedByConditionDefinition {

    @JsonAlias("domainEvent")
    private String message;

    private String condition;

}
