package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.processtemplate.RelationNodeSelector;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.RelationPattern;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static lombok.AccessLevel.PRIVATE;

/**
 * Factory for {@link Relation}s, matches {@link RelationPattern}s against the domain model and creates relations if
 * the pattern matches.
 */
@UtilityClass
class RelationFactory {

    Collection<Relation> createMatchingRelations(String systemId, RelationPattern relationPattern, ProcessDataWrapper processDataWrapper) {
        Set<RelationNode> objects = findNodesByPattern(processDataWrapper, relationPattern.getObjectSelector());
        if (objects.isEmpty()) {
            // No objects found - no need to even look for subjects
            return Set.of();
        }
        Set<RelationNode> subjects = findNodesByPattern(processDataWrapper, relationPattern.getSubjectSelector());
        if (subjects.isEmpty()) {
            // No subjects found - no need to create relations
            return Set.of();
        }

        return createRelations(objects, subjects, systemId, relationPattern);
    }

    /**
     * Finds relation nodes by matching the {@link RelationPattern} against process data instances
     */
    private Set<RelationNode> findNodesByPattern(ProcessDataWrapper processDataWrapper, RelationNodeSelector selector) {
        String processDataKey = selector.getProcessDataKey();
        String processDataRole = selector.getProcessDataRole();
        Collection<ProcessData> matches = processDataWrapper.findByKeyAndOptionalRole(processDataKey, processDataRole);
        return matches.stream()
                .map(processData -> RelationNode.of(processData, selector))
                .collect(toSet());
    }

    /**
     * Create relations by building the cartesian product of all matching subject/object pairs for this relation pattern
     * if no join type has been specified. Otherwise, create the relations according to join type:
     * - "byRole": when role values are equal
     * - "byValue": when values are equal
     */
    private Collection<Relation> createRelations(Set<RelationNode> objects, Set<RelationNode> subjects, String systemId, RelationPattern relationPattern) {
        Collection<Relation> relations = new ArrayList<>();
        for (RelationNode object : objects) {
            for (RelationNode subject : subjects) {
                if (relationPattern.getJoinType() == null) {
                    relations.add(createRelation(systemId, relationPattern.getPredicateType(), object, subject));
                } else if ("byRole".equals(relationPattern.getJoinType())) {
                    if (object.getRole() != null && object.getRole().equals(subject.getRole())) {
                        relations.add(createRelation(systemId, relationPattern.getPredicateType(), object, subject));
                    }
                } else if ("byValue".equals(relationPattern.getJoinType())) {
                    if (object.getId() != null && object.getId().equals(subject.getId())) {
                        relations.add(createRelation(systemId, relationPattern.getPredicateType(), object, subject));
                    }
                }
            }
        }
        return relations;
    }

    private Relation createRelation(String systemId, String predicateType, RelationNode object, RelationNode subject) {
        return Relation.builder()
                .systemId(systemId)
                .subjectType(subject.getType())
                .subjectId(subject.getId())
                .objectType(object.getType())
                .objectId(object.getId())
                .predicateType(predicateType)
                .build();
    }

    @Value
    @AllArgsConstructor(access = PRIVATE)
    private static class RelationNode {
        String type;
        String id;
        String role;

        static RelationNode of(ProcessData pd, RelationNodeSelector selector) {
            return new RelationNode(selector.getType(), pd.getValue(), pd.getRole());
        }
    }
}
