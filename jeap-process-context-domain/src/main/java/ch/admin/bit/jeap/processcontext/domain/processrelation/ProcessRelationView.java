package ch.admin.bit.jeap.processcontext.domain.processrelation;

import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessRelationRoleType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@AllArgsConstructor
@Builder(access = AccessLevel.PUBLIC)
public class ProcessRelationView {

    String relationName;
    String originRole;
    String targetRole;
    ProcessRelationRoleType relationRole;
    Map<String, String> relation;
    String processTemplateName;
    Map<String, String> processName;
    String processId;
    String processState;
}
