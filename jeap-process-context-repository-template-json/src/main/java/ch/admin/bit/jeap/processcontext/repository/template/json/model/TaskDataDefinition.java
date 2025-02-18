package ch.admin.bit.jeap.processcontext.repository.template.json.model;

import lombok.Data;

import java.util.Set;

@Data
public class TaskDataDefinition {
    String sourceMessage;
    Set<String> messageDataKeys;
}
