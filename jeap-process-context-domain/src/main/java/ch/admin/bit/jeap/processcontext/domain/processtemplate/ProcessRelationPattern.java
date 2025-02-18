package ch.admin.bit.jeap.processcontext.domain.processtemplate;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
public class ProcessRelationPattern {
    String name;
    ProcessRelationRoleType roleType;
    String originRole;
    String targetRole;
    ProcessRelationRoleVisibility visibility;
    ProcessRelationSource source;

    @Builder
    public ProcessRelationPattern(@NonNull String name,
                                  @NonNull ProcessRelationRoleType roleType,
                                  @NonNull String originRole,
                                  @NonNull String targetRole,
                                  @NonNull ProcessRelationRoleVisibility visibility,
                                  @NonNull ProcessRelationSource source) {
        this.name = name;
        this.roleType = roleType;
        this.originRole = originRole;
        this.targetRole = targetRole;
        this.visibility = visibility;
        this.source = source;
    }
}
