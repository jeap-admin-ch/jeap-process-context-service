package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessData;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessDataRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@Timed(value = "jeap_pcs_repository_processdata")
class ProcessDataRepositoryImpl implements ProcessDataRepository {

    private final ProcessDataJpaRepository processDataJpaRepository;

    @Override
    public List<ProcessData> findProcessData(ProcessInstance processInstance, String processDataKey, String processDataRole) {
        if (processDataRole != null) {
            return processDataJpaRepository.findByProcessInstanceAndKeyAndRole(processInstance, processDataKey, processDataRole);
        }
        return processDataJpaRepository.findByProcessInstanceAndKey(processInstance, processDataKey);
    }

    @Override
    public List<ProcessData> findByProcessInstanceId(UUID processInstanceId) {
        return processDataJpaRepository.findByProcessInstanceId(processInstanceId);
    }

    @Override
    public Page<ProcessData> findByProcessInstanceId(UUID processInstanceId, Pageable pageable) {
        return processDataJpaRepository.findByProcessInstanceId(processInstanceId, pageable);
    }

    @Override
    public boolean saveIfNew(ProcessData processData) {
        return processDataJpaRepository.saveIfNew(
                processData.getId(),
                processData.getProcessInstance().getId(),
                processData.getKey(),
                processData.getValue(),
                processData.getRole(),
                processData.getCreatedAt()) > 0;
    }
}
