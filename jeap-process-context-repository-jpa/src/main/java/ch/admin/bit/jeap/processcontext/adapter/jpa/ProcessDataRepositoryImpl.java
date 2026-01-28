package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessData;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessDataRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class ProcessDataRepositoryImpl implements ProcessDataRepository {

    private final ProcessDataJpaRepository processDataJpaRepository;

    @Override
    public List<ProcessData> findProcessData(ProcessInstance processInstance, String processDataKey, String processDataRole) {
        if (processDataRole != null) {
            return processDataJpaRepository.findByProcessInstanceAndKeyAndRole(processInstance, processDataKey, processDataRole);
        }
        return processDataJpaRepository.findByProcessInstanceAndKey(processInstance, processDataKey);
    }
}
