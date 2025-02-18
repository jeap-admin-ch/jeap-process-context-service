package ch.admin.bit.jeap.processcontext.repository.template.json.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessSnapshotConditionDefinition {
    private String condition;
    private String completion;
}
