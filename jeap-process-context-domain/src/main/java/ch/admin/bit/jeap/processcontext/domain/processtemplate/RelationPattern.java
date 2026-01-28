package ch.admin.bit.jeap.processcontext.domain.processtemplate;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
public class RelationPattern {

    public enum JoinType {
        BY_VALUE,
        BY_ROLE;

        public static JoinType of(String joinType) {
            if (joinType == null) {
                return null;
            }
            return switch (joinType) {
                case "byValue" -> BY_VALUE;
                case "byRole" -> BY_ROLE;
                default -> throw new IllegalArgumentException("Unknown join type: " + joinType);
            };
        }
    }

    String predicateType;
    JoinType joinType;
    RelationNodeSelector subjectSelector;
    RelationNodeSelector objectSelector;
    String featureFlag;

    @Builder
    private RelationPattern(@NonNull String predicateType, JoinType joinType, @NonNull RelationNodeSelector subjectSelector, @NonNull RelationNodeSelector objectSelector, String featureFlag) {
        this.predicateType = predicateType;
        this.joinType = joinType;
        this.subjectSelector = subjectSelector;
        this.objectSelector = objectSelector;
        this.featureFlag = featureFlag;
    }
}
