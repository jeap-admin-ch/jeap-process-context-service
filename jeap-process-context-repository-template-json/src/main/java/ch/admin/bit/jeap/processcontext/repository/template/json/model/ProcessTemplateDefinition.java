package ch.admin.bit.jeap.processcontext.repository.template.json.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessTemplateDefinition {

    private String name;
    @JsonAlias("events")
    private List<MessageReferenceDefinition> messages = new ArrayList<>();
    private List<TaskTypeDefinition> tasks = new ArrayList<>();
    private List<ProcessDataDefinition> processData = new ArrayList<>();
    private String relationSystemId;
    private List<RelationPatternDefinition> relationPatterns = new ArrayList<>();
    private List<ProcessRelationPatternDefinition> processRelationPatterns = new ArrayList<>();
    private List<ProcessCompletionDefinition> completions = new ArrayList<>();
    private List<ProcessSnapshotDefinition> snapshots = new ArrayList<>();
}
