package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.*;
import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextFactory;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class ProcessInstanceRepositoryImpl implements ProcessInstanceRepository {

    private final ProcessInstanceJpaRepository processInstanceJpaRepository;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final ProcessTemplateRepository processTemplateRepository;
    private final ProcessContextFactory processContextFactory;

    @Override
    public ProcessInstance save(ProcessInstance processInstance) {
        return processInstanceJpaRepository.save(processInstance);
    }

    @Override
    public void deleteAllById(Set<UUID> ids) {
        processInstanceJpaRepository.deleteEventReferenceByProcessInstanceIds(ids);
        processInstanceJpaRepository.deleteProcessDataByProcessInstanceIds(ids);
        processInstanceJpaRepository.deleteProcessRelationsByProcessInstanceIds(ids);
        processInstanceJpaRepository.deleteProcessInstanceProcessRelationsByProcessInstanceIds(ids);
        Set<String> originProcessIds = processInstanceJpaRepository.getOriginProcessIdsByInstanceIds(ids);
        processInstanceJpaRepository.deleteProcessInstanceProcessRelationsByRelatedOriginProcessIds(originProcessIds);
        processInstanceJpaRepository.deleteTaskInstanceByProcessInstanceIds(ids);
        processInstanceJpaRepository.deleteByProcessInstanceIds(ids);
    }

    @Transactional(readOnly = true)
    @Override
    public boolean existsByOriginProcessId(String originProcessId) {
        return processInstanceJpaRepository.existsByOriginProcessId(originProcessId);
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<ProcessInstance> findByOriginProcessId(String originProcessId) {
        return processInstanceJpaRepository.findByOriginProcessId(originProcessId).map(this::setProcessTemplateReference);
    }

    @Override
    public Optional<ProcessInstanceSummary> findProcessInstanceSummaryByOriginProcessId(String originProcessId) {
        return processInstanceJpaRepository.findProcessInstanceSummaryByOriginProcessId(originProcessId);
    }

    @Override
    public Page<ProcessInstance> findAll(Pageable pageable) {
        return processInstanceJpaRepository.findAll(pageable).map(this::setProcessTemplateReference);
    }

    @Override
    public Page<ProcessInstance> findByProcessData(String processData, Pageable pageable) {
        return processInstanceJpaRepository.findByProcessDataValue(processData, pageable).map(this::setProcessTemplateReference);
    }

    @Override
    public Set<String> findUncompletedProcessInstancesHavingProcessData(String processTemplateName, String processDataKey, String processDataValue, String processDataRole) {
        if (processDataRole != null) {
            return processInstanceJpaRepository.findUncompletedProcessInstancesHavingProcessData(processTemplateName, processDataKey, processDataValue, processDataRole);
        } else {
            return processInstanceJpaRepository.findUncompletedProcessInstancesHavingProcessDataWithoutRole(processTemplateName, processDataKey, processDataValue);
        }
    }

    @Override
    public Slice<ProcessInstanceQueryResult> findProcessInstances(ProcessState processState, ZonedDateTime olderThan, Pageable pageable) {
        return processInstanceJpaRepository.findIdOriginProcessIdByStateAndCreatedAtBefore(processState, olderThan, pageable);
    }

    private ProcessInstance setProcessTemplateReference(ProcessInstance processInstance) {
        ProcessTemplate template = processTemplateRepository.findByName(processInstance.getProcessTemplateName())
                .orElseThrow(ProcessTemplateNotFoundException.templateNotFound(processInstance.getOriginProcessId(), processInstance.getProcessTemplateName()));
        if (processInstance.getProcessTemplate() == null) {
            processInstance.onAfterLoadFromPersistentState(template, processContextFactory);
        } else if (!processInstance.getProcessTemplate().equals(template)) {
            throw new IllegalStateException("Cannot replace process template on process " + processInstance.getOriginProcessId() + " with a different process template");
        }
        return processInstance;
    }

    @Override
    public Slice<String> findUncompletedProcessInstanceOriginIdsByTemplateHashChanged(
            ZonedDateTime createdAtAfter, ProcessTemplate template, Pageable pageable) {
        return processInstanceJpaRepository.findUncompletedProcessInstanceOriginIdsByTemplateHashChanged(
                template.getName(), template.getTemplateHash(), createdAtAfter, pageable);
    }

    @Override
    public Optional<ProcessInstanceTemplate> findProcessInstanceTemplate(String originProcessId) {
        return processInstanceJpaRepository.findProcessInstanceTemplate(originProcessId);
    }

    @Override
    public Optional<MessageReference> findLatestMessageReferenceByMessageTypeAndOriginTaskId(ProcessInstance processInstance, String messageType, String originTaskId) {
        return processInstanceJpaRepository.findMessageReferencesByMessageTypeAndOriginTaskId(
                        processInstance.getId(), messageType, originTaskId, PageRequest.of(0, 1))
                .stream()
                .findFirst();
    }

    @Override
    public Optional<MessageReference> findLatestMessageReferenceByMessageType(ProcessInstance processInstance, String messageType) {
        return processInstanceJpaRepository.findMessageReferencesByMessageType(
                        processInstance.getId(), messageType, PageRequest.of(0, 1))
                .stream()
                .findFirst();
    }

    @Override
    public void flush() {
        processInstanceJpaRepository.flush();
    }
}
