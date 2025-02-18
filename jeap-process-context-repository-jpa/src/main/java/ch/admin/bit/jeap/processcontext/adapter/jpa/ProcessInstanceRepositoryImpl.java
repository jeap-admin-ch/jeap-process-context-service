package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.*;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.*;

import static java.util.stream.Collectors.*;

@Component
@RequiredArgsConstructor
class ProcessInstanceRepositoryImpl implements ProcessInstanceRepository {

    private final ProcessInstanceJpaRepository processInstanceJpaRepository;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final ProcessTemplateRepository processTemplateRepository;

    @Override
    public ProcessInstance save(ProcessInstance processInstance) {
        return processInstanceJpaRepository.save(processInstance);
    }

    @Override
    public void deleteAllById(Set<UUID> ids) {
        processInstanceJpaRepository.deleteEventReferenceByProcessInstanceIds(ids);
        processInstanceJpaRepository.deleteMilestoneByProcessInstanceIds(ids);
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
    public Optional<ProcessInstance> findByOriginProcessIdLoadingMessages(String originProcessId) {
        return findByOriginProcessIdWithoutLoadingMessages(originProcessId).map(this::setMessageReferenceMessageDTOs);
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<ProcessInstance> findByOriginProcessIdWithoutLoadingMessages(String originProcessId) {
        return processInstanceJpaRepository.findByOriginProcessId(originProcessId).map(this::setProcessTemplateReference);
    }

    @Override
    public Optional<ProcessInstanceSummary> findProcessInstanceSummaryByOriginProcessId(String originProcessId) {
        return processInstanceJpaRepository.findProcessInstanceSummaryByOriginProcessId(originProcessId);
    }

    @Override
    public Optional<String> getProcessTemplateNameByOriginProcessId(String originProcessId) {
        return processInstanceJpaRepository.getProcessTemplateNameByOriginProcessId(originProcessId);
    }

    @Override
    public Page<ProcessInstance> findAll(Pageable pageable) {
        return processInstanceJpaRepository.findAll(pageable).map(this::setProcessTemplateReferenceAndMessageReferencesMessageDTOs);
    }

    @Override
    public Page<ProcessInstance> findByProcessData(String processData, Pageable pageable) {
        return processInstanceJpaRepository.findDistinctByProcessData_value(processData, pageable).map(this::setProcessTemplateReferenceAndMessageReferencesMessageDTOs);
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
        return processInstanceJpaRepository.findIdOriginProcessIdByStateAndModifiedAtBefore(processState, olderThan, pageable);
    }

    private ProcessInstance setProcessTemplateReference(ProcessInstance processInstance) {
        ProcessTemplate template = processTemplateRepository.findByName(processInstance.getProcessTemplateName())
                .orElseThrow(ProcessTemplateNotFoundException.templateNotFound(processInstance.getOriginProcessId(), processInstance.getProcessTemplateName()));
        if (processInstance.getProcessTemplate() == null) {
            processInstance.setProcessTemplate(template);
        } else if (!processInstance.getProcessTemplate().equals(template)) {
            throw new IllegalStateException("Cannot replace process template on process " + processInstance.getOriginProcessId() + " with a different process template");
        }
        return processInstance;
    }

    @Override
    public void setHashForTemplateIfNull(ProcessTemplate template) {
        processInstanceJpaRepository.setInitialTemplateHashIfNotSet(template.getName(), template.getTemplateHash());
    }

    @Override
    public Slice<String> findUncompletedProcessInstanceOriginIdsByTemplateHashChanged(
            ZonedDateTime lastModifiedAfter, ProcessTemplate template, Pageable pageable) {
        return processInstanceJpaRepository.findUncompletedProcessInstanceOriginIdsByTemplateHashChanged(
                template.getName(), template.getTemplateHash(), lastModifiedAfter, pageable);
    }

    @Override
    public Optional<ProcessInstanceTemplate> findProcessInstanceTemplate(String originProcessId) {
        return processInstanceJpaRepository.findProcessInstanceTemplate(originProcessId);
    }

    @Override
    public List<MessageReferenceMessageDTO> findMessageReferenceMessageDTOs(UUID processInstanceId) {
        List<MessageReferenceMessage> messages = processInstanceJpaRepository.findMessageReferencesMessages(processInstanceId);
        Map<UUID, Set<MessageReferenceMessageDataDTO>> messageDataById = processInstanceJpaRepository.findMessageReferencesMessageData(processInstanceId).stream()
                .collect(groupingBy(MessageReferenceMessageData::getMessageReferenceId, mapping(this::toDTO, toSet())));
        Map<UUID, Set<String>> relatedTaskIdsByMessageReferenceId = processInstanceJpaRepository.findMessageReferencesRelatedOriginTaskIds(processInstanceId).stream()
                .collect(groupingBy(MessageReferenceRelatedTaskId::getMessageReferenceId, mapping(MessageReferenceRelatedTaskId::getRelatedOriginTaskId, toSet())));
        return messages.stream().map(messageReferenceMessage -> MessageReferenceMessageDTO.builder()
                .messageReferenceId(messageReferenceMessage.getMessageReferenceId())
                .messageId(messageReferenceMessage.getMessageId())
                .messageName(messageReferenceMessage.getMessageName())
                .messageReceivedAt(messageReferenceMessage.getMessageReceivedAt())
                .messageData(messageDataById.getOrDefault(messageReferenceMessage.getMessageReferenceId(), Set.of()))
                .traceId(messageReferenceMessage.getTraceId())
                .relatedOriginTaskIds(relatedTaskIdsByMessageReferenceId.getOrDefault(messageReferenceMessage.getMessageReferenceId(), Set.of()))
                .build()).collect(toList());
    }

    @Override
    public Optional<Integer> getLatestProcessSnapshotVersion(String originProcessId) {
        return Optional.ofNullable(processInstanceJpaRepository.getLatestProcessSnapshotVersion(originProcessId));
    }

    @Override
    public void flush() {
        processInstanceJpaRepository.flush();
    }

    private MessageReferenceMessageDataDTO toDTO(MessageReferenceMessageData messageReferenceMessageData) {
        return MessageReferenceMessageDataDTO.builder()
                .messageDataKey(messageReferenceMessageData.getMessageDataKey())
                .messageDataValue(messageReferenceMessageData.getMessageDataValue())
                .messageDataRole(messageReferenceMessageData.getMessageDataRole())
                .build();
    }

    private ProcessInstance setMessageReferenceMessageDTOs(ProcessInstance processInstance) {
        processInstance.setMessageReferenceMessageDTOS(findMessageReferenceMessageDTOs(processInstance.getId()));
        return processInstance;
    }

    private ProcessInstance setProcessTemplateReferenceAndMessageReferencesMessageDTOs(ProcessInstance processInstance) {
        setProcessTemplateReference(processInstance);
        setMessageReferenceMessageDTOs(processInstance);
        return processInstance;
    }

}
