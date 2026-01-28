package ch.admin.bit.jeap.processcontext.domain.processinstance.relation;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessData;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessDataRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.Relation;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.RelationNodeSelector;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.RelationPattern;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.RelationPattern.JoinType;
import com.google.common.collect.Streams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
class RelationFactory {
    private final ProcessDataRepository processDataRepository;

    public Set<Relation> createNewRelations(ProcessInstance processInstance, List<ProcessData> newProcessDataList) {
        if (newProcessDataList.isEmpty()) {
            return Set.of();
        }

        Set<Relation> newRelations = new HashSet<>();
        ProcessTemplate processTemplate = processInstance.getProcessTemplate();
        String systemId = processTemplate.getRelationSystemId();

        for (ProcessData newProcessData : newProcessDataList) {
            // Find all relation patterns defined in the process template where the object pattern matches the new process data
            List<RelationPattern> matchingObjectPatterns = processTemplate.getRelationPatterns().stream()
                    .filter(pattern -> pattern.getObjectSelector().matches(newProcessData))
                    .toList();
            for (RelationPattern matchingObjectPattern : matchingObjectPatterns) {
                // Find matching subjects in the list of new process data
                Stream<ProcessData> matchingNewSubjects = newProcessDataList.stream()
                        .filter(subjectProcessData -> matchingObjectPattern.getSubjectSelector().matches(subjectProcessData));

                // Find matching subjects in previously persisted process data
                List<ProcessData> matchingPersistentSubjects = processDataRepository.findProcessData(processInstance,
                        matchingObjectPattern.getSubjectSelector().getProcessDataKey(),
                        matchingObjectPattern.getSubjectSelector().getProcessDataRole());

                // Now create a relation from the object to each matching subject
                List<ProcessData> subjects = Streams.concat(matchingNewSubjects, matchingPersistentSubjects.stream()).toList();
                createRelations(processInstance, newRelations, systemId, matchingObjectPattern, List.of(newProcessData), subjects);
            }

            List<RelationPattern> matchingSubjectPatterns = processTemplate.getRelationPatterns().stream()
                    .filter(pattern -> pattern.getSubjectSelector().matches(newProcessData))
                    .toList();
            for (RelationPattern matchingSubjectPattern : matchingSubjectPatterns) {
                // Find matching objects in the list of new process data
                Stream<ProcessData> matchingNewObjects = newProcessDataList.stream()
                        .filter(objectProcessData -> matchingSubjectPattern.getObjectSelector().matches(objectProcessData));

                // Find matching objects in previously persisted process data
                List<ProcessData> matchingPersistentObjects = processDataRepository.findProcessData(processInstance,
                        matchingSubjectPattern.getObjectSelector().getProcessDataKey(),
                        matchingSubjectPattern.getObjectSelector().getProcessDataRole());

                // Now create a relation from the subject to each matching object
                List<ProcessData> objects = Streams.concat(matchingNewObjects, matchingPersistentObjects.stream()).toList();
                createRelations(processInstance, newRelations, systemId, matchingSubjectPattern, objects, List.of(newProcessData));
            }

        }

        return newRelations;
    }

    private void createRelations(ProcessInstance processInstance, Set<Relation> newRelations, String systemId, RelationPattern pattern,
                                 List<ProcessData> objectProcessDataStream, List<ProcessData> subjectProcessDataStream) {
        for (ProcessData objectProcessData : objectProcessDataStream) {
            for (ProcessData subjectProcessData : subjectProcessDataStream) {
                createRelationIfJoinMatches(processInstance, newRelations, systemId, pattern, objectProcessData, subjectProcessData);
            }
        }
    }

    /**
     * If no joinType is defined, join each pair.
     * If the joinTyp is byRole, join only if roles match.
     * If the joinType is byValue, join only if values match.
     */
    private void createRelationIfJoinMatches(ProcessInstance processInstance, Set<Relation> newRelations, String systemId, RelationPattern pattern, ProcessData objectProcessData, ProcessData subjectProcessData) {
        if (pattern.getJoinType() == null) {
            newRelations.add(createRelation(processInstance, systemId, pattern, objectProcessData, subjectProcessData));
        } else if (JoinType.BY_ROLE == pattern.getJoinType()) {
            if (objectProcessData.getRole() != null && objectProcessData.getRole().equals(subjectProcessData.getRole())) {
                newRelations.add(createRelation(processInstance, systemId, pattern, objectProcessData, subjectProcessData));
            }
        } else if (JoinType.BY_VALUE == pattern.getJoinType()) {
            if (objectProcessData.getValue() != null && objectProcessData.getValue().equals(subjectProcessData.getValue())) {
                newRelations.add(createRelation(processInstance, systemId, pattern, objectProcessData, subjectProcessData));
            }
        }
    }

    private Relation createRelation(ProcessInstance processInstance, String systemId,
                                    RelationPattern pattern, ProcessData objectProcessData, ProcessData subjectProcessData) {
        return createRelation(processInstance, systemId, pattern.getPredicateType(),
                RelationNode.of(objectProcessData, pattern.getObjectSelector()),
                RelationNode.of(subjectProcessData, pattern.getSubjectSelector()),
                pattern.getFeatureFlag()
        );
    }

    private Relation createRelation(ProcessInstance processInstance, String systemId, String predicateType,
                                    RelationNode object, RelationNode subject, String featureFlag) {
        return Relation.builder()
                .processInstance(processInstance)
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
