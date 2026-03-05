package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessRelation;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessRelationRepository;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Timed(value = "jeap_pcs_repository_processrelation")
public class ProcessRelationRepositoryImpl implements ProcessRelationRepository {
    private final ProcessRelationJpaRepository processRelationJpaRepository;


    @Override
    public List<ProcessRelation> findAllByRelatedProcessId(String processId) {
        return processRelationJpaRepository.findAllByRelatedProcessId(processId);
    }

    @Override
    public List<ProcessRelation> findAllByProcessInstanceId(UUID processInstanceId) {
        return processRelationJpaRepository.findAllByProcessInstanceId(processInstanceId);
    }

    @Override
    public Page<ProcessRelation> findAllVisibleForProcess(ProcessInstance processInstance, Pageable pageable) {
        return processRelationJpaRepository.findAllVisibleForProcess(
                processInstance.getId(), processInstance.getOriginProcessId(),
                pageable);
    }

    @Override
    public boolean exists(UUID processInstanceId, ProcessRelation processRelation) {
        return processRelationJpaRepository.existsByProcessInstance_IdAndNameAndRoleTypeAndOriginRoleAndTargetRoleAndVisibilityTypeAndRelatedProcessId(
                processInstanceId,
                processRelation.getName(),
                processRelation.getRoleType(),
                processRelation.getOriginRole(),
                processRelation.getTargetRole(),
                processRelation.getVisibilityType(),
                processRelation.getRelatedProcessId());
    }

    @Override
    public List<ProcessRelation> saveAll(List<ProcessRelation> processRelations) {
        return processRelationJpaRepository.saveAll(processRelations);
    }

}
