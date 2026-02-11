package ch.admin.bit.jeap.processcontext.domain.message;

import ch.admin.bit.jeap.messaging.kafka.tracing.TraceContextProvider;
import ch.admin.bit.jeap.messaging.model.MessagePayload;
import ch.admin.bit.jeap.messaging.model.MessageReferences;
import ch.admin.bit.jeap.messaging.model.MessageUser;
import ch.admin.bit.jeap.processcontext.domain.port.MessageConsumerFactory;
import ch.admin.bit.jeap.processcontext.domain.port.MetricsListener;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceQueryRepository;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.CorrelatedByProcessData;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.MessageReference;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import ch.admin.bit.jeap.processcontext.domain.processupdate.ProcessUpdateService;
import ch.admin.bit.jeap.processcontext.plugin.api.message.PayloadExtractor;
import ch.admin.bit.jeap.processcontext.plugin.api.message.ReferenceExtractor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.function.Supplier;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;

@Component
@RequiredArgsConstructor
@Slf4j
class MessageService implements MessageReceiver {
    private final ProcessTemplateRepository processTemplateRepository;
    private final MessageConsumerFactory messageConsumerFactory;
    private final MessageRepository messageRepository;
    private final ProcessUpdateService processUpdateService;
    private final ProcessInstanceQueryRepository processInstanceQueryRepository;
    private final PlatformTransactionManager transactionManager;
    private final MetricsListener metricsListener;
    private final TraceContextProvider traceContextProvider;

    @EventListener
    public void onAppStarted(ApplicationStartedEvent ignored) {
        startDomainEventListeners();
    }

    void startDomainEventListeners() {
        Set<MessageDto> eventTopics = processTemplateRepository.getAllTemplates().stream()
                .flatMap(template -> template.getMessageReferences().stream())
                .map(messageRef -> MessageDto.of(messageRef.getMessageName(), messageRef.getTopicName(), messageRef.getClusterName()))
                .collect(toSet());

        eventTopics.forEach(messageDto ->
                messageConsumerFactory.startConsumer(
                        messageDto.getTopicName(),
                        messageDto.getMessageName(),
                        messageDto.getClusterName(),
                        this));
    }

    @Override
    public void messageReceived(ch.admin.bit.jeap.messaging.model.Message message) {
        log.debug("Received message: {}", message.getType().getName());
        metricsListener.timed("jeap_pcs_process_message", Map.of(), () -> processMessage(message));
        log.debug("Processed message: {}", message.getType().getName());
    }

    private void processMessage(ch.admin.bit.jeap.messaging.model.Message message) {
        log.debug("Processing message '{}'", message.getType().getName());
        // Extract message data and origin task ids from the message
        Set<MessageData> templateMessageData = new HashSet<>();
        Set<OriginTaskId> templateOriginTaskIds = new HashSet<>();
        var messageReferencesByTemplateNameForMessageName = processTemplateRepository.getMessageReferencesByTemplateNameForMessageName(message.getType().getName());
        messageReferencesByTemplateNameForMessageName.forEach((templateName, messageReference) -> {
            Set<ch.admin.bit.jeap.processcontext.plugin.api.message.MessageData> messageData = getMessageData(message, messageReference);
            templateMessageData.addAll(MessageData.from(templateName, messageData));
            Set<String> originTaskIds = getRelatedOriginTaskIds(message, messageReference);
            templateOriginTaskIds.addAll(OriginTaskId.from(templateName, originTaskIds));
        });

        // Create a Message instance with the extracted data.
        Message messageEntity = withinTransaction(() -> createAndSaveMessageIfNeeded(message, templateMessageData, templateOriginTaskIds));

        // Correlate message to origin process IDs. In a separate transaction from writing the message to make sure
        // correlation by process data always gets executed either here or as late correlation during a process update.
        Set<String> correlatedOriginProcessIds = new HashSet<>();
        metricsListener.timed("jeap_pcs_early_correlate_message", emptyMap(), () -> {
            Set<String> originProcessIds = withinReadOnlyTransaction(() -> correlateMessage(message, templateMessageData));
            correlatedOriginProcessIds.addAll(originProcessIds);
        });

        // Initiate process updates for the origin process IDs correlated with the message.
        correlatedOriginProcessIds.forEach(originProcessId ->
                produceProcessUpdateMessage(messageEntity, message, originProcessId));
    }

    private void produceProcessUpdateMessage(Message messageEntity, ch.admin.bit.jeap.messaging.model.Message message, String originProcessId) {
        Optional<String> templateName = getTemplateNameIfProcessInstantingMessage(message, messageEntity, originProcessId);
        if (templateName.isPresent()) {
            processUpdateService.processCreatingMessageReceived(originProcessId, messageEntity, templateName.get());
        } else {
            processUpdateService.messageReceived(originProcessId, messageEntity);
        }
    }

    private Optional<String> getTemplateNameIfProcessInstantingMessage(ch.admin.bit.jeap.messaging.model.Message message, Message messageEntity, String originProcessId) {
        Set<String> processTemplateNamesForInstantiation = getProcessTemplateNamesInstantiatedByMessage(message);
        // Not a message that triggers process creation
        if (processTemplateNamesForInstantiation.isEmpty()) {
            return Optional.empty();
        }
        // Message triggers more than one process type, this is a misconfiguration in the process templates as we cannot
        // start more than one process for the same origin process id. Log an error and skip process creation in this case.
        if (processTemplateNamesForInstantiation.size() > 1) {
            log.error("Message '{}' is configured to start a process in more than one process template ({}). Cannot start more than one " +
                            "process for the same origin process id ({}). Skipping process creation. Make sure that the process instantiation " +
                            "conditions configured on the same message in different process templates compute mutual exclusive results.",
                    messageEntity.getMessageName(), String.join(",", processTemplateNamesForInstantiation), originProcessId);
            return Optional.empty();
        }
        // Single process template for instantiation found, return it
        return processTemplateNamesForInstantiation.stream().findFirst();
    }

    private Set<String> getProcessTemplateNamesInstantiatedByMessage(ch.admin.bit.jeap.messaging.model.Message message) {
        return processTemplateRepository.getMessageReferencesByTemplateNameForMessageName(message.getType().getName())
                .entrySet().stream()
                .filter(entry ->
                        entry.getValue().getProcessInstantiationCondition().triggersProcessInstantiation(message))
                .map(Map.Entry::getKey)
                .collect(toSet());
    }

    private Message createAndSaveMessageIfNeeded(ch.admin.bit.jeap.messaging.model.Message message, Set<MessageData> messageData, Set<OriginTaskId> originTaskIds) {
        // Don't save the same message twice (idempotence)
        return messageRepository.findByMessageNameAndIdempotenceId(message.getType().getName(), message.getIdentity().getIdempotenceId())
                .map(messageEntity -> {
                    metricsListener.messageReceived(message.getType(), false);
                    return messageEntity;
                })
                .orElseGet(() -> {
                    metricsListener.messageReceived(message.getType(), true);
                    return saveMessage(message, messageData, originTaskIds, getTraceId());
                });
    }

    private Set<String> correlateMessage(ch.admin.bit.jeap.messaging.model.Message message, Set<MessageData> templateMessageData) {
        var messageReferencesByTemplateName = processTemplateRepository.getMessageReferencesByTemplateNameForMessageName(message.getType().getName());
        Set<String> originProcessIds = new HashSet<>();
        messageReferencesByTemplateName.forEach((templateName, messageReference) -> {
            originProcessIds.addAll(getProcessIdsCorrelatedByCorrelationProvider(message, messageReference));
            originProcessIds.addAll(getProcessIdsCorrelatedByProcessData(messageReference, templateMessageData, templateName));
        });
        return originProcessIds;
    }

    private Set<String> getProcessIdsCorrelatedByProcessData(MessageReference messageReference, Set<MessageData> templateMessageData, String templateName) {
        CorrelatedByProcessData correlatedByProcessData = messageReference.getCorrelatedByProcessData();
        if (correlatedByProcessData == null) {
            return emptySet();
        }

        return templateMessageData.stream()
                .filter(ed -> ed.getTemplateName().equals(templateName))
                .filter(ed -> correlatedByProcessData.getMessageDataKey().equals(ed.getKey()))
                .map(ed -> processInstanceQueryRepository.findUncompletedProcessInstancesHavingProcessData(
                        templateName, correlatedByProcessData.getProcessDataKey(), ed.getValue(), ed.getRole()))
                .flatMap(Collection::stream)
                .collect(toSet());
    }

    private Message saveMessage(ch.admin.bit.jeap.messaging.model.Message message, Set<MessageData> messageData, Set<OriginTaskId> originTaskIds, String traceId) {
        Message newMessage = Message.messageBuilder()
                .messageId(message.getIdentity().getId())
                .idempotenceId(message.getIdentity().getIdempotenceId())
                .traceId(traceId)
                .messageName(message.getType().getName())
                .messageData(messageData)
                .userData(getUserDataFromMessage(message))
                .originTaskIds(originTaskIds)
                .messageCreatedAt(message.getIdentity().getCreatedZoned())
                .build();
        return messageRepository.save(newMessage);
    }

    private Set<MessageUserData> getUserDataFromMessage(ch.admin.bit.jeap.messaging.model.Message message) {
        Set<MessageUserData> userData = new HashSet<>();
        Optional<? extends MessageUser> optionalUser = message.getOptionalUser();
        if (optionalUser.isPresent()) {
            MessageUser messageUser = optionalUser.get();
            addNullableField(userData, MessageUserData.KEY_ID, messageUser.getId());
            addNullableField(userData, MessageUserData.KEY_FAMILY_NAME, messageUser.getFamilyName());
            addNullableField(userData, MessageUserData.KEY_GIVEN_NAME, messageUser.getGivenName());
            addNullableField(userData, MessageUserData.KEY_BUSINESS_PARTNER_NAME, messageUser.getBusinessPartnerName());
            addNullableField(userData, MessageUserData.KEY_BUSINESS_PARTNER_ID, messageUser.getBusinessPartnerId());
            messageUser.getPropertiesMap().forEach((key, value) -> userData.add(new MessageUserData(key, value)));
        }
        return userData;
    }

    private void addNullableField(Set<MessageUserData> userData, String key, String value) {
        if (value != null) {
            userData.add(new MessageUserData(key, value));
        }
    }

    private Set<String> getProcessIdsCorrelatedByCorrelationProvider(ch.admin.bit.jeap.messaging.model.Message message, MessageReference messageReference) {
        Set<String> relatedOriginProcessIds = messageReference.getCorrelationProvider().getOriginProcessIds(message);
        log.debug("Related origin process ids extracted from message {}: {}", message.getType().getName(), relatedOriginProcessIds);
        return relatedOriginProcessIds;
    }

    private Set<String> getRelatedOriginTaskIds(ch.admin.bit.jeap.messaging.model.Message message, MessageReference messageReference) {
        Set<String> relatedOriginTaskIds = messageReference.getCorrelationProvider().getRelatedOriginTaskIds(message);
        log.debug("Related origin task ids extracted from message {}: {}", message.getType().getName(), relatedOriginTaskIds);
        return relatedOriginTaskIds;
    }

    private Set<ch.admin.bit.jeap.processcontext.plugin.api.message.MessageData> getMessageData(ch.admin.bit.jeap.messaging.model.Message message, MessageReference messageReference) {
        Set<ch.admin.bit.jeap.processcontext.plugin.api.message.MessageData> messageData = new HashSet<>(
                getMessageDataFromPayload(message, messageReference));
        messageData.addAll(getMessageDataFromReferences(message, messageReference));
        return messageData;
    }

    private Set<ch.admin.bit.jeap.processcontext.plugin.api.message.MessageData> getMessageDataFromPayload(ch.admin.bit.jeap.messaging.model.Message message, MessageReference messageReference) {
        Optional<? extends MessagePayload> optionalPayload = message.getOptionalPayload();
        if (optionalPayload.isEmpty()) {
            return emptySet();
        }
        PayloadExtractor<MessagePayload> payloadExtractor = messageReference.getPayloadExtractor();
        MessagePayload payload = optionalPayload.get();
        Set<ch.admin.bit.jeap.processcontext.plugin.api.message.MessageData> eventData = new HashSet<>(payloadExtractor
                .getMessageData(payload));
        Set<ch.admin.bit.jeap.processcontext.plugin.api.message.MessageData> messageData = payloadExtractor.getMessageData(payload);
        Set<ch.admin.bit.jeap.processcontext.plugin.api.message.MessageData> data = new HashSet<>(messageData);
        data.addAll(eventData);
        log.debug("Message data extracted from payload of message {}: {}", message.getType().getName(), data);
        return data;
    }

    private Set<ch.admin.bit.jeap.processcontext.plugin.api.message.MessageData> getMessageDataFromReferences(ch.admin.bit.jeap.messaging.model.Message message, MessageReference messageReference) {
        MessageReferences messageReferences = message.getOptionalReferences().orElse(null);
        if (messageReferences == null) {
            return emptySet();
        }
        ReferenceExtractor<MessageReferences> referenceExtractor = messageReference.getReferenceExtractor();
        Set<ch.admin.bit.jeap.processcontext.plugin.api.message.MessageData> eventData = new HashSet<>(referenceExtractor
                .getMessageData(messageReferences));

        Set<ch.admin.bit.jeap.processcontext.plugin.api.message.MessageData> data = new HashSet<>(referenceExtractor.getMessageData(messageReferences));
        data.addAll(eventData);
        log.debug("Message data extracted from reference of message {}: {}", message.getType().getName(), data);
        return data;
    }

    private <T> T withinTransaction(Supplier<T> callable) {
        return withinTransaction(callable, false);
    }

    private <T> T withinReadOnlyTransaction(Supplier<T> callable) {
        return withinTransaction(callable, true);
    }

    private <T> T withinTransaction(Supplier<T> callable, boolean readOnly) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setReadOnly(readOnly);
        return transactionTemplate.execute(status -> callable.get());
    }

    private String getTraceId() {
        return traceContextProvider.getTraceContext() != null ? traceContextProvider.getTraceContext().getTraceIdString() : null;
    }

    @Value(staticConstructor = "of")
    private static class MessageDto {
        @NonNull
        String messageName;
        @NonNull
        String topicName;
        String clusterName;
    }

}
