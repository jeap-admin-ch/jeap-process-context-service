package ch.admin.bit.jeap.processcontext.repository.template.json.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessSnapshotDefinition {
    private ProcessSnapshotConditionDefinition createdOn;
}
