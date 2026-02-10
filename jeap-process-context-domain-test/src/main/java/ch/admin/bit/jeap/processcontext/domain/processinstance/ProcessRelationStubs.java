package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessRelationRoleType;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessRelationRoleVisibility;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProcessRelationStubs {

    public static ProcessRelation createProcessRelation(ProcessInstance processInstance, String name, String relatedProcessId) {
        ProcessRelation processRelation = ProcessRelation.builder()
                .processInstance(processInstance)
                .name(name)
                .roleType(ProcessRelationRoleType.ORIGIN)
                .originRole("originRole")
                .targetRole("targetRole")
                .visibilityTyp(ProcessRelationRoleVisibility.BOTH)
                .relatedProcessId(relatedProcessId)
                .build();
        return processRelation;
    }
}
