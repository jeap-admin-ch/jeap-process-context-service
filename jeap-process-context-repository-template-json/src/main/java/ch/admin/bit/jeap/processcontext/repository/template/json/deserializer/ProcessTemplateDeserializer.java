package ch.admin.bit.jeap.processcontext.repository.template.json.deserializer;

import ch.admin.bit.jeap.processcontext.domain.processtemplate.*;
import ch.admin.bit.jeap.processcontext.plugin.api.condition.*;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessCompletionConclusion;
import ch.admin.bit.jeap.processcontext.plugin.api.event.*;
import ch.admin.bit.jeap.processcontext.repository.template.json.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static ch.admin.bit.jeap.processcontext.repository.template.json.model.RelationPatternDefinition.JOIN_BY_ROLE;
import static ch.admin.bit.jeap.processcontext.repository.template.json.model.RelationPatternDefinition.JOIN_BY_VALUE;
import static java.util.stream.Collectors.toMap;
import static org.springframework.util.StringUtils.hasText;

@RequiredArgsConstructor
public class ProcessTemplateDeserializer {

    private final ProcessTemplateDefinition templateDefinition;
    private final String templateHash;

    public ProcessTemplate toProcessTemplate() {
        String name = templateDefinition.getName();
        if (!hasText(name)) {
            throw TemplateDefinitionException.emptyProcessName();
        }

        List<RelationPattern> relationPatterns = relationPatterns();
        String relationSystemId = templateDefinition.getRelationSystemId();

        if (!relationPatterns.isEmpty() && !StringUtils.hasText(relationSystemId)) {
            throw TemplateDefinitionException.missingRelationSystemId();
        }

        List<ProcessRelationPattern> processRelationPatterns = processRelations();

        validateCompletedByEvents(templateDefinition);
        validateObserves(templateDefinition);
        validatePlannedByEvents(templateDefinition);
        validateProcessCompletedByEvents(templateDefinition);
        validateProcessRelations(templateDefinition);


        return ProcessTemplate.builder()
                .name(name)
                .templateHash(templateHash)
                .messageReferences(messageReferences())
                .taskTypes(taskTypes())
                .milestones(milestones())
                .processDataTemplates(processDatas())
                .relationSystemId(relationSystemId)
                .relationPatterns(relationPatterns)
                .processRelationPatterns(processRelationPatterns)
                .processCompletionConditions(processCompletionConditions())
                .processSnapshotConditions(processSnapshotConditions())
                .build();
    }

    private void validateObserves(ProcessTemplateDefinition processTemplate) {
        Set<String> definedEventNames = getDefinedEventNames(processTemplate);

        Set<String> eventsMissingInTemplate = processTemplate.getTasks().stream()
                .map(TaskTypeDefinition::getObserves)
                .filter(Objects::nonNull)
                .map(TaskObservesConditionDefinition::getMessage)
                .filter(Objects::nonNull)
                .filter(completedByEvent -> !definedEventNames.contains(completedByEvent))
                .collect(Collectors.toSet());

        if (!eventsMissingInTemplate.isEmpty()) {
            throw TemplateDefinitionException.observesEventNotDefined(processTemplate.getName(), eventsMissingInTemplate);
        }
    }

    private void validateCompletedByEvents(ProcessTemplateDefinition processTemplate) {
        Set<String> definedEventNames = getDefinedEventNames(processTemplate);

        Set<String> eventsMissingInTemplate = processTemplate.getTasks().stream()
                .map(TaskTypeDefinition::getCompletedBy)
                .filter(Objects::nonNull)
                .map(TaskCompletionConditionDefinition::getMessage)
                .filter(Objects::nonNull)
                .filter(completedByEvent -> !definedEventNames.contains(completedByEvent))
                .collect(Collectors.toSet());

        if (!eventsMissingInTemplate.isEmpty()) {
            throw TemplateDefinitionException.completedByEventNotDefined(processTemplate.getName(), eventsMissingInTemplate);
        }
    }

    private void validatePlannedByEvents(ProcessTemplateDefinition processTemplate) {
        Set<String> definedEventNames = getDefinedEventNames(processTemplate);
        Set<String> eventsMissingInTemplate = processTemplate.getTasks().stream()
                .map(TaskTypeDefinition::getPlannedBy)
                .filter(Objects::nonNull)
                .map(TaskPlannedByConditionDefinition::getMessage)
                .filter(Objects::nonNull)
                .filter(plannedByEvent -> !definedEventNames.contains(plannedByEvent))
                .collect(Collectors.toSet());

        if (!eventsMissingInTemplate.isEmpty()) {
            throw TemplateDefinitionException.plannedByEventNotDefined(processTemplate.getName(), eventsMissingInTemplate);
        }
    }

    private void validateProcessCompletedByEvents(ProcessTemplateDefinition processTemplate) {
        Set<String> definedEventNames = getDefinedEventNames(processTemplate);
        Set<String> eventsMissingInTemplate = processTemplate.getCompletions().stream()
                .map(ProcessCompletionDefinition::getCompletedBy)
                .filter(Objects::nonNull)
                .map(ProcessCompletionConditionDefinition::getMessage)
                .filter(Objects::nonNull)
                .filter(processCompletedByEvent -> !definedEventNames.contains(processCompletedByEvent))
                .collect(Collectors.toSet());
        if (!eventsMissingInTemplate.isEmpty()) {
            throw TemplateDefinitionException.processCompletedByEventNotDefined(processTemplate.getName(), eventsMissingInTemplate);
        }
    }

    private void validateProcessRelations(ProcessTemplateDefinition processTemplate) {
        List<ProcessRelationPatternDefinition> processRelationPatternDefinitionList = processTemplate.getProcessRelationPatterns();

        ProcessTemplateUtils.validateUniqueNames("processRelationPatterns",
                processRelationPatternDefinitionList.stream().map(ProcessRelationPatternDefinition::getName));

        ProcessTemplateUtils.validateMessageName(templateDefinition.getMessages(), processRelationPatternDefinitionList);

    }

    private static Set<String> getDefinedEventNames(ProcessTemplateDefinition processTemplate) {
        return processTemplate.getMessages().stream()
                .map(MessageReferenceDefinition::getMessageName)
                .collect(Collectors.toSet());
    }

    private MessageReference toDomainEventReference(MessageReferenceDefinition eventRefDefinition) {
        String messageName = eventRefDefinition.getMessageName();
        String topicName = eventRefDefinition.getTopicName();
        String clusterName = eventRefDefinition.getClusterName();
        String correlationProvider = eventRefDefinition.getCorrelationProvider();
        String payloadExtractor = eventRefDefinition.getPayloadExtractor();
        String referenceExtractor = eventRefDefinition.getReferenceExtractor();
        String processInstanceCondition = getProcessInstantiationCondition(eventRefDefinition);
        CorrelatedByProcessData correlatedByProcessData = toCorrelatedByProcessData(eventRefDefinition.getCorrelatedBy());

        if (!hasText(messageName)) {
            throw TemplateDefinitionException.emptyEventName();
        }
        if (!hasText(topicName)) {
            throw TemplateDefinitionException.emptyTopicName();
        }
        if (!hasText(correlationProvider)) {
            correlationProvider = MessageProcessIdCorrelationProvider.class.getName();
        }
        if (!hasText(payloadExtractor)) {
            payloadExtractor = EmptySetPayloadExtractor.class.getName();
        }
        if (!hasText(referenceExtractor)) {
            referenceExtractor = EmptySetReferenceExtractor.class.getName();
        }
        return MessageReference.builder()
                .messageName(messageName)
                .topicName(topicName)
                .clusterName(clusterName)
                .correlationProvider(ProcessTemplateUtils.newInstance(correlationProvider))
                .payloadExtractor(ProcessTemplateUtils.newInstance(payloadExtractor))
                .referenceExtractor(ProcessTemplateUtils.newInstance(referenceExtractor))
                .processInstantiationCondition(ProcessTemplateUtils.newInstance(processInstanceCondition))
                .correlatedByProcessData(correlatedByProcessData)
                .build();
    }

    private String getProcessInstantiationCondition(MessageReferenceDefinition eventRefDefinition) {
        if (eventRefDefinition.getTriggersProcessInstantiation() != null && !eventRefDefinition.getTriggersProcessInstantiation()) {
            // process creation explicitly disabled
            return NeverProcessInstantiationCondition.class.getName();
        }
        if (hasText(eventRefDefinition.getProcessInstantiationCondition())) {
            // process instantiation condition configured
            return eventRefDefinition.getProcessInstantiationCondition();
        }
        if (eventRefDefinition.getTriggersProcessInstantiation() != null) {
            return AlwaysProcessInstantiationCondition.class.getName();
        } else {
            return NeverProcessInstantiationCondition.class.getName();
        }
    }

    private CorrelatedByProcessData toCorrelatedByProcessData(CorrelatedByProcessDataDefinition definition) {
        if (definition == null) {
            return null;
        }

        if (!hasText(definition.getProcessDataKey())) {
            throw TemplateDefinitionException.emptyProcessDataKeyInCorrelatedBy();
        }
        if (!hasText(definition.getMessageDataKey())) {
            throw TemplateDefinitionException.emptyMessgaeDataKeyInCorrelatedBy();
        }

        return CorrelatedByProcessData.builder()
                .processDataKey(definition.getProcessDataKey())
                .messageDataKey(definition.getMessageDataKey())
                .build();
    }

    private MilestoneCondition toMilestoneCondition(MilestoneDefinition milestoneDefinition) {
        MilestoneConditionDefinition reachedWhen = milestoneDefinition.getReachedWhen();
        if (reachedWhen == null) {
            throw TemplateDefinitionException.missingMilestoneConditionDefinition(milestoneDefinition.getName());
        }

        Set<String> tasksCompleted = reachedWhen.getTasksCompleted() == null ? Set.of() : reachedWhen.getTasksCompleted();
        String condition = reachedWhen.getCondition();

        if (tasksCompleted.isEmpty() && !hasText(condition)) {
            throw TemplateDefinitionException.emptyMilestoneConditionDefinition();
        }
        if (!tasksCompleted.isEmpty() && hasText(condition)) {
            throw TemplateDefinitionException.invalidMilestoneConditionDefinition();
        }

        if (!tasksCompleted.isEmpty()) {
            return new TasksCompletedMilestoneCondition(tasksCompleted);
        } else {
            return ProcessTemplateUtils.newInstance(condition);
        }
    }

    private String toTaskCompletedByDomainEvent(TaskCompletionConditionDefinition definition) {
        String domainEvent = definition.getMessage();
        if (!hasText(domainEvent)) {
            throw TemplateDefinitionException.emptyTaskCompletionConditionDefinition();
        }
        return domainEvent;
    }

    private TaskInstantiationCondition toTaskInstantiationCondition(String domainEvent, String condition) {
        if (!hasText(domainEvent) && !hasText(condition)) {
            throw TemplateDefinitionException.emptyTaskInstantiationConditionDefinition();
        }
        if (!hasText(domainEvent)) {
            throw TemplateDefinitionException.invalidTaskInstantiationDefinition();
        }

        return hasText(condition) ? ProcessTemplateUtils.newInstance(condition) : null;
    }

    private List<ProcessCompletionCondition> processCompletionConditions() {
        return templateDefinition.getCompletions().stream()
                .map(ProcessCompletionDefinition::getCompletedBy)
                .filter(Objects::nonNull)
                .map(this::toProcessCompletionCondition)
                .toList();
    }

    private ProcessCompletionCondition toProcessCompletionCondition(ProcessCompletionConditionDefinition processCompletionDefinition) {
        if (hasText(processCompletionDefinition.getCondition()) && hasText(processCompletionDefinition.getMessage())) {
            throw TemplateDefinitionException.processMustBeCompletedEitherByConditionOrDomainEvent();
        }
        if (hasText(processCompletionDefinition.getCondition())) {
            if (processCompletionDefinition.getConclusion() != null) {
                throw TemplateDefinitionException.processCompletionByConditionDoesNotSupportAdditionalAttributes();
            }
            return ProcessTemplateUtils.newInstance(processCompletionDefinition.getCondition());
        } else if (hasText(processCompletionDefinition.getMessage())) {
            if ((processCompletionDefinition.getConclusion() == null) || !hasText(processCompletionDefinition.getName())) {
                throw TemplateDefinitionException.processCompletionByDomainEventMustProvideConclusionAndName(processCompletionDefinition.getMessage());
            }
            return new MessageProcessCompletionCondition(
                    processCompletionDefinition.getMessage(),
                    processCompletionDefinition.getConclusion(),
                    processCompletionDefinition.getName());
        } else {
            throw TemplateDefinitionException.processMustBeCompletedEitherByConditionOrDomainEvent();
        }
    }

    TaskType toTaskType(TaskTypeDefinition definition, int taskIndex) {
        String name = definition.getName();
        TaskCompletionConditionDefinition completedBy = definition.getCompletedBy();
        TaskPlannedByConditionDefinition plannedBy = definition.getPlannedBy();
        TaskObservesConditionDefinition observes = definition.getObserves();
        Set<TaskData> taskData = toTaskData(definition.getTaskData());

        if (!hasText(name)) {
            throw TemplateDefinitionException.emptyTaskName();
        }

        validateLifecycleCardinalityConfiguration(definition.getLifecycle(), definition.getCardinality(), name);
        validateObserverTaskConfiguration(definition);
        validateTaskInstantiationConfiguration(definition);
        validateTaskDataConfiguration(taskData, definition);

        return TaskType.builder()
                .name(name)
                .index(taskIndex)
                .lifecycle(definition.getLifecycle())
                .cardinality(toTaskCardinality(definition.getCardinality()))
                .completedByDomainEvent(completedBy == null ? null : toTaskCompletedByDomainEvent(completedBy))
                .plannedByDomainEvent(plannedBy == null ? null : plannedBy.getMessage())
                .instantiationCondition(getInstantiationCondition(plannedBy, observes))
                .observesMessage(observes == null ? null : observes.getMessage())
                .taskData(taskData)
                .build();
    }

    private Set<TaskData> toTaskData(Set<TaskDataDefinition> definitions) {
        if (definitions == null) {
            return null;
        }
        return definitions.stream().map(this::toTaskData).collect(Collectors.toSet());
    }

    private TaskData toTaskData(TaskDataDefinition definition) {
        String sourceEvent = definition.getSourceMessage();
        Set<String> eventDataKeys = definition.getMessageDataKeys();

        if (!hasText(sourceEvent)) {
            throw TemplateDefinitionException.missingTaskDataSourceEvent();
        }
        if ((eventDataKeys == null) || eventDataKeys.isEmpty()) {
            throw TemplateDefinitionException.missingOrEmptyTaskDataEventDataKeys();
        }

        return TaskData.builder().
                sourceMessage(definition.getSourceMessage()).
                messageDataKeys(definition.getMessageDataKeys()).
                build();
    }

    private TaskInstantiationCondition getInstantiationCondition(TaskPlannedByConditionDefinition plannedBy, TaskObservesConditionDefinition observes) {
        if (plannedBy != null) {
            return toTaskInstantiationCondition(plannedBy.getMessage(), plannedBy.getCondition());
        }
        if (observes != null) {
            return toTaskInstantiationCondition(observes.getMessage(), observes.getCondition());
        }
        return null;
    }

    void validateObserverTaskConfiguration(TaskTypeDefinition definition) {
        TaskCompletionConditionDefinition completedBy = definition.getCompletedBy();
        TaskPlannedByConditionDefinition plannedBy = definition.getPlannedBy();
        TaskObservesConditionDefinition observes = definition.getObserves();

        if ((observes != null) && (plannedBy != null)) {
            throw TemplateDefinitionException.observesAndPlannedByTaskNotTogether(templateDefinition.getName(), definition.getName());
        }

        if ((observes != null) && (completedBy != null)) {
            throw TemplateDefinitionException.observesAndCompletedByTaskNotTogether(templateDefinition.getName(), definition.getName());
        }
    }

    void validateTaskInstantiationConfiguration(TaskTypeDefinition definition) {
        TaskPlannedByConditionDefinition plannedBy = definition.getPlannedBy();
        if (plannedBy != null && TaskLifecycle.STATIC.equals(definition.getLifecycle())) {
            throw TemplateDefinitionException.staticTasksCannotBePlanned(templateDefinition.getName(), definition.getName());
        }
    }

    void validateLifecycleCardinalityConfiguration(TaskLifecycle lifecycle, String cardinalityStr, String taskName) {
        if (lifecycle == null) {
            throw TemplateDefinitionException.emptyTaskTypeLifecycle(taskName);
        }
        if (TaskTypeDefinition.isLegacyCardinalityName(cardinalityStr)) {
            throw TemplateDefinitionException.lifecycleWithDeprecatedCardinailty(cardinalityStr, taskName);
        }
        final TaskCardinality cardinality = toTaskCardinality(cardinalityStr);
        if (!lifecycle.supportsCardinality(cardinality)) {
            throw TemplateDefinitionException.unsupportedCardinalityForLifecycle(cardinality, lifecycle);
        }
    }

    private static void validateTaskDataConfiguration(Set<TaskData> taskData, TaskTypeDefinition definition) {
        if (taskData == null) {
            // task data not mandatory
            return;
        }
        Set<String> planningOrCompletingMessages = getPlanningOrCompletingMessages(definition);
        Set<String> illegalSourceMessages = taskData.stream().
                map(TaskData::getSourceMessage).
                filter(sourceMessage -> !planningOrCompletingMessages.contains(sourceMessage)).
                collect(Collectors.toSet());
        if (!illegalSourceMessages.isEmpty()) {
            throw TemplateDefinitionException.taskDataSourceMessagesNotPlanningOrCompletingTheTask(illegalSourceMessages);
        }
    }

    private static Set<String> getPlanningOrCompletingMessages(TaskTypeDefinition definition) {
        Set<String> planningOrCompletingMessages = new HashSet<>();
        if ((definition.getPlannedBy() != null) && hasText(definition.getPlannedBy().getMessage())) {
            planningOrCompletingMessages.add(definition.getPlannedBy().getMessage());
        }
        if ((definition.getCompletedBy() != null) && hasText(definition.getCompletedBy().getMessage())) {
            planningOrCompletingMessages.add(definition.getCompletedBy().getMessage());
        }
        if ((definition.getObserves() != null) && hasText(definition.getObserves().getMessage())) {
            planningOrCompletingMessages.add(definition.getObserves().getMessage());
        }
        return planningOrCompletingMessages;
    }

    private TaskCardinality toTaskCardinality(String cardinalityStr) {
        try {
            return TaskCardinality.valueOf(cardinalityStr.toUpperCase().replace("-", "_"));
        } catch (Exception e) {
            throw TemplateDefinitionException.invalidCardinality(cardinalityStr);
        }
    }


    private List<TaskType> taskTypes() {
        List<TaskType> taskTypeList = new ArrayList<>();
        int taskIndex = 0;
        for (TaskTypeDefinition taskTypeDefinition : templateDefinition.getTasks()) {
            TaskType taskType = toTaskType(taskTypeDefinition, taskIndex++);
            taskTypeList.add(taskType);
        }
        ProcessTemplateUtils.validateUniqueNames("task", taskTypeList.stream().map(TaskType::getName));
        ProcessTemplateUtils.validateEventReferences(templateDefinition);
        return taskTypeList;
    }

    private Map<String, MilestoneCondition> milestones() {
        List<MilestoneDefinition> milestones = templateDefinition.getMilestones();
        ProcessTemplateUtils.validateUniqueNames("milestone", milestones.stream().map(MilestoneDefinition::getName));
        return milestones.stream()
                .collect(toMap(MilestoneDefinition::getName, this::createValidatedMilestoneCondition));
    }

    private MilestoneCondition createValidatedMilestoneCondition(MilestoneDefinition milestoneDefinition) {
        MilestoneCondition milestoneCondition = toMilestoneCondition(milestoneDefinition);
        if (milestoneCondition instanceof TasksCompletedMilestoneCondition tasksCompletedMilestoneCondition) {
            Set<String> referencedTaskNames = tasksCompletedMilestoneCondition.getTaskNames();
            ProcessTemplateUtils.validateTasksExist(templateDefinition, referencedTaskNames, milestoneDefinition.getName());
        }
        return milestoneCondition;
    }

    private List<MessageReference> messageReferences() {
        List<MessageReference> messageReferences = templateDefinition.getMessages().stream()
                .map(this::toDomainEventReference)
                .toList();
        ProcessTemplateUtils.validateUniqueNames("message", messageReferences.stream()
                .map(MessageReference::getMessageName));
        return messageReferences;
    }

    private List<ProcessDataTemplate> processDatas() {
        List<ProcessDataDefinition> processDataDefinitions = templateDefinition.getProcessData();
        ProcessTemplateUtils.validateEventName(templateDefinition.getMessages(), processDataDefinitions);
        return processDataDefinitions.stream()
                .map(ProcessTemplateDeserializer::toProcessDataTemplate)
                .toList();
    }

    private List<RelationPattern> relationPatterns() {
        List<RelationPatternDefinition> relationPatternDefinitions = templateDefinition.getRelationPatterns();
        return relationPatternDefinitions.stream()
                .map(this::toRelationPattern)
                .toList();
    }

    private List<ProcessRelationPattern> processRelations() {
        List<ProcessRelationPatternDefinition> processRelationPatternDefinitions = templateDefinition.getProcessRelationPatterns();
        return processRelationPatternDefinitions.stream()
                .map(this::toProcessRelation)
                .toList();
    }

    static ProcessDataTemplate toProcessDataTemplate(ProcessDataDefinition processDataDefinition) {
        String key = processDataDefinition.getKey();
        ProcessDataSourceDefinition source = processDataDefinition.getSource();

        if (!hasText(key)) {
            throw TemplateDefinitionException.emptyProcessDataKey();
        }
        if (source == null) {
            throw TemplateDefinitionException.emptyProcessDataSource();
        }

        return ProcessDataTemplate.builder()
                .key(key)
                .sourceMessageName(source.getMessage())
                .sourceMessageDataKey(source.getMessageDataKey())
                .build();
    }

    RelationPattern toRelationPattern(RelationPatternDefinition relationPatternDefinition) {
        String predicateType = relationPatternDefinition.getPredicateType();
        String joinType = relationPatternDefinition.getJoinType();
        RelationNodeDefinition subject = relationPatternDefinition.getSubject();
        RelationNodeDefinition object = relationPatternDefinition.getObject();

        if (!hasText(predicateType)) {
            throw TemplateDefinitionException.emptyPredicateType();
        }
        if (hasText(joinType)) {
            if (!joinType.equals(JOIN_BY_ROLE) && !joinType.equals(JOIN_BY_VALUE)) {
                throw TemplateDefinitionException.unsupportedRelationJoinType(joinType);
            }
        }
        if (subject == null) {
            throw TemplateDefinitionException.emptyRelationNode(predicateType);
        }
        if (object == null) {
            throw TemplateDefinitionException.emptyRelationNode(predicateType);
        }

        RelationNodeSelector subjectSelector = toSelector(subject);
        RelationNodeSelector objectSelector = toSelector(object);
        return RelationPattern.builder()
                .predicateType(predicateType)
                .joinType(joinType)
                .subjectSelector(subjectSelector)
                .objectSelector(objectSelector)
                .build();
    }

    private RelationNodeSelector toSelector(RelationNodeDefinition nodeDefinition) {
        if (!hasText(nodeDefinition.getType())) {
            throw TemplateDefinitionException.emptyRelationNodeType();
        }
        if (nodeDefinition.getSelector() == null) {
            throw TemplateDefinitionException.emptyRelationNodeSelector(nodeDefinition.getType());
        }
        String processDataKey = nodeDefinition.getSelector().getProcessDataKey();
        if (!hasText(processDataKey)) {
            throw TemplateDefinitionException.emptyRelationSelectorProcessDataKey(nodeDefinition.getType());
        }
        ProcessTemplateUtils.validateProcessDataKey(templateDefinition.getProcessData(), processDataKey);

        return RelationNodeSelector.builder()
                .type(nodeDefinition.getType())
                .processDataKey(processDataKey)
                .processDataRole(nodeDefinition.getSelector().getRole())
                .build();
    }

    ProcessRelationPattern toProcessRelation(ProcessRelationPatternDefinition processRelationPatternDefinition) {
        String name = processRelationPatternDefinition.getName();
        ProcessRelationRoleType processRelationRoleType = processRelationPatternDefinition.getRoleType();
        String originRole = processRelationPatternDefinition.getOriginRole();
        String targetRole = processRelationPatternDefinition.getTargetRole();
        ProcessRelationRoleVisibility visibility = processRelationPatternDefinition.getVisibility();
        ProcessRelationSourceDefinition source = processRelationPatternDefinition.getSource();

        if (!hasText(name)) {
            throw TemplateDefinitionException.emptyProcessRelationProperty("name");
        }

        if (processRelationRoleType == null) {
            throw TemplateDefinitionException.emptyProcessRelationRoleType();
        }

        if (!hasText(originRole)) {
            throw TemplateDefinitionException.emptyProcessRelationProperty("orginRole");
        }

        if (!hasText(targetRole)) {
            throw TemplateDefinitionException.emptyProcessRelationProperty("targetRole");
        }

        if (visibility == null) {
            throw TemplateDefinitionException.emptyProcessRelationVisibility();
        }

        if (source == null) {
            throw TemplateDefinitionException.emptyProcessRelationSource();
        }

        if (!hasText(source.getMessageName())) {
            throw TemplateDefinitionException.emptyProcessRelationProperty("messageName");
        }

        if (!hasText(source.getMessageDataKey())) {
            throw TemplateDefinitionException.emptyProcessRelationProperty("messageDataKey");
        }


        return ProcessRelationPattern.builder()
                .name(name)
                .roleType(processRelationRoleType)
                .originRole(originRole)
                .targetRole(targetRole)
                .visibility(visibility)
                .source(ProcessRelationSource.builder()
                        .messageName(source.getMessageName())
                        .messageDataKey(source.getMessageDataKey())
                        .build())
                .build();
    }

    private List<ProcessSnapshotCondition> processSnapshotConditions() {
        return templateDefinition.getSnapshots().stream()
                .map(ProcessSnapshotDefinition::getCreatedOn)
                .filter(Objects::nonNull)
                .map(this::toProcessSnapshotCondition)
                .toList();
    }

    private ProcessSnapshotCondition toProcessSnapshotCondition(ProcessSnapshotConditionDefinition processSnapshotDefinition) {
        if (hasText(processSnapshotDefinition.getCondition()) && hasText(processSnapshotDefinition.getCompletion())) {
            throw TemplateDefinitionException.processSnapshotMustBeCreatedEitherByConditionOrCompletion();
        }
        if (hasText(processSnapshotDefinition.getCondition())) {
            return ProcessTemplateUtils.newInstance(processSnapshotDefinition.getCondition());
        } else if (hasText(processSnapshotDefinition.getCompletion())) {
            final String completionConclusion = processSnapshotDefinition.getCompletion();
            if ("any".equalsIgnoreCase(completionConclusion)) {
                return new ProcessCompletionProcessSnapshotCondition(null);
            } else {
                return new ProcessCompletionProcessSnapshotCondition(parseCompletionConclusion(completionConclusion));
            }
        } else {
            throw TemplateDefinitionException.processSnapshotMustBeCreatedEitherByConditionOrCompletion();
        }
    }

    private ProcessCompletionConclusion parseCompletionConclusion(String completionConclusion) {
        try {
            return ProcessCompletionConclusion.valueOf(completionConclusion.toUpperCase());
        } catch (Exception e) {
            throw TemplateDefinitionException.invalidProcessSnapshotCompletion(completionConclusion);
        }
    }

}
