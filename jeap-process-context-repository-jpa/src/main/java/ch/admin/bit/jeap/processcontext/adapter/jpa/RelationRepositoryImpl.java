package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.Relation;
import ch.admin.bit.jeap.processcontext.domain.processinstance.RelationRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
class RelationRepositoryImpl implements RelationRepository {

    static final int BATCH_SIZE = 100;

    private final RelationJpaRepository relationJpaRepository;

    @Override
    public Set<Relation> saveAllNewRelations(Collection<Relation> relations) {
        if (relations.isEmpty()) {
            return Set.of();
        }

        // Reduce the set of relations to only those that do not already exist in the database
        Set<Relation> newRelations = filterOutExistingRelations(relations);

        // Persist only relations that do not already exist
        if (!newRelations.isEmpty()) {
            relationJpaRepository.saveAll(newRelations);
        }

        return newRelations;
    }

    private Set<Relation> filterOutExistingRelations(Collection<Relation> relations) {
        List<Relation> relationList = new ArrayList<>(relations);
        Set<Relation> newRelations = new HashSet<>();

        for (int i = 0; i < relationList.size(); i += BATCH_SIZE) {
            // Create batches to avoid large queries when checking for existing relations
            int endIndex = Math.min(i + BATCH_SIZE, relationList.size());
            List<Relation> batch = relationList.subList(i, endIndex);

            // Filter out existing relations in the current batch
            Set<Relation> newRelationsInBatch = filterOutExistingRelations(batch);

            // Add only new relations from the current batch to the list of new relations
            newRelations.addAll(newRelationsInBatch);
        }
        return newRelations;
    }

    private Set<Relation> filterOutExistingRelations(List<Relation> relations) {
        Specification<Relation> spec = buildExistsSpecification(relations);
        List<Relation> existingRelations = relationJpaRepository.findAll(spec);

        Set<RelationKey> existingKeys = new HashSet<>();
        for (Relation existing : existingRelations) {
            existingKeys.add(RelationKey.of(existing));
        }

        Set<Relation> newRelations = new HashSet<>();
        for (Relation relation : relations) {
            if (!existingKeys.contains(RelationKey.of(relation))) {
                newRelations.add(relation);
            }
        }

        return newRelations;
    }

    private Specification<Relation> buildExistsSpecification(List<Relation> relations) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> orPredicates = new ArrayList<>();

            for (Relation relation : relations) {
                Predicate matchesRelation = criteriaBuilder.and(
                        criteriaBuilder.equal(root.get("processInstance"), relation.getProcessInstance()),
                        criteriaBuilder.equal(root.get("subjectType"), relation.getSubjectType()),
                        criteriaBuilder.equal(root.get("subjectId"), relation.getSubjectId()),
                        criteriaBuilder.equal(root.get("objectType"), relation.getObjectType()),
                        criteriaBuilder.equal(root.get("objectId"), relation.getObjectId()),
                        criteriaBuilder.equal(root.get("predicateType"), relation.getPredicateType())
                );
                orPredicates.add(matchesRelation);
            }

            return criteriaBuilder.or(orPredicates.toArray(new Predicate[0]));
        };
    }

    @Override
    public Set<Relation> findByProcessInstance(ProcessInstance processInstance) {
        return relationJpaRepository.findByProcessInstance(processInstance);
    }

    private record RelationKey(
            UUID processInstanceId,
            String subjectType,
            String subjectId,
            String objectType,
            String objectId,
            String predicateType) {
        static RelationKey of(Relation relation) {
            return new RelationKey(
                    relation.getProcessInstance().getId(),
                    relation.getSubjectType(),
                    relation.getSubjectId(),
                    relation.getObjectType(),
                    relation.getObjectId(),
                    relation.getPredicateType()
            );
        }
    }
}
