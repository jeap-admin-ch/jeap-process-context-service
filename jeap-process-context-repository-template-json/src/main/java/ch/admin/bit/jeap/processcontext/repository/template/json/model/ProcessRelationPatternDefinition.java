package ch.admin.bit.jeap.processcontext.repository.template.json.model;

import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessRelationRoleType;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessRelationRoleVisibility;
import lombok.Data;

@Data
public class ProcessRelationPatternDefinition {

    private String name;
    private ProcessRelationRoleType roleType;
    private String originRole;
    private String targetRole;
    private ProcessRelationRoleVisibility visibility;
    private ProcessRelationSourceDefinition source;
}

