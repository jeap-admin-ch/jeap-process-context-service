package ch.admin.bit.jeap.processcontext.domain.processtemplate;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
public class RelationPattern {
    String predicateType;
    String joinType;
    RelationNodeSelector subjectSelector;
    RelationNodeSelector objectSelector;

    @Builder
    private RelationPattern(@NonNull String predicateType, String joinType, @NonNull RelationNodeSelector subjectSelector, @NonNull RelationNodeSelector objectSelector) {
        this.predicateType = predicateType;
        this.joinType = joinType;
        this.subjectSelector = subjectSelector;
        this.objectSelector = objectSelector;
    }
}
