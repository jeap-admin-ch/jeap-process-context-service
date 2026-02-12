package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.message.MessageData;
import ch.admin.bit.jeap.processcontext.domain.message.MessageReferenceRepository;
import ch.admin.bit.jeap.processcontext.domain.message.MessageRepository;
import ch.admin.bit.jeap.processcontext.domain.port.MetricsListener;
import ch.admin.bit.jeap.processcontext.domain.processinstance.relation.RelationService;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.MessageReference;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessRelationPattern;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import ch.admin.bit.jeap.processcontext.domain.tx.Transactions;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

import static net.logstash.logback.argument.StructuredArguments.keyValue;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessInstanceService {
    private static final String ORIGIN_PROCESS_ID = "originProcessId";

    private final ProcessInstanceRepository processInstanceRepository;
    private final TaskService taskService;
    private final ProcessInstanceMigrationService processInstanceMigrationService;
    private final ProcessDataService processDataService;
    private final ProcessTemplateRepository processTemplateRepository;
    private final MessageRepository messageRepository;
    private final MessageReferenceRepository messageReferenceRepository;
    private final ProcessSnapshotService processSnapshotService;
    private final RelationService relationService;
    private final ProcessRelationRepository processRelationRepository;
    private final Transactions transactions;
    private final MetricsListener metricsListener;
    private final ProcessInstanceFactory processInstanceFactory;
    private final PendingMessageRepository pendingMessageRepository;

    @Timed(value = "jeap_pcs_update_migrate", description = "Migrate process template", percentiles = {0.5, 0.8, 0.99})
    public void migrateProcessInstanceTemplate(String originProcessId) {
        migrateProcessInstanceTemplateIfNeeded(originProcessId);
    }

    @Timed(value = "jeap_pcs_update_process_state", description = "Update process state", percentiles = {0.5, 0.8, 0.99})
    public void handleMessage(String originProcessId, UUID messageId) {
        transactions.withinNewTransaction(() -> updateProcessStateInTransaction(originProcessId, messageId, null));
    }

    @Timed(value = "jeap_pcs_update_process_state", description = "Update process state", percentiles = {0.5, 0.8, 0.99})
    public void handleMessage(String originProcessId, UUID messageId, String createProcessByTemplateName) {
        transactions.withinNewTransaction(() -> updateProcessStateInTransaction(originProcessId, messageId, createProcessByTemplateName));
    }

    private void updateProcessStateInTransaction(String originProcessId, UUID messageId, String createProcessByTemplateName) {
        try {
            Message message = messageRepository.findById(messageId)
                    .orElseThrow(NotFoundException.messageNotFound(messageId, originProcessId));

            // First, try to find an existing process instance for the given originProcessId. If it exists,
            // correlate the message to it and update the process instance state.
            Optional<ProcessInstance> existingInstance = processInstanceRepository.findByOriginProcessId(originProcessId);
            if (existingInstance.isPresent()) {
                handleMessageForProcessInstance(existingInstance.get(), message);
                return;
            }

            // If no process instance exists for the given originProcessId,  check if the message triggers the
            // creation of a new process instance. If yes, create the new process instance and correlate the message to it.
            Optional<ProcessInstance> createdInstance = processInstanceFactory.createProcessInstance(originProcessId, createProcessByTemplateName);
            if (createdInstance.isPresent()) {
                ProcessInstance processInstance = createdInstance.get();
                handleMessageForProcessInstance(processInstance, message);
                handlePendingMessagesForNewProcessInstance(originProcessId, messageId, processInstance);
                return;
            }

            // Process instance does not exist yet, and message does not trigger creation of a new process instance.
            // It is saved as pending message, so that it can be processed when the process instance is created by a
            // later message.
            pendingMessageRepository.saveIfNew(PendingMessage.from(message, originProcessId));
        } catch (ProcessUpdateFailedException ex) {
            log.error("Failed to apply process update to process {}", keyValue(ORIGIN_PROCESS_ID, originProcessId), ex);
            metricsListener.processUpdateFailed();
            throw ex;
        }
    }

    private void handlePendingMessagesForNewProcessInstance(String originProcessId, UUID messageId, ProcessInstance processInstance) {
        List<PendingMessage> pendingMessages = pendingMessageRepository.findByOriginProcessId(originProcessId);
        for (PendingMessage pendingMessage : pendingMessages) {
            Message messageToHandle = messageRepository.findById(pendingMessage.getMessageId())
                    .orElseThrow(NotFoundException.messageNotFound(messageId, originProcessId));
            handleMessageForProcessInstance(processInstance, messageToHandle);
        }
        pendingMessageRepository.deleteAll(pendingMessages);
    }

    private void handleMessageForProcessInstance(ProcessInstance processInstance, Message message) {
        applyTemplateMigrationsIfRequired(processInstance);
        processUpdate(processInstance, message);
        metricsListener.processUpdateProcessed(processInstance.getProcessTemplate());
    }

    private void migrateProcessInstanceTemplateIfNeeded(String originProcessId) {
        transactions.withinNewTransaction(() ->
                processInstanceRepository.findProcessInstanceTemplate(originProcessId).ifPresent(processInstanceTemplate ->
                        migrateIfNeeded(originProcessId, processInstanceTemplate)));
    }

    private void migrateIfNeeded(String originProcessId, ProcessInstanceTemplate processInstanceTemplate) {
        processTemplateRepository.findByName(processInstanceTemplate.getTemplateName()).ifPresent(template -> {
            if (!template.getTemplateHash().equals(processInstanceTemplate.getTemplateHash())) {
                processInstanceRepository.findByOriginProcessId(originProcessId).ifPresent(
                        this::applyTemplateMigrationsIfRequired);
            }
        });
    }

    private void applyTemplateMigrationsIfRequired(ProcessInstance processInstance) {
        ProcessState stateBeforeMigration = processInstance.getState();
        Optional<List<TaskInstance>> listOfTasksPlannedByMigration = processInstanceMigrationService.applyTemplateMigrationIfChanged(processInstance);
        if (listOfTasksPlannedByMigration.isPresent()) {
            List<TaskInstance> tasksPlannedByMigration = listOfTasksPlannedByMigration.get();
            // Evaluate if any of the tasks planned by the migration can be completed immediately because they are
            // waiting for messages received before the migration
            taskService.evaluatePlannedTasksCompletedByExistingMessages(tasksPlannedByMigration);
            processInstance.updateState();
            countCompletionIfCompleted(processInstance, stateBeforeMigration);
        }
    }

    private void countCompletionIfCompleted(ProcessInstance processInstance, ProcessState stateBeforeUpdate) {
        if (stateBeforeUpdate != ProcessState.COMPLETED && processInstance.getState() == ProcessState.COMPLETED) {
            metricsListener.processCompleted(processInstance.getProcessTemplate());
        }
    }

    private void processUpdate(ProcessInstance processInstance, Message message) {
        try {
            applyProcessUpdateAndReEvaluateProcessInstance(processInstance, message);
        } catch (Exception e) {
            throw ProcessUpdateFailedException.withCause(processInstance.getOriginProcessId(), e);
        }
    }

    private void applyProcessUpdateAndReEvaluateProcessInstance(ProcessInstance processInstance, Message message) {
        // Idempotence check: If the message has already been correlated to the process instance, skip processing
        if (messageReferenceRepository.existsByProcessInstanceIdAndMessageId(processInstance.getId(), message.getId())) {
            return;
        }

        ProcessState stateBeforeUpdate = processInstance.getState();

        var newProcessData = processDataService.copyMessageDataToProcessData(processInstance, message);
        var messageReferenceMessageDTO = addMessage(processInstance, message);
        if (!newProcessData.isEmpty()) {
            relationService.onNewProcessData(processInstance, newProcessData);
            metricsListener.timed("jeap_pcs_late_correlate_message", Map.of(),
                    () -> correlateMessagesByProcessDataIfRequired(processInstance, newProcessData));
        }

        metricsListener.timed("pcs_process_update", Map.of(), () ->
                updateProcessInstance(processInstance, messageReferenceMessageDTO, message));
        countCompletionIfCompleted(processInstance, stateBeforeUpdate);
        createProcessSnapshotIfTriggered(processInstance);
        processInstanceRepository.flush();
    }

    private MessageReferenceMessageDTO addMessage(ProcessInstance processInstance, Message message) {
        var messageReference = ch.admin.bit.jeap.processcontext.domain.processinstance.MessageReference.from(message, processInstance);
        var persistedReference = messageReferenceRepository.save(messageReference);
        return MessageReferenceMessageDTO.of(processInstance.getProcessTemplateName(), persistedReference.getId(), message);
    }

    private void createProcessSnapshotIfTriggered(ProcessInstance processInstance) {
        Set<String> triggeredSnapshots = processInstance.evaluateSnapshotConditions();
        if (!triggeredSnapshots.isEmpty()) {
            // Make sure that process instance fields managed by the repository layer (createdAt, etc.) are up-to-date
            // before extracting the snapshot archive data from the process instance.
            processInstanceRepository.flush();
            processSnapshotService.createAndStoreSnapshot(processInstance);
            triggeredSnapshots.forEach(processInstance::registerSnapshot);
        }
    }

    private void updateProcessInstance(ProcessInstance processInstance, MessageReferenceMessageDTO messageReferenceMessageDTO, Message message) {
        List<TaskInstance> plannedDomainEventTasks = taskService.planDomainEventTasks(processInstance, messageReferenceMessageDTO, message);
        taskService.completeObservationTasks(processInstance, messageReferenceMessageDTO, message);
        // Check for tasks that have been planned in the current iteration, which may have been completed by
        // events received earlier
        taskService.evaluatePlannedTasksCompletedByExistingMessages(plannedDomainEventTasks);
        // Complete tasks completed by the current event
        taskService.evaluateCompletedTasks(processInstance, messageReferenceMessageDTO);
        evaluateProcessRelations(processInstance, message);
        processInstance.updateState();
    }

    private void evaluateProcessRelations(ProcessInstance processInstance, Message message) {
        List<ProcessRelationPattern> patterns = processInstance.getProcessTemplate()
                .getProcessRelationPatternsByMessageName(message.getMessageName());
        if (patterns.isEmpty()) {
            return;
        }

        Set<MessageData> messageDataSet = message.getMessageData(processInstance.getProcessTemplateName());
        List<ProcessRelation> newRelations = new ArrayList<>();
        for (ProcessRelationPattern pattern : patterns) {
            String messageKey = pattern.getSource().getMessageDataKey();
            for (MessageData messageData : messageDataSet) {
                if (messageKey.equals(messageData.getKey())) {
                    ProcessRelation processRelation = ProcessRelation.createMatchingProcessRelation(processInstance, pattern, messageData.getValue());
                    if (!processRelationRepository.exists(processInstance.getId(), processRelation)) {
                        newRelations.add(processRelation);
                    }
                }
            }
        }

        if (!newRelations.isEmpty()) {
            processRelationRepository.saveAll(newRelations);
            log.debug("Saved {} new process relations for process instance {}", newRelations.size(), processInstance.getId());
        }
    }

    private void correlateMessagesByProcessDataIfRequired(ProcessInstance processInstance, List<ProcessData> newProcessData) {
        // Optimization: If no process templates define any event correlation by process data, or no new process data
        // has been added, we exit early here to avoid costly correlation queries.
        if (!processTemplateRepository.isAnyTemplateHasEventsCorrelatedByProcessData() || newProcessData.isEmpty()) {
            return;
        }

        // Optimization: Most PCS process templates do not define any event correlation by process data, so we exit
        // early here to avoid costly correlation queries.
        String originProcessId = processInstance.getOriginProcessId();
        boolean anyEventsCorrelatedByProcessData = isAnyEventsCorrelatedByProcessData(processInstance.getProcessTemplateName());
        if (!anyEventsCorrelatedByProcessData) {
            log.trace("Process template of process with originProcessId '{}' does not define any event correlation by process data", originProcessId);
            return;
        }

        correlateMessagesByProcessData(processInstance, newProcessData);
    }

    private boolean isAnyEventsCorrelatedByProcessData(String templateName) {
        return processTemplateRepository.findByName(templateName)
                .map(ProcessTemplate::isAnyEventCorrelatedByProcessData)
                .orElse(false);
    }

    private void correlateMessagesByProcessData(ProcessInstance processInstance, List<ProcessData> newProcessData) {
        processInstanceRepository.flush();

        List<Message> eventsToCorrelate = new ArrayList<>();
        List<UUID> alreadyCorrelatedEventIds = messageReferenceRepository.findByProcessInstanceId(processInstance.getId())
                .stream().map(MessageReferenceMessageDTO::getMessageId).toList();

        for (ProcessData processData : newProcessData) {
            Set<MessageReference> messageReferences = processInstance.getProcessTemplate().getDomainEventReferencesCorrelatedBy(processData.getKey());
            log.debug("Found {} messageReferences with processDataKey {}", messageReferences.size(), processData.getKey());

            for (MessageReference messageReference : messageReferences) {
                List<Message> messageRepositoryEventsToCorrelate = findEventsToCorrelateByProcessData(processInstance, processData, messageReference, alreadyCorrelatedEventIds);
                eventsToCorrelate.addAll(messageRepositoryEventsToCorrelate);
            }
        }

        if (!eventsToCorrelate.isEmpty()) {
            log.info("Found {} old messages in the db to correlate with this process instance", eventsToCorrelate.size());
        }

        for (Message message : eventsToCorrelate) {
            applyProcessUpdateAndReEvaluateProcessInstance(processInstance, message);
        }
    }

    private List<Message> findEventsToCorrelateByProcessData(ProcessInstance processInstance, ProcessData processData, MessageReference messageReference, List<UUID> alreadyCorrelatedEventIds) {
        if (processData.getRole() != null) {
            if (alreadyCorrelatedEventIds.isEmpty()) {
                return messageRepository.findMessagesToCorrelate(messageReference.getMessageName(), processInstance.getProcessTemplateName(), messageReference.getCorrelatedByProcessData().getMessageDataKey(), processData.getValue(), processData.getRole());
            }
            return messageRepository.findMessagesToCorrelate(messageReference.getMessageName(), processInstance.getProcessTemplateName(), messageReference.getCorrelatedByProcessData().getMessageDataKey(), processData.getValue(), processData.getRole(), alreadyCorrelatedEventIds);
        } else {
            if (alreadyCorrelatedEventIds.isEmpty()) {
                return messageRepository.findMessagesToCorrelate(messageReference.getMessageName(), processInstance.getProcessTemplateName(), messageReference.getCorrelatedByProcessData().getMessageDataKey(), processData.getValue());
            }
            return messageRepository.findMessagesToCorrelate(messageReference.getMessageName(), processInstance.getProcessTemplateName(), messageReference.getCorrelatedByProcessData().getMessageDataKey(), processData.getValue(), alreadyCorrelatedEventIds);
        }
    }

    private static class ProcessUpdateFailedException extends RuntimeException {

        private ProcessUpdateFailedException(String message, Throwable cause) {
            super(message, cause);
        }

        private static ProcessUpdateFailedException withCause(String originProcessId, Throwable cause) {
            String message = "Failed updating process with origin process ID %s" + originProcessId;
            return new ProcessUpdateFailedException(message, cause);
        }
    }
}
