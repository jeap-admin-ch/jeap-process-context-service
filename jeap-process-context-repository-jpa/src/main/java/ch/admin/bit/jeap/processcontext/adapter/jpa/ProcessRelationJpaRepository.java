package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessRelation;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessRelationRoleType;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessRelationRoleVisibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProcessRelationJpaRepository extends JpaRepository<ProcessRelation, UUID> {

    List<ProcessRelation> findAllByRelatedProcessId(String processId);

    List<ProcessRelation> findAllByProcessInstanceId(UUID processInstanceId);

    boolean existsByProcessInstance_IdAndNameAndRoleTypeAndOriginRoleAndTargetRoleAndVisibilityTypeAndRelatedProcessId(
            UUID processInstanceId, String name, ProcessRelationRoleType roleType, String originRole, String targetRole,
            ProcessRelationRoleVisibility visibilityType, String relatedProcessId);
}
