package ch.admin.bit.jeap.processcontext.domain.processtemplate;

import ch.admin.bit.jeap.processcontext.plugin.api.condition.AllTasksInFinalStateProcessCompletionCondition;
import ch.admin.bit.jeap.processcontext.plugin.api.condition.MilestoneCondition;
import ch.admin.bit.jeap.processcontext.plugin.api.condition.ProcessCompletionCondition;
import ch.admin.bit.jeap.processcontext.plugin.api.condition.ProcessSnapshotCondition;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

@ToString(onlyExplicitlyIncluded = true)
public final class ProcessTemplate {

    /**
     * Technical, internal name of the process (must be unique)
     */
    @Getter
    @ToString.Include
    private final String name;

    /**
     * Current template definition hash (taken from the canonical JSON contents of the template definition)
     */
    @Getter
    @ToString.Include
    private final String templateHash;

    private final List<TaskType> taskTypes;

    private final List<MessageReference> messageReferences;

    private final Map<String, TaskType> taskTypesByName;

    private final Map<String, MilestoneCondition> milestones;

    private final List<ProcessCompletionCondition> processCompletionConditions;

    private final List<ProcessDataTemplate> processDataTemplates;

    private final Map<String, List<ProcessDataTemplate>> processDataTemplatesBySourceEventName;

    @Getter
    private final String relationSystemId;

    private final List<RelationPattern> relationPatterns;

    private final List<ProcessRelationPattern> processRelationPatterns;

    private final List<ProcessSnapshotCondition> processSnapshotConditions;

    @Builder
    private ProcessTemplate(@NonNull String name, @NonNull String templateHash,
                            @NonNull List<TaskType> taskTypes,
                            List<MessageReference> messageReferences,
                            Map<String, MilestoneCondition> milestones,
                            List<ProcessDataTemplate> processDataTemplates,
                            String relationSystemId,
                            List<RelationPattern> relationPatterns,
                            List<ProcessCompletionCondition> processCompletionConditions,
                            List<ProcessRelationPattern> processRelationPatterns,
                            List<ProcessSnapshotCondition> processSnapshotConditions) {
        this.name = name;
        this.templateHash = templateHash;
        if (taskTypes.isEmpty()) {
            throw ProcessTemplateException.createEmptyProcessTemplate(name);
        }
        this.taskTypes = taskTypes;
        this.taskTypesByName = taskTypes.stream()
                .collect(toMap(TaskType::getName, Function.identity()));
        this.milestones = Objects.requireNonNullElseGet(milestones, Collections::emptyMap);
        this.messageReferences = Objects.requireNonNullElseGet(messageReferences, List::of);
        this.processDataTemplates = Objects.requireNonNullElseGet(processDataTemplates, List::of);
        this.processDataTemplatesBySourceEventName = this.processDataTemplates.stream()
                .collect(groupingBy(ProcessDataTemplate::getSourceMessageName));
        this.relationSystemId = relationSystemId;
        this.relationPatterns = Objects.requireNonNullElseGet(relationPatterns, List::of);
        if ((processCompletionConditions == null) || processCompletionConditions.isEmpty()) {
            this.processCompletionConditions = List.of(new AllTasksInFinalStateProcessCompletionCondition());
        } else {
            this.processCompletionConditions = List.copyOf(processCompletionConditions);
        }
        this.processRelationPatterns = processRelationPatterns;
        this.processSnapshotConditions = processSnapshotConditions != null ? processSnapshotConditions : List.of();
    }

    public List<TaskType> getTaskTypes() {
        return Collections.unmodifiableList(taskTypes);
    }

    public List<MessageReference> getMessageReferences() {
        return Collections.unmodifiableList(messageReferences);
    }

    public Optional<TaskType> getTaskTypeByName(String taskTypeName) {
        return Optional.ofNullable(taskTypesByName.get(taskTypeName));
    }

    public Set<String> getMilestoneNames() {
        return Set.copyOf(milestones.keySet());
    }

    public Optional<MilestoneCondition> getMilestoneConditionByMilestoneName(String name) {
        return Optional.ofNullable(milestones.get(name));
    }

    public List<ProcessCompletionCondition> getProcessCompletionConditions() {
        return processCompletionConditions;
    }


    public List<ProcessDataTemplate> getProcessDataTemplates() {
        return Collections.unmodifiableList(processDataTemplates);
    }

    public List<ProcessDataTemplate> getProcessDataTemplatesBySourceMessageName(String eventName) {
        return processDataTemplatesBySourceEventName.getOrDefault(eventName, List.of());
    }

    public List<RelationPattern> getRelationPatterns() {
        return Collections.unmodifiableList(relationPatterns);
    }

    public Set<MessageReference> getDomainEventReferencesCorrelatedBy(String processDataKey) {
        Set<ProcessDataTemplate> processDataTemplatesWithProcessDataKey = this.getProcessDataTemplates().stream()
                .filter(pdt -> pdt.getKey().equals(processDataKey))
                .collect(Collectors.toSet());

        Set<MessageReference> messageDataKey = new HashSet<>();
        for (ProcessDataTemplate processDataTemplate : processDataTemplatesWithProcessDataKey) {
            messageDataKey.addAll(this.getMessageReferences().stream()
                    .filter(der -> der.getCorrelatedByProcessData() != null
                            && der.getCorrelatedByProcessData().getProcessDataKey().equals(processDataTemplate.getKey())
                            && der.getCorrelatedByProcessData().getMessageDataKey().equals(processDataTemplate.getSourceMessageDataKey()))
                    .collect(Collectors.toSet()));
        }
        return messageDataKey;
    }

    public Set<String> getTaskNames() {
        return getTaskTypes().stream().map(TaskType::getName).collect(Collectors.toUnmodifiableSet());
    }

    public List<ProcessRelationPattern> getProcessRelationPatterns() {
        return Collections.unmodifiableList(processRelationPatterns);
    }

    public List<ProcessSnapshotCondition> getProcessSnapshotConditions() {
        return Collections.unmodifiableList(processSnapshotConditions);
    }

}
