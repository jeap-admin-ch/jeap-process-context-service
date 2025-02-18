package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.PcsConfigProperties;
import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.message.MessageRepository;
import ch.admin.bit.jeap.processcontext.domain.port.InternalMessageProducer;
import ch.admin.bit.jeap.processcontext.domain.port.MetricsListener;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.MessageReference;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.*;
import ch.admin.bit.jeap.processcontext.domain.processupdate.ProcessUpdate;
import ch.admin.bit.jeap.processcontext.domain.processupdate.ProcessUpdateQueryRepository;
import ch.admin.bit.jeap.processcontext.domain.processupdate.ProcessUpdateRepository;
import ch.admin.bit.jeap.processcontext.domain.processupdate.ProcessUpdateType;
import ch.admin.bit.jeap.processcontext.domain.tx.Transactions;
import ch.admin.bit.jeap.processcontext.plugin.api.condition.TaskInstantiationCondition;
import com.google.common.collect.Lists;
import io.micrometer.core.annotation.Timed;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessContextFactory.createMessage;
import static net.logstash.logback.argument.StructuredArguments.keyValue;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessInstanceService {
    private final InternalMessageProducer internalMessageProducer;
    private final ProcessUpdateQueryRepository processUpdateQueryRepository;
    private final ProcessInstanceRepository processInstanceRepository;
    private final ProcessTemplateRepository processTemplateRepository;
    private final MessageRepository messageRepository;
    private final ProcessUpdateRepository processUpdateRepository;
    private final ProcessSnapshotService processSnapshotService;
    private final Transactions transactions;
    private final MetricsListener metricsListener;
    private final PcsConfigProperties pcsConfigProperties;

    public void createProcessInstance(String originProcessId, String processTemplateName, Set<ProcessData> processData) {
        // Create & persist a process instance unless it already exists (idempotency)
        // Trigger process instance state update after the DB is committed as the consumer of this message needs it.
        // The consumer needs to be idempotent, so producing the message twice in case of error will have no effect.
        transactions.withinNewTransaction(() -> {
            if (!processInstanceRepository.existsByOriginProcessId(originProcessId)) {
                createFromTemplate(originProcessId, processTemplateName, processData);

                ProcessUpdate processUpdate = ProcessUpdate.processCreated()
                        .originProcessId(originProcessId)
                        .build();
                processUpdateRepository.save(processUpdate);
            }
        });
        internalMessageProducer.produceProcessContextOutdatedEventSynchronously(originProcessId);
    }

    private ProcessInstance createFromTemplate(String originProcessId, String processTemplateName, Set<ProcessData> processData) {
        ProcessTemplate processTemplate = processTemplateRepository.findByName(processTemplateName)
                .orElseThrow(NotFoundException.templateNotFound(processTemplateName, originProcessId));
        ProcessInstance processInstance = ProcessInstance.startProcess(originProcessId, processTemplate, processData);

        log.info("Creating process {} with origin process ID {} from template {}", processInstance.getId(), originProcessId, processTemplateName);
        metricsListener.processInstanceCreated(processTemplateName);
        return processInstanceRepository.save(processInstance);
    }

    @Timed(value = "jeap_pcs_update_process_state", description = "Update process state", percentiles = {0.5, 0.8, 0.95, 0.99})
    public void updateProcessState(String originProcessId) {
        List<ProcessUpdate> processUpdates = processUpdateQueryRepository.findByOriginProcessIdAndHandledFalse(originProcessId).stream()
                // move process updates of type CREATE_PROCESS to the front of the list
                .sorted(Comparator.comparingInt(pu -> pu.getProcessUpdateType().getPriority()))
                .toList();
        if (processUpdates.isEmpty()) {
            // If we get a request to update the process state but there are no process updates, the cause could be a
            // process template migration requested by the process instance migration service.
            migrateProcessInstanceTemplateIfNeeded(originProcessId);
        }
        Lists.partition(processUpdates, pcsConfigProperties.getProcessInstanceUpdateApplicationBatchSize()).forEach(
                batch -> processUpdates(originProcessId, batch)
        );
        if (transactions.withinNewTransactionWithResult(() -> processInstanceRepository.existsByOriginProcessId(originProcessId))) {
            internalMessageProducer.produceProcessContextStateChangedEventSynchronously(originProcessId);
        }
        metricsListener.timed("jeap_pcs_late_correlate_message", Collections.emptyMap(),
                () -> correlateMessagesByProcessData(originProcessId));
    }

    private void migrateProcessInstanceTemplateIfNeeded(String originProcessId) {
        transactions.withinNewTransaction(() -> {
            processInstanceRepository.findProcessInstanceTemplate(originProcessId).ifPresent(processInstanceTemplate -> {
                processTemplateRepository.findByName(processInstanceTemplate.getTemplateName()).ifPresent(template -> {
                    if (!template.getTemplateHash().equals(processInstanceTemplate.getTemplateHash())) {
                        processInstanceRepository.findByOriginProcessIdLoadingMessages(originProcessId).ifPresent(
                                ProcessInstance::applyTemplateMigrationIfChanged);
                    }
                });
            });
        });
    }

    private void processUpdates(String originProcessId, List<ProcessUpdate> processUpdates) {
        if (processUpdates.isEmpty()) {
            return;
        }

        metricsListener.timed("pcs_process_batch_update", Map.of("batchSize", getBatchSizeGroup(processUpdates)), () ->
                handleProcessUpdateBatch(originProcessId, processUpdates));
    }

    private void handleProcessUpdateBatch(String originProcessId, List<ProcessUpdate> processUpdates) {
        List<ProcessUpdate> remainingUpdates = transactions.withinNewTransactionWithTxStatusAndResult(txStatus -> {
            try {
                ProcessInstance processInstance = createOrLoadProcessInstance(originProcessId, processUpdates).orElse(null);
                if (processInstance == null) {
                    log.debug("Process instance for process " + keyValue("originProcessId", originProcessId) + " not found, won't handle the corresponding process updates (yet).");
                    return List.of();
                }

                processInstance.applyTemplateMigrationIfChanged();
                processUpdates(processInstance, processUpdates);
                markHandled(processUpdates);
                metricsListener.processUpdateProcessed(processInstance.getProcessTemplate(), true, processUpdates.size());
                return List.of();
            } catch (ProcessUpdateFailedException pufe) {
                txStatus.setRollbackOnly();
                ProcessTemplate processTemplate = createOrLoadProcessInstance(originProcessId, processUpdates)
                        .map(ProcessInstance::getProcessTemplate)
                        .orElse(null);
                metricsListener.processUpdateProcessed(processTemplate, false, 1);
                failUpdate(pufe.getFailedProcessUpdate(), pufe);
                return processUpdates.stream()
                        .filter(u -> u != pufe.getFailedProcessUpdate())
                        .toList();
            }
        });

        if (!remainingUpdates.isEmpty()) {
            if (remainingUpdates.size() < processUpdates.size()) {
                processUpdates(originProcessId, remainingUpdates);
            } else {
                String msg = String.format("This should not have happened. There must be less remaining updates (%s) than initial updates (%s).",
                        remainingUpdates.size(), processUpdates.size());
                throw new IllegalStateException(msg);
            }
        }
    }


    private Optional<ProcessInstance> createOrLoadProcessInstance(String originProcessId, List<ProcessUpdate> processUpdates) {
        return processUpdates.stream()
                .filter(update -> update.getProcessUpdateType() == ProcessUpdateType.CREATE_PROCESS)
                .findFirst()
                .map(createProcessUpdate -> {
                    try {
                        return Optional.of(createOrGetProcessInstance(createProcessUpdate));
                    } catch (Exception e) {
                        throw ProcessUpdateFailedException.createProcessUpdateFailed(createProcessUpdate, e);
                    }
                })
                .orElse(processInstanceRepository.findByOriginProcessIdLoadingMessages(originProcessId));
    }

    private ProcessInstance createOrGetProcessInstance(ProcessUpdate processUpdate) {
        String originProcessId = processUpdate.getOriginProcessId();
        // Has this process instance already been created?
        Optional<ProcessInstance> pio = processInstanceRepository.findByOriginProcessIdLoadingMessages(originProcessId);
        // Don't create the process instance again, simply return the existing one
        return pio.orElseGet(() ->
                createFromTemplate(originProcessId, processUpdate.getParams(), Set.of()));
    }

    private void processUpdates(ProcessInstance processInstance, List<ProcessUpdate> processUpdates) {
        for (ProcessUpdate processUpdate : processUpdates) {
            try {
                applyProcessUpdateAndReEvaluateProcessInstance(processInstance, processUpdate);
            } catch (Exception e) {
                throw ProcessUpdateFailedException.createProcessUpdateFailed(processUpdate, e);
            }
        }
    }

    private void failUpdate(ProcessUpdate processUpdate, Exception e) {
        log.error("Failed to apply process update to process " + keyValue("originProcessId", processUpdate.getOriginProcessId()), e);
        transactions.withinNewTransaction(() -> processUpdateRepository.markHandlingFailed(processUpdate.getId()));
        metricsListener.processUpdateFailed(processUpdate, e);
    }

    private void markHandled(Collection<ProcessUpdate> processUpdates) {
        processUpdates.forEach(update -> processUpdateRepository.markHandled(update.getId()));
    }

    private void applyProcessUpdateAndReEvaluateProcessInstance(ProcessInstance processInstance, ProcessUpdate update) {
        ZonedDateTime timestamp = ZonedDateTime.now();
        final Optional<UUID> reference = update.getMessageReference();
        if (reference.isPresent()) {
            Message message = messageRepository.findById(reference.get())
                    .orElseThrow(NotFoundException.messageNotFound(reference.get(), processInstance.getOriginProcessId()));
            MessageReferenceMessageDTO messageReferenceMessageDTO = processInstance.addMessage(message);
            metricsListener.timed("pcs_process_single_update", Map.of("updateType", update.getProcessUpdateType().name()),
                    () -> updateProcessInstance(processInstance, update, messageReferenceMessageDTO, message));
            timestamp = message.getMessageCreatedAt();
        }
        processInstance.evaluateCompletedTasksAndReachedMilestones(timestamp);
        createProcessSnapshotIfTriggered(processInstance);
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

    private void updateProcessInstance(ProcessInstance processInstance, ProcessUpdate processUpdate, MessageReferenceMessageDTO messageReferenceMessageDTO, Message message) {
        switch (processUpdate.getProcessUpdateType()) {
            case CREATE_PROCESS, DOMAIN_EVENT:
                createMessageTasks(processUpdate, processInstance, messageReferenceMessageDTO, message);
                completeObservationTasks(processUpdate, processInstance, messageReferenceMessageDTO, message);
                processInstance.evaluateCompletedTasks(messageReferenceMessageDTO, message.getMessageCreatedAt());
                processInstance.evaluateRelations();
                processInstance.evaluateProcessRelations(message);
                break;
            case PROCESS_CREATED:
                processInstance.evaluateRelations();
                processInstance.evaluateProcessRelations(message);
                break;
            default:
                throw new RuntimeException("Update Type " + processUpdate.getProcessUpdateType() + " does not exist");
        }
    }

    private void createMessageTasks(ProcessUpdate processUpdate, ProcessInstance processInstance, MessageReferenceMessageDTO messageReferenceMessageDTO, Message message) {
        processInstance.getProcessTemplate().getTaskTypes().stream()
                .filter(taskType -> processUpdate.getMessageName().equals(taskType.getPlannedByDomainEvent()))
                .forEach(taskType -> {
                    TaskInstantiationCondition instantiationCondition = taskType.getInstantiationCondition();
                    if (instantiationCondition == null || instantiationCondition.instantiate(createMessage(messageReferenceMessageDTO))) {
                        if (taskType.getCardinality() == TaskCardinality.SINGLE_INSTANCE) {
                            planSingleInstanceDomainEventTaskIfNotExists(processInstance, messageReferenceMessageDTO, taskType, message.getMessageId(), message.getMessageCreatedAt(), message.getId());
                        } else {
                            messageReferenceMessageDTO.getRelatedOriginTaskIds()
                                    .forEach(originTaskId -> processInstance.planDomainEventTask(taskType, originTaskId, message.getMessageCreatedAt(), message.getId()));
                        }
                    }
                });
    }

    private void planSingleInstanceDomainEventTaskIfNotExists(ProcessInstance processInstance, MessageReferenceMessageDTO eventReference, TaskType taskType, String eventId, ZonedDateTime timestamp, UUID messageId) {
        List<TaskInstance> existingTaskInstancesForTaskType = processInstance.getTasks().stream()
                .filter(task -> task.getTaskType().isPresent())
                .filter(task -> task.getTaskType().get().equals(taskType)).toList();
        Set<String> relatedOriginTaskIds = eventReference.getRelatedOriginTaskIds();
        if (relatedOriginTaskIds.size() > 1) {
            throw TaskPlanningException.singleInstanceTaskMultipleIds(taskType, relatedOriginTaskIds);
        }

        String taskId = relatedOriginTaskIds.isEmpty() ? eventId : relatedOriginTaskIds.iterator().next();
        if (!existingTaskInstancesForTaskType.isEmpty()) {
            boolean isNewTaskId = existingTaskInstancesForTaskType.stream()
                    .noneMatch(instance -> instance.getOriginTaskId().equals(taskId));
            if (isNewTaskId) {
                throw TaskPlanningException.createTaskAlreadyPlanned(taskId, taskType, existingTaskInstancesForTaskType);
            }
            return;
        }
        processInstance.planDomainEventTask(taskType, taskId, timestamp, messageId);
    }

    private void completeObservationTasks(ProcessUpdate processUpdate, ProcessInstance processInstance, MessageReferenceMessageDTO messageReferenceMessageDTO, Message message) {
        processInstance.getProcessTemplate().getTaskTypes().stream()
                .filter(taskType ->
                        (TaskLifecycle.OBSERVED == taskType.getLifecycle()) &&
                        (processUpdate.getMessageName().equals(taskType.getObservesDomainEvent())))
                .forEach(taskType -> {
                    TaskInstantiationCondition instantiationCondition = taskType.getInstantiationCondition();
                    if (instantiationCondition == null || instantiationCondition.instantiate(createMessage(messageReferenceMessageDTO))) {
                        processInstance.addObservationTask(taskType, message.getMessageId(), message.getMessageCreatedAt(), message.getId());
                    }
                });
    }

    private void correlateMessagesByProcessData(String originProcessId) {
        boolean newEventsCorrelated = transactions.withinNewTransactionWithResult(() -> {
            Optional<ProcessInstance> processInstance = processInstanceRepository.findByOriginProcessIdLoadingMessages(originProcessId);
            if (processInstance.isPresent()) {
                return correlateMessagesByProcessData(processInstance.get());
            } else {
                log.info("Process with originProcessId '{}' not found. There is no message to correlate.", originProcessId);
                return false;
            }
        });
        if (newEventsCorrelated) {
            internalMessageProducer.produceProcessContextOutdatedEventSynchronously(originProcessId);
        }
    }

    private boolean correlateMessagesByProcessData(ProcessInstance processInstance) {
        ZonedDateTime lastCorrelationAt = processInstance.getLastCorrelationAt() != null ? processInstance.getLastCorrelationAt() : ZonedDateTime.of(LocalDateTime.MIN, ZoneId.systemDefault());

        List<ProcessData> processDataList = processInstance.getProcessData().stream()
                .filter(pd -> pd.getCreatedAt().isAfter(lastCorrelationAt)).toList();
        log.debug("Found {} processData created after the last correlation of this process instance at {}", processDataList.size(), lastCorrelationAt.format(DateTimeFormatter.ISO_DATE_TIME));

        List<Message> eventsToCorrelate = new ArrayList<>();
        List<UUID> alreadyCorrelatedEventIds = processInstance.getMessageReferences().stream().map(MessageReferenceMessageDTO::getMessageId).toList();

        for (ProcessData processData : processDataList) {
            Set<MessageReference> messageReferences = processInstance.getProcessTemplate().getDomainEventReferencesCorrelatedBy(processData.getKey());
            log.debug("Found {} messageReferences with processDataKey {}", messageReferences.size(), processData.getKey());

            for (MessageReference messageReference : messageReferences) {
                List<Message> messageRepositoryEventsToCorrelate;
                if (processData.getRole() != null) {
                    if (alreadyCorrelatedEventIds.isEmpty()) {
                        messageRepositoryEventsToCorrelate = messageRepository.findMessagesToCorrelate(messageReference.getMessageName(), processInstance.getProcessTemplateName(), messageReference.getCorrelatedByProcessData().getMessageDataKey(), processData.getValue(), processData.getRole());
                    } else {
                        messageRepositoryEventsToCorrelate = messageRepository.findMessagesToCorrelate(messageReference.getMessageName(), processInstance.getProcessTemplateName(), messageReference.getCorrelatedByProcessData().getMessageDataKey(), processData.getValue(), processData.getRole(), alreadyCorrelatedEventIds);
                    }
                } else {
                    if (alreadyCorrelatedEventIds.isEmpty()) {
                        messageRepositoryEventsToCorrelate = messageRepository.findMessagesToCorrelate(messageReference.getMessageName(), processInstance.getProcessTemplateName(), messageReference.getCorrelatedByProcessData().getMessageDataKey(), processData.getValue());
                    } else {
                        messageRepositoryEventsToCorrelate = messageRepository.findMessagesToCorrelate(messageReference.getMessageName(), processInstance.getProcessTemplateName(), messageReference.getCorrelatedByProcessData().getMessageDataKey(), processData.getValue(), alreadyCorrelatedEventIds);
                    }
                }
                log.debug("Found {} old messages with messageName {}, messageDataKey {}, value {}, role {}", messageRepositoryEventsToCorrelate.size(), messageReference.getMessageName(), messageReference.getCorrelatedByProcessData().getMessageDataKey(), processData.getValue(), processData.getRole());
                eventsToCorrelate.addAll(messageRepositoryEventsToCorrelate);
            }
        }

        log.debug("Found {} old messages in the db to correlate with this process instance", eventsToCorrelate.size());

        for (Message message : eventsToCorrelate) {
            var processUpdate = ProcessUpdate.messageReceived()
                    .originProcessId(processInstance.getOriginProcessId())
                    .messageReference(message.getId())
                    .messageName(message.getMessageName())
                    .idempotenceId(message.getIdempotenceId())
                    .build();
            processUpdateRepository.save(processUpdate);
        }

        //Update the last correlation timestamp for this process instance
        processInstance.correlatedAt(processDataList.stream().map(ProcessData::getCreatedAt).max(ZonedDateTime::compareTo).orElse(ZonedDateTime.now()));

        return !eventsToCorrelate.isEmpty();
    }

    private String getBatchSizeGroup(List<ProcessUpdate> processUpdates) {
        int num = processUpdates.size();
        if (num <= 5) {
            return Integer.toString(num);
        }
        if (num <= 10) {
            return "5<n<11";
        }
        if (num <= 25) {
            return "10<n<26";
        }
        if (num <= 50) {
            return "25<n<51";
        }
        if (num <= 100) {
            return "50<n<100";
        }
        return "100<n";
    }

    private static class ProcessUpdateFailedException extends RuntimeException {

        @Getter
        private final transient ProcessUpdate failedProcessUpdate; // for sonar: this exception will never be serialized

        private ProcessUpdateFailedException(ProcessUpdate processUpdate, String message, Throwable cause) {
            super(message, cause);
            this.failedProcessUpdate = processUpdate;
        }

        private static ProcessUpdateFailedException createProcessUpdateFailed(ProcessUpdate processUpdate, Throwable cause) {
            String message = String.format("Failed creating process with originId %s for a create process update for the template %s and the message %s.",
                    processUpdate.getOriginProcessId(), processUpdate.getParams(), processUpdate.getMessageName());
            return new ProcessUpdateFailedException(processUpdate, message, cause);
        }

    }

}
