package ch.admin.bit.jeap.processcontext.repository.template.json.model;

import lombok.Data;

@Data
public class RelationNodeDefinition {

    private String type;
    private RelationSelectorDefinition selector;
}
