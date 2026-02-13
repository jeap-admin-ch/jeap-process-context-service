package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessRelationRoleType;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessRelationRoleVisibility;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProcessRelationStubs {

    public static ProcessRelation createProcessRelation(ProcessInstance processInstance, String name, String relatedProcessId) {
        return createProcessRelation(processInstance, name, relatedProcessId, ProcessRelationRoleVisibility.BOTH);
    }

    public static ProcessRelation createProcessRelation(ProcessInstance processInstance, String name, String relatedProcessId, ProcessRelationRoleVisibility visibility) {
        return ProcessRelation.builder()
                .processInstance(processInstance)
                .name(name)
                .roleType(ProcessRelationRoleType.ORIGIN)
                .originRole("originRole")
                .targetRole("targetRole")
                .visibilityTyp(visibility)
                .relatedProcessId(relatedProcessId)
                .build();
    }
}
