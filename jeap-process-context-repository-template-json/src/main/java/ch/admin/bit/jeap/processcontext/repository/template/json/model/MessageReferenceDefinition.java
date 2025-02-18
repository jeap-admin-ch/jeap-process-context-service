package ch.admin.bit.jeap.processcontext.repository.template.json.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class MessageReferenceDefinition {

    @JsonAlias("eventName")
    private String messageName;
    private String topicName;
    private String clusterName;
    private String correlationProvider;
    private String payloadExtractor;
    private String referenceExtractor;
    private CorrelatedByProcessDataDefinition correlatedBy;
    private Boolean triggersProcessInstantiation;
    private String processInstantiationCondition;
}
