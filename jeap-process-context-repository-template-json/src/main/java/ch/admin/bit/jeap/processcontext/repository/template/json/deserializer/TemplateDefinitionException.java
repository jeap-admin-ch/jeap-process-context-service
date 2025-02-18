package ch.admin.bit.jeap.processcontext.repository.template.json.deserializer;

import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskCardinality;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskLifecycle;
import ch.admin.bit.jeap.processcontext.repository.template.json.model.RelationPatternDefinition;

import java.util.Collection;
import java.util.Set;

import static java.lang.String.format;

class TemplateDefinitionException extends RuntimeException {

    private TemplateDefinitionException(String message) {
        super(message);
    }

    private TemplateDefinitionException(String message, Exception cause) {
        super(message, cause);
    }

    static TemplateDefinitionException emptyProcessName() {
        return new TemplateDefinitionException("Missing mandatory process property 'name'");
    }

    static TemplateDefinitionException emptyTaskName() {
        return new TemplateDefinitionException("Missing mandatory task property 'name'");
    }

    static TemplateDefinitionException duplicateName(String namedObject, Collection<String> duplicateNames) {
        return new TemplateDefinitionException("Duplicate " + namedObject + " names: " + duplicateNames);
    }

    public static TemplateDefinitionException errorWhileCreatingInstance(String conditionClassName, Exception cause) {
        return new TemplateDefinitionException("Error while creating instance of " + conditionClassName, cause);
    }

    static TemplateDefinitionException emptyEventName() {
        return new TemplateDefinitionException("Missing mandatory event property 'eventName'");
    }

    static TemplateDefinitionException emptyTopicName() {
        return new TemplateDefinitionException("Missing mandatory event property 'topicName'");
    }

    static TemplateDefinitionException emptyTaskCompletionConditionDefinition() {
        return new TemplateDefinitionException("Task with empty completion condition");
    }

    static TemplateDefinitionException emptyTaskInstantiationConditionDefinition() {
        return new TemplateDefinitionException("Task with empty instantiation condition");
    }

    static TemplateDefinitionException invalidTaskInstantiationDefinition() {
        return new TemplateDefinitionException("Invalid property combination in task instantiation condition");
    }

    static TemplateDefinitionException emptyMilestoneConditionDefinition() {
        return new TemplateDefinitionException("Milestone with empty condition definition");
    }

    static TemplateDefinitionException invalidMilestoneConditionDefinition() {
        return new TemplateDefinitionException("Invalid property combination in milestone condition definition");
    }

    static TemplateDefinitionException invalidTaskReferenceForMilestone(String referencedTaskName, String milestoneName) {
        return new TemplateDefinitionException("Task " + referencedTaskName + " referenced in milestone " + milestoneName + " not found");
    }

    static TemplateDefinitionException missingMilestoneConditionDefinition(String milestoneName) {
        return new TemplateDefinitionException("Milestone " + milestoneName + " is missing a reachedWhen condition");
    }

    static TemplateDefinitionException undefinedEventReference(Set<String> undefinedEventNames) {
        return new TemplateDefinitionException("No domain event reference found for event(s) used in completion condition: " + undefinedEventNames);
    }

    static TemplateDefinitionException emptyProcessDataKey() {
        return new TemplateDefinitionException("Missing mandatory ProcessData property 'key'");
    }

    static TemplateDefinitionException emptyProcessDataSource() {
        return new TemplateDefinitionException("Missing mandatory ProcessData property 'source'");
    }

    static TemplateDefinitionException messageNameDoesNotExist(String messageName) {
        return new TemplateDefinitionException("Message '" + messageName + "' referenced in process data definition does not exists in message references");
    }

    static TemplateDefinitionException messageNameDoesNotExistProcessRelations(String messageName) {
        return new TemplateDefinitionException("Message '" + messageName + "' referenced in process relation definition does not exists in message references");
    }

    static TemplateDefinitionException processDataKeyDoesNotExist(String processDataKey) {
        return new TemplateDefinitionException("Process data reference to key with name '" + processDataKey + "' does not point to an existing process data definition");
    }

    static TemplateDefinitionException emptyPredicateType() {
        return new TemplateDefinitionException("Missing mandatory relation patterns property 'predicateType'");
    }

    static TemplateDefinitionException emptyRelationNodeType() {
        return new TemplateDefinitionException("Missing mandatory relation node property 'type'");
    }

    static TemplateDefinitionException unsupportedRelationJoinType(String relationJoinType) {
        return new TemplateDefinitionException("Unsupported relation join type '%s'. Valid values for 'joinType' are ['%s', '%s']".formatted(relationJoinType, RelationPatternDefinition.JOIN_BY_ROLE, RelationPatternDefinition.JOIN_BY_VALUE));
    }

    static TemplateDefinitionException emptyRelationNode(String relationType) {
        return new TemplateDefinitionException("Missing mandatory relation node'left' or 'right' in relation pattern of type " + relationType);
    }

    static TemplateDefinitionException emptyRelationNodeSelector(String relationNodeType) {
        return new TemplateDefinitionException("Missing mandatory relation node selector for relation node of type " + relationNodeType);
    }

    static TemplateDefinitionException emptyRelationSelectorProcessDataKey(String relationNodeType) {
        return new TemplateDefinitionException("Missing mandatory relation node selector property 'processDataKey' for relation node of type " + relationNodeType);
    }

    static TemplateDefinitionException missingRelationSystemId() {
        return new TemplateDefinitionException("Missing attribute relationSystemId, mandatory when relation patterns are declared");
    }

    static TemplateDefinitionException emptyProcessDataKeyInCorrelatedBy() {
        return new TemplateDefinitionException("processDataKey mustn't be empty in correlatedBy definition.");
    }

    static TemplateDefinitionException emptyMessgaeDataKeyInCorrelatedBy() {
        return new TemplateDefinitionException("messageDataKey mustn't be empty in correlatedBy definition.");
    }

    static TemplateDefinitionException emptyTaskTypeLifecycle(String taskName) {
        return new TemplateDefinitionException("lifecycle mustn't be empty in definition of task '" + taskName + "'.");
    }

    static TemplateDefinitionException unsupportedCardinalityForLifecycle(TaskCardinality cardinality, TaskLifecycle lifecycle) {
        return new TemplateDefinitionException("task lifecycle '" + lifecycle.name() + "' doesn't support cardinality '" + cardinality.name() + "'.");
    }

    static TemplateDefinitionException invalidCardinality(String cardinality) {
        return new TemplateDefinitionException("'" + cardinality + "' is not a valid task cardinality.");
    }

    static TemplateDefinitionException lifecycleWithDeprecatedCardinailty(String cardinalityStr, String taskName) {
        return new TemplateDefinitionException("Deprecated cardinality name '" + cardinalityStr + "' in task '" + taskName + "' mustn't be used together with a lifecycle definition.");
    }

    static TemplateDefinitionException completedByEventNotDefined(String templateName, Collection<String> eventNames) {
        return eventNotDefined(templateName, "completedBy", eventNames);
    }

    static TemplateDefinitionException observesEventNotDefined(String templateName, Collection<String> eventNames) {
        return eventNotDefined(templateName, "observes", eventNames);
    }

    static TemplateDefinitionException observesAndPlannedByTaskNotTogether(String templateName, String taskName) {
        return new TemplateDefinitionException(templateName + ": Task definition is wrong in " + taskName +". Observed and PlannedBy cannot both be defined for the same task");
    }

    static TemplateDefinitionException staticTasksCannotBePlanned(String templateName, String taskName) {
        return new TemplateDefinitionException(templateName + ": Task definition is wrong in " + taskName +". A static task cannot have a plannedBy section.");
    }

    static TemplateDefinitionException observesAndCompletedByTaskNotTogether(String templateName, String taskName) {
        return new TemplateDefinitionException(templateName + ": Task definition is wrong in " + taskName +". Observed and CompletedBy cannot both be defined for the same task");
    }

    static TemplateDefinitionException plannedByEventNotDefined(String templateName, Collection<String> eventNames) {
        return eventNotDefined(templateName, "plannedBy", eventNames);
    }

    static TemplateDefinitionException processCompletedByEventNotDefined(String templateName, Collection<String> eventNames) {
        return eventNotDefined(templateName, "completedBy", eventNames);
    }

    static TemplateDefinitionException processMustBeCompletedEitherByConditionOrDomainEvent() {
        return new TemplateDefinitionException("A process completion must either provide a condition or name a domainevent, but not both at the same time.");
    }

    static TemplateDefinitionException processCompletionByConditionDoesNotSupportAdditionalAttributes() {
        return new TemplateDefinitionException("A process completion providing a condition mustn't provide additional attributes.");
    }

    static TemplateDefinitionException processCompletionByDomainEventMustProvideConclusionAndName(String eventName) {
        return new TemplateDefinitionException("Illegal process completion naming the domain event '" + eventName + "'. A process completion by domain event must provide a conclusion and a name.");
    }

    private static TemplateDefinitionException eventNotDefined(String templateName, String eventType, Collection<String> eventNames) {
        String eventNamesList = String.join(", ", eventNames);
        return new TemplateDefinitionException(format("Process template %s defines %s events [%s] which are not contained in event definitions.",
                templateName,
                eventType,
                eventNamesList));
    }

    static TemplateDefinitionException emptyProcessRelationProperty(String propertyName) {
        return new TemplateDefinitionException("Missing mandatory process relation property '" + propertyName +"'");
    }

    static TemplateDefinitionException emptyProcessRelationRoleType() {
        return new TemplateDefinitionException("Missing mandatory process relation property 'relationRoleType'. Allowed are 'origin' or 'target");
    }

    static TemplateDefinitionException emptyProcessRelationVisibility() {
        return new TemplateDefinitionException("Missing mandatory process relation property 'visibility'. Allowed are 'origin', 'target' or 'both'" );
    }

    static TemplateDefinitionException emptyProcessRelationSource() {
        return new TemplateDefinitionException("Missing mandatory process relation property 'source'." );
    }

    static TemplateDefinitionException processSnapshotMustBeCreatedEitherByConditionOrCompletion() {
        return new TemplateDefinitionException("A process snapshot must either specify a condition or a completion, but not both at the same time.");
    }

    static TemplateDefinitionException invalidProcessSnapshotCompletion(String unknown) {
        return new TemplateDefinitionException("'%s' is not a valid process snapshot completion value.".formatted(unknown));
    }

    static TemplateDefinitionException missingTaskDataSourceEvent() {
        return new TemplateDefinitionException("Missing mandatory task data property 'messageSource'");
    }

    static TemplateDefinitionException missingOrEmptyTaskDataEventDataKeys() {
        return new TemplateDefinitionException("Missing or empty mandatory task data property 'eventDataKeys'");
    }

    static TemplateDefinitionException taskDataSourceMessagesNotPlanningOrCompletingTheTask(Set<String> illegalSourceMessages) {
        return new TemplateDefinitionException("""
                   Illegal source message declaration(s) in task data definition. The following messages are \
                   declared as sources for a task's data but are not planning or completing the task: %s""".
                formatted(String.join(", ", illegalSourceMessages)));
    }

}
