package ch.admin.bit.jeap.processcontext.repository.template.json.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RelationSelectorDefinition {

    private String processDataKey;
    private String role;
}
