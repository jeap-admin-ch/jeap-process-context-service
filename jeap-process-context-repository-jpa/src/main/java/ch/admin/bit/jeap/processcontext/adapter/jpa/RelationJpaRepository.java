package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.Relation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;

@Repository
interface RelationJpaRepository extends JpaRepository<Relation, UUID> {

    Set<Relation> findByProcessInstance(ProcessInstance processInstance);

    boolean existsByProcessInstanceAndSubjectTypeAndSubjectIdAndObjectTypeAndObjectIdAndPredicateType(
            ProcessInstance processInstance, String subjectType, String subjectId,
            String objectType, String objectId, String predicateType);
}
