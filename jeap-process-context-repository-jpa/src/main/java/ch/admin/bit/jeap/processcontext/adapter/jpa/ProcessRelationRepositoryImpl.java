package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessRelation;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessRelationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
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
