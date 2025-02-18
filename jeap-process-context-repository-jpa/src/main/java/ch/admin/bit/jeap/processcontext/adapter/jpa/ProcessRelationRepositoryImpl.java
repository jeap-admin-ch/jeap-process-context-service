package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessRelation;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessRelationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProcessRelationRepositoryImpl implements ProcessRelationRepository {
    private final ProcessRelationJpaRepository processRelationJpaRepository;


    @Override
    public List<ProcessRelation> findByRelatedProcessId(String processId) {
        return processRelationJpaRepository.findAllByRelatedProcessId(processId);
    }

}
