package ch.admin.bit.jeap.processcontext.repository.template.json.deserializer;

import ch.admin.bit.jeap.processcontext.repository.template.json.model.*;
import lombok.experimental.UtilityClass;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

@UtilityClass
class ProcessTemplateUtils {

    <T> T newInstance(String className) {
        try {
            @SuppressWarnings("unchecked")
            Class<T> conditionClass = (Class<T>) Class.forName(className);
            return conditionClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw TemplateDefinitionException.errorWhileCreatingInstance(className, e);
        }
    }

    void validateEventReferences(ProcessTemplateDefinition templateDefinition) {
        Set<String> eventNamesInCompletionConditions = templateDefinition.getTasks().stream()
                .map(TaskTypeDefinition::getCompletedBy)
                .filter(Objects::nonNull)
                .map(TaskCompletionConditionDefinition::getMessage)
                .filter(Objects::nonNull)
                .collect(toSet());
        Set<String> definedEventReferences = templateDefinition.getMessages().stream()
                .map(MessageReferenceDefinition::getMessageName)
                .collect(toSet());
        eventNamesInCompletionConditions.removeAll(definedEventReferences);
        if (!eventNamesInCompletionConditions.isEmpty()) {
            throw TemplateDefinitionException.undefinedEventReference(eventNamesInCompletionConditions);
        }
    }

    void validateUniqueNames(String namedObject, Stream<String> names) {
        Set<String> allNames = new HashSet<>();
        Set<String> duplicateNames = names
                .filter(n -> !allNames.add(n))
                .collect(toSet());
        if (!duplicateNames.isEmpty()) {
            throw TemplateDefinitionException.duplicateName(namedObject, duplicateNames);
        }
    }

    static void validateEventName(List<MessageReferenceDefinition> events, List<ProcessDataDefinition> processDataDefinitions) {
         Set<String> eventNamesFromProcessData = processDataDefinitions.stream()
                 .map(processDataDefinition -> processDataDefinition.getSource().getMessage())
                 .collect(toSet());

         Set<String> eventNamesFormEvents = events.stream()
                 .map(MessageReferenceDefinition::getMessageName)
                 .collect(toSet());
         eventNamesFromProcessData.forEach(eventNameProcessData -> {
             if (!eventNamesFormEvents.contains(eventNameProcessData)) {
                 throw TemplateDefinitionException.messageNameDoesNotExist(eventNameProcessData);
             }
         });
    }

    /**
     * Validates whether the message in the process relation pattern is also registered in messages
     */
    static void validateMessageName(List<MessageReferenceDefinition> events,
                                    List<ProcessRelationPatternDefinition> processRelationPatternDefinitions) {
        Set<String> messageNamesFromProcessRelationPattern = processRelationPatternDefinitions.stream()
                .map(processRelationPatternDefinition -> processRelationPatternDefinition.getSource().getMessageName())
                .collect(toSet());

        Set<String> messageNamesFormMessages = events.stream()
                .map(MessageReferenceDefinition::getMessageName)
                .collect(toSet());
        messageNamesFromProcessRelationPattern.forEach(messageNameProcessRelation -> {
            if (!messageNamesFormMessages.contains(messageNameProcessRelation)) {
                throw TemplateDefinitionException.messageNameDoesNotExistProcessRelations(messageNameProcessRelation);
            }
        });
    }

    static void validateProcessDataKey(List<ProcessDataDefinition> processDataDefinitions, String processDataKey) {
        Set<String> definedProcessDataKeys = processDataDefinitions.stream()
                .map(ProcessDataDefinition::getKey)
                .collect(toSet());

        if (!definedProcessDataKeys.contains(processDataKey)) {
            throw TemplateDefinitionException.processDataKeyDoesNotExist(processDataKey);
        }
    }
}
