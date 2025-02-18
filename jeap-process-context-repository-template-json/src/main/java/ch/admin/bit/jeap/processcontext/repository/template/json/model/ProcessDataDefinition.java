package ch.admin.bit.jeap.processcontext.repository.template.json.model;

import lombok.Data;

@Data
public class ProcessDataDefinition {

    private String key;
    private ProcessDataSourceDefinition source;
}

