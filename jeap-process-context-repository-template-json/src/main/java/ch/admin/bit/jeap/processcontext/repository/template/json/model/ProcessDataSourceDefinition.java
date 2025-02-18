package ch.admin.bit.jeap.processcontext.repository.template.json.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class ProcessDataSourceDefinition {

    @JsonAlias("eventName")
    private String message;

    @JsonAlias("eventDataKey")
    private String messageDataKey;
}
