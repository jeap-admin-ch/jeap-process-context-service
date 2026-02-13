package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessRelation;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessRelationRoleType;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessRelationRoleVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProcessRelationJpaRepository extends JpaRepository<ProcessRelation, UUID> {

    List<ProcessRelation> findAllByRelatedProcessId(String processId);

    List<ProcessRelation> findAllByProcessInstanceId(UUID processInstanceId);

    Page<ProcessRelation> findAllByProcessInstanceId(UUID processInstanceId, Pageable pageable);

    @Query(value = "SELECT r FROM ProcessRelation r JOIN FETCH r.processInstance " +
            "WHERE (r.processInstance.id = :processInstanceId AND r.visibilityType <> 'TARGET') " +
            "OR (r.relatedProcessId = :originProcessId AND r.processInstance.id <> :processInstanceId AND r.visibilityType <> 'ORIGIN')",
            countQuery = "SELECT COUNT(r) FROM ProcessRelation r " +
                    "WHERE (r.processInstance.id = :processInstanceId AND r.visibilityType <> 'TARGET') " +
                    "OR (r.relatedProcessId = :originProcessId AND r.processInstance.id <> :processInstanceId AND r.visibilityType <> 'ORIGIN')")
    Page<ProcessRelation> findAllVisibleForProcess(@Param("processInstanceId") UUID processInstanceId, @Param("originProcessId") String originProcessId, Pageable pageable);

    boolean existsByProcessInstance_IdAndNameAndRoleTypeAndOriginRoleAndTargetRoleAndVisibilityTypeAndRelatedProcessId(
            UUID processInstanceId, String name, ProcessRelationRoleType roleType, String originRole, String targetRole,
            ProcessRelationRoleVisibility visibilityType, String relatedProcessId);
}
