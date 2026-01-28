package ch.admin.bit.jeap.processcontext.domain.processinstance.relation;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessData;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.Relation;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.RelationNodeSelector;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.RelationPattern;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// TODO JEAP-6536 feature flags
@Component
class RelationFactory {
    public Collection<Relation> createNewRelations(ProcessInstance processInstance, List<ProcessData> newProcessDataList) {
        if (newProcessDataList.isEmpty()) {
            return List.of();
        }

        List<Relation> newRelations = new ArrayList<>();
        ProcessTemplate processTemplate = processInstance.getProcessTemplate();
        String systemId = processTemplate.getRelationSystemId();

        // 1.) Find all relation patterns defined in the process template where the subject or object pattern matches
        // the new process data
        for (ProcessData newProcessData : newProcessDataList) {
            List<RelationPattern> matchingObjectPatterns = processTemplate.getRelationPatterns().stream()
                    .filter(pattern -> pattern.getObjectSelector().matches(newProcessData))
                    .toList();
            for (RelationPattern matchingObjectPattern : matchingObjectPatterns) {
                // Find matching subject in the list of new process data
                List<Relation> relations = newProcessDataList.stream()
                        .filter(subjectProcessData -> matchingObjectPattern.getSubjectSelector().matches(subjectProcessData))
                        .map(subjectProcessData -> createRelation(systemId, matchingObjectPattern, subjectProcessData, newProcessData))
                        .toList();
                newRelations.addAll(relations);

                // Find matching subject in previously persisted process data
                // TODO JEAP-6536
            }

            List<RelationPattern> matchingSubjectPatterns = processTemplate.getRelationPatterns().stream()
                    .filter(pattern -> pattern.getSubjectSelector().matches(newProcessData))
                    .toList();
            // TODO JEAP-6536
        }

        // 2.) For each relation pattern, use RelationFactory to create matching relations

        return newRelations;
    }

    private Relation createRelation(String systemId, RelationPattern pattern, ProcessData subjectProcessData, ProcessData objectProcessData) {
        return createRelation(
                systemId,
                pattern.getPredicateType(),
                RelationNode.of(objectProcessData, pattern.getObjectSelector()),
                RelationNode.of(subjectProcessData, pattern.getSubjectSelector()),
                pattern.getFeatureFlag()
        );
    }

    private Relation createRelation(String systemId, String predicateType, RelationNode object, RelationNode subject, String featureFlag) {
        return Relation.builder()
                .systemId(systemId)
                .subjectType(subject.type())
                .subjectId(subject.id())
                .objectType(object.type())
                .objectId(object.id())
                .predicateType(predicateType)
                .featureFlag(featureFlag)
                .build();
    }

    private record RelationNode(String type, String id, String role) {

        static RelationNode of(ProcessData pd, RelationNodeSelector selector) {
            return new RelationNode(selector.getType(), pd.getValue(), pd.getRole());
        }
    }
}
