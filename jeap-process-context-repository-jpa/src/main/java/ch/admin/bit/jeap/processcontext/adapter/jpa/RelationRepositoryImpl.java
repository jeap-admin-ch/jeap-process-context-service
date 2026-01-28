package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.Relation;
import ch.admin.bit.jeap.processcontext.domain.processinstance.RelationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
class RelationRepositoryImpl implements RelationRepository {

    private final RelationJpaRepository relationJpaRepository;

    @Override
    public Set<Relation> saveAll(Collection<Relation> relations) {
        Set<Relation> newRelations = new HashSet<>();

        for (Relation relation : relations) {
            boolean exists = relationJpaRepository.existsByProcessInstanceAndSubjectTypeAndSubjectIdAndObjectTypeAndObjectIdAndPredicateType(
                    relation.getProcessInstance(),
                    relation.getSubjectType(),
                    relation.getSubjectId(),
                    relation.getObjectType(),
                    relation.getObjectId(),
                    relation.getPredicateType());

            if (!exists) {
                newRelations.add(relation);
            }
        }

        if (!newRelations.isEmpty()) {
            relationJpaRepository.saveAll(newRelations);
        }

        return newRelations;
    }

    @Override
    public Set<Relation> findByProcessInstance(ProcessInstance processInstance) {
        return relationJpaRepository.findByProcessInstance(processInstance);
    }
}
