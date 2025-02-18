package ch.admin.bit.jeap.processcontext.repository.template.json.model;

import lombok.Data;

@Data
public class RelationPatternDefinition {

    public static final String JOIN_BY_VALUE = "byValue";
    public static final String JOIN_BY_ROLE = "byRole";

    private String predicateType;
    private String joinType;
    private RelationNodeDefinition subject;
    private RelationNodeDefinition object;
}

