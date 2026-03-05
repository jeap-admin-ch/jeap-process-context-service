package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.Relation;
import ch.admin.bit.jeap.processcontext.domain.processinstance.RelationRepository;
import io.micrometer.core.annotation.Timed;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Timed(value = "jeap.pcs.repository.relation")
class RelationRepositoryImpl implements RelationRepository {

    static final int BATCH_SIZE = 100;

    private final RelationJpaRepository relationJpaRepository;

    @Override
    public Page<Relation> findByProcessInstanceId(UUID processInstanceId, Pageable pageable) {
        return relationJpaRepository.findByProcessInstanceId(processInstanceId, pageable);
    }

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
        // This returns the list of relations that already exist given the spec, which only selects those
        // matching a relation that we want to insert
        List<Relation> duplicateRelations = relationJpaRepository.findAll(spec);

        Set<Relation> newRelations = new HashSet<>(relations);
        // Remove duplicates, keep only new relations
        duplicateRelations.forEach(newRelations::remove);

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

}
