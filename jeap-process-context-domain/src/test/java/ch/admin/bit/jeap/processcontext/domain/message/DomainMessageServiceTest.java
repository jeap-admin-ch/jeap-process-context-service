package ch.admin.bit.jeap.processcontext.domain.message;

import ch.admin.bit.jeap.messaging.avro.AvroMessageUser;
import ch.admin.bit.jeap.messaging.kafka.tracing.TraceContext;
import ch.admin.bit.jeap.messaging.kafka.tracing.TraceContextProvider;
import ch.admin.bit.jeap.messaging.model.*;
import ch.admin.bit.jeap.processcontext.domain.StubMetricsListener;
import ch.admin.bit.jeap.processcontext.domain.port.MessageConsumerFactory;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceQueryRepository;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.*;
import ch.admin.bit.jeap.processcontext.domain.processupdate.ProcessUpdateService;
import ch.admin.bit.jeap.processcontext.plugin.api.event.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ExtendWith(MockitoExtension.class)
class DomainMessageServiceTest {

    private static final String EVENT_ID = "eventId";
    private static final String EVENT_NAME = "eventName";
    private static final String TOPIC_NAME = "topicName";
    private static final String IDEMPOTENCE_ID = "idempotenceId";

    private static final MessageProcessIdCorrelationProvider CORRELATION_PROVIDER_FIRST_TEMPLATE = new MessageProcessIdCorrelationProvider() {
        @Override
        public Set<String> getRelatedOriginTaskIds(ch.admin.bit.jeap.messaging.model.Message message) {
            return Set.of("taskId1-first");
        }

        @Override
        public Set<String> getOriginProcessIds(ch.admin.bit.jeap.messaging.model.Message message) {
            return Set.of("originProcessId1-first");
        }
    };

    private static final MessageCorrelationProvider<ch.admin.bit.jeap.messaging.model.Message> CORRELATION_PROVIDER_SECOND_TEMPLATE = new MessageCorrelationProvider<>() {
        @Override
        public Set<String> getRelatedOriginTaskIds(ch.admin.bit.jeap.messaging.model.Message message) {
            return Set.of("taskId2-second", "taskId3-second");
        }

        @Override
        public Set<String> getOriginProcessIds(ch.admin.bit.jeap.messaging.model.Message message) {
            return Set.of("originProcessId2-second", "originProcessId3-second");
        }
    };

    private static final PayloadExtractor<MessagePayload> PAYLOAD_EXTRACTOR_FIRST_TEMPLATE = new PayloadExtractor<>() {
        @Override
        public Set<ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData> getMessageData(MessagePayload payload) {
            return Set.of(new ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData("pkey1-first", "pvalue1-first"));
        }
    };

    private static final PayloadExtractor<MessagePayload> PAYLOAD_EXTRACTOR_SECOND_TEMPLATE = new PayloadExtractor<>() {
        @Override
        public Set<ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData> getMessageData(MessagePayload payload) {
            return Set.of(new ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData("pkey2-second", "pvalue2-second"),
                    new ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData("pkey3-second", "pvalue3-second", "prole3-second"));
        }
    };

    private static final PayloadExtractor<MessagePayload> PAYLOAD_EXTRACTOR_FOR_PROCESS_DATA_CORRELATION_TEMPLATE = new PayloadExtractor<>() {
        @Override
        public Set<ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData> getMessageData(MessagePayload payload) {
            return Set.of(new ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData("process-data-event-key", "process-data-event-value", "process-data-event-role"),
                    new ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData("other-key", "other-value", "other-role"));
        }
    };

    private static final PayloadExtractor<MessagePayload> PAYLOAD_EXTRACTOR_FOR_PROCESS_DATA_CORRELATION_WITHOUT_ROLE_TEMPLATE = new PayloadExtractor<>() {
        @Override
        public Set<ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData> getMessageData(MessagePayload payload) {
            return Set.of(new ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData("process-data-without-role-event-key", "process-data-without-role-event-value", null));
        }
    };


    private static final ReferenceExtractor<MessageReferences> REFERENCE_EXTRACTOR_FIRST_TEMPLATE = new ReferenceExtractor<>() {
        @Override
        public Set<ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData> getMessageData(MessageReferences references) {
            return Set.of(new ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData("rkey1-first", "rvalue1-first"));
        }
    };

    private static final ReferenceExtractor<MessageReferences> REFERENCE_EXTRACTOR_SECOND_TEMPLATE = new ReferenceExtractor<>() {
        @Override
        public Set<ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData> getMessageData(MessageReferences references) {
            return Set.of(new ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData("rkey2-second", "rvalue2-second"),
                    new ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData("rkey3-second", "rvalue3-second", "rrole3-second"));
        }
    };

    @Mock
    private ProcessTemplateRepository processTemplateRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private MessageConsumerFactory messageConsumerFactory;
    @Mock
    private ProcessUpdateService processUpdateService;
    @Mock
    private ProcessInstanceQueryRepository processInstanceQueryRepository;
    @Mock
    private ch.admin.bit.jeap.messaging.model.Message message;
    @Mock
    private MessageIdentity messageIdentity;
    @Mock
    private MessageType messageType;
    @Mock
    private MessageReferences messageReferences;
    @Mock
    private MessagePayload messagePayload;
    @Mock
    private TraceContextProvider traceContextProvider;
    @Captor
    private ArgumentCaptor<Message> eventCaptor;

    private MessageService domainEventService;

    @Test
    void startDomainEventListeners() {
        List<ProcessTemplate> templates = List.of(
                createFirstProcessTemplateWithEventReference("event1", "topic1", null),
                createFirstProcessTemplateWithEventReference("event2", "topic2", null));
        doReturn(templates).when(processTemplateRepository).getAllTemplates();

        domainEventService.startDomainEventListeners();

        verify(messageConsumerFactory).startConsumer("topic1", "event1", null, domainEventService);
        verify(messageConsumerFactory).startConsumer("topic2", "event2", null, domainEventService);
    }

    @Test
    void startDomainEventListeners_clusterDefined() {
        List<ProcessTemplate> templates = List.of(
                createFirstProcessTemplateWithEventReference("event1", "topic1", "aws"),
                createFirstProcessTemplateWithEventReference("event2", "topic2", "aws"));
        doReturn(templates).when(processTemplateRepository).getAllTemplates();

        domainEventService.startDomainEventListeners();

        verify(messageConsumerFactory).startConsumer("topic1", "event1", "aws", domainEventService);
        verify(messageConsumerFactory).startConsumer("topic2", "event2", "aws", domainEventService);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void domainEventReceived() {
        final ProcessTemplate processTemplate1 = createFirstProcessTemplateWithEventReference(EVENT_NAME, TOPIC_NAME, null);
        final ProcessTemplate processTemplate2 = createSecondProcessTemplateWithEventReference(EVENT_NAME, TOPIC_NAME);
        final ProcessTemplate processTemplateProcessDataCorrelation = createProcessDataCorrelationTemplate(EVENT_NAME, TOPIC_NAME);
        final ProcessTemplate processTemplateProcessDataCorrelationWithoutRole = createProcessDataCorrelationWithoutRoleTemplate(EVENT_NAME, TOPIC_NAME);
        final String template1Name = processTemplate1.getName();
        final String template2Name = processTemplate2.getName();
        final String templateProcessDataCorrelationName = processTemplateProcessDataCorrelation.getName();
        final String templateProcessDataCorrelationWithoutRoleName = processTemplateProcessDataCorrelationWithoutRole.getName();
        final ZonedDateTime timestamp = ZonedDateTime.now();
        doReturn(messageIdentity).when(message).getIdentity();
        doReturn(messageType).when(message).getType();
        doReturn(Optional.of(messagePayload)).when(message).getOptionalPayload();
        doReturn(Optional.of(messageReferences)).when(message).getOptionalReferences();
        doReturn(EVENT_NAME).when(messageType).getName();
        doReturn(EVENT_ID).when(messageIdentity).getId();
        doReturn(IDEMPOTENCE_ID).when(messageIdentity).getIdempotenceId();
        when(messageIdentity.getCreatedZoned()).thenReturn(timestamp);
        doReturn(Map.of(template1Name, processTemplate1.getMessageReferences().stream().filter(r -> r.getMessageName().equals(EVENT_NAME)).findFirst().get(),
                template2Name, processTemplate2.getMessageReferences().stream().filter(r -> r.getMessageName().equals(EVENT_NAME)).findFirst().get(),
                templateProcessDataCorrelationName, processTemplateProcessDataCorrelation.getMessageReferences().stream().filter(r -> r.getMessageName().equals(EVENT_NAME)).findFirst().get(),
                templateProcessDataCorrelationWithoutRoleName, processTemplateProcessDataCorrelationWithoutRole.getMessageReferences().stream().filter(r -> r.getMessageName().equals(EVENT_NAME)).findFirst().get()))
                .when(processTemplateRepository).getMessageReferencesByTemplateNameForMessageName(EVENT_NAME);
        doReturn(Optional.empty()).when(messageRepository).findByMessageNameAndIdempotenceId(EVENT_NAME, IDEMPOTENCE_ID);
        doReturn(Set.of("originProcessId-ProcessDataCorrelation")).when(processInstanceQueryRepository).findUncompletedProcessInstancesHavingProcessData(templateProcessDataCorrelationName, "process-data-key", "process-data-event-value", "process-data-event-role");
        doReturn(Set.of("originProcessId-ProcessDataCorrelationWithoutRole")).when(processInstanceQueryRepository).findUncompletedProcessInstancesHavingProcessData(templateProcessDataCorrelationWithoutRoleName, "process-data-without-role-key", "process-data-without-role-event-value", null);
        TraceContext traceContext = new TraceContext(1L, 1L, 1L, 1L, "66016cec9b6734e17b88d21c0466c6e7");
        doReturn(traceContext).when(traceContextProvider).getTraceContext();

        doReturn(mock(Message.class)).when(messageRepository).save(any());
        domainEventService.messageReceived(message);

        verify(messageRepository, times(1)).save(eventCaptor.capture());
        verifyNoMoreInteractions(messageRepository);
        verify(processInstanceQueryRepository, times(1)).findUncompletedProcessInstancesHavingProcessData(templateProcessDataCorrelationName, "process-data-key", "process-data-event-value", "process-data-event-role");
        verify(processInstanceQueryRepository, times(1)).findUncompletedProcessInstancesHavingProcessData(templateProcessDataCorrelationWithoutRoleName, "process-data-without-role-key", "process-data-without-role-event-value", null);
        verifyNoMoreInteractions(processInstanceQueryRepository);
        verify(processUpdateService, times(1)).messageReceived(eq("originProcessId1-first"), eventCaptor.capture());
        verify(processUpdateService, times(1)).messageReceived(eq("originProcessId2-second"), eventCaptor.capture());
        verify(processUpdateService, times(1)).messageReceived(eq("originProcessId3-second"), eventCaptor.capture());
        verify(processUpdateService, times(1)).messageReceived(eq("originProcessId-ProcessDataCorrelation"), eventCaptor.capture());
        verify(processUpdateService, times(1)).messageReceived(eq("originProcessId-ProcessDataCorrelationWithoutRole"), eventCaptor.capture());
        verifyNoMoreInteractions(processUpdateService);
        List<Message> capturedMessages = eventCaptor.getAllValues();

        Message messageSaved = capturedMessages.getFirst();
        assertThat(messageSaved.getId()).isNotNull();
        assertThat(messageSaved.getMessageId()).isEqualTo(EVENT_ID);
        assertThat(messageSaved.getMessageName()).isEqualTo(EVENT_NAME);
        assertThat(messageSaved.getIdempotenceId()).isEqualTo(IDEMPOTENCE_ID);
        assertThat(messageSaved.getMessageCreatedAt()).isEqualTo(timestamp);
        assertThat(messageSaved.getOriginTaskIds()).containsOnly(
                OriginTaskId.from(template1Name, "taskId1-first"),
                OriginTaskId.from(template2Name, "taskId2-second"),
                OriginTaskId.from(template2Name, "taskId3-second"));
        assertThat(messageSaved.getOriginTaskIds(template1Name))
                .containsOnly(OriginTaskId.from(template1Name, "taskId1-first"));
        assertThat(messageSaved.getOriginTaskIds(template2Name))
                .containsOnly(OriginTaskId.from(template2Name, "taskId2-second"), OriginTaskId.from(template2Name, "taskId3-second"));
        assertThat(messageSaved.getMessageData()).containsOnly(
                new MessageData(template1Name, "pkey1-first", "pvalue1-first"),
                new MessageData(template1Name, "rkey1-first", "rvalue1-first"),
                new MessageData(template2Name, "pkey2-second", "pvalue2-second"),
                new MessageData(template2Name, "pkey3-second", "pvalue3-second", "prole3-second"),
                new MessageData(template2Name, "rkey2-second", "rvalue2-second"),
                new MessageData(template2Name, "rkey3-second", "rvalue3-second", "rrole3-second"),
                new MessageData(templateProcessDataCorrelationName, "process-data-event-key", "process-data-event-value", "process-data-event-role"),
                new MessageData(templateProcessDataCorrelationName, "other-key", "other-value", "other-role"),
                new MessageData(templateProcessDataCorrelationWithoutRoleName, "process-data-without-role-event-key", "process-data-without-role-event-value", null));
        assertThat(messageSaved.getMessageData(template1Name)).containsOnly(
                new MessageData(template1Name, "pkey1-first", "pvalue1-first"),
                new MessageData(template1Name, "rkey1-first", "rvalue1-first"));
        assertThat(messageSaved.getMessageData(template2Name)).containsOnly(
                new MessageData(template2Name, "pkey2-second", "pvalue2-second"),
                new MessageData(template2Name, "pkey3-second", "pvalue3-second", "prole3-second"),
                new MessageData(template2Name, "rkey2-second", "rvalue2-second"),
                new MessageData(template2Name, "rkey3-second", "rvalue3-second", "rrole3-second"));
        assertThat(messageSaved.getMessageData(templateProcessDataCorrelationName)).containsOnly(
                new MessageData(templateProcessDataCorrelationName, "process-data-event-key", "process-data-event-value", "process-data-event-role"),
                new MessageData(templateProcessDataCorrelationName, "other-key", "other-value", "other-role"));
        assertThat(messageSaved.getMessageData(templateProcessDataCorrelationWithoutRoleName)).containsOnly(
                new MessageData(templateProcessDataCorrelationWithoutRoleName, "process-data-without-role-event-key", "process-data-without-role-event-value", null));
        assertThat(messageSaved.getTraceId()).isEqualTo("66016cec9b6734e17b88d21c0466c6e7");
    }

    @Test
    void domainEventReceivedIdempotenceCheck() {
        final ProcessTemplate processTemplate = createFirstProcessTemplateWithEventReference(EVENT_NAME, TOPIC_NAME, null);
        final String templateName = processTemplate.getName();
        doReturn(messageIdentity).when(message).getIdentity();
        doReturn(messageType).when(message).getType();
        doReturn(Optional.of(messagePayload)).when(message).getOptionalPayload();
        doReturn(Optional.of(messageReferences)).when(message).getOptionalReferences();
        doReturn(EVENT_NAME).when(messageType).getName();
        doReturn(IDEMPOTENCE_ID).when(messageIdentity).getIdempotenceId();
        doReturn(Map.of(templateName, processTemplate.getMessageReferences().stream().filter(r -> r.getMessageName().equals(EVENT_NAME)).findFirst().get()))
                .when(processTemplateRepository).getMessageReferencesByTemplateNameForMessageName(EVENT_NAME);
        Message existingMessage = Mockito.mock(Message.class);
        doReturn(Optional.of(existingMessage)).when(messageRepository).findByMessageNameAndIdempotenceId(EVENT_NAME, IDEMPOTENCE_ID);

        domainEventService.messageReceived(message);

        verify(messageRepository, never()).save(eventCaptor.capture());
        verify(processUpdateService, times(1)).messageReceived(eq("originProcessId1-first"), eventCaptor.capture());
        verifyNoMoreInteractions(processUpdateService);
        assertThat(eventCaptor.getValue()).isEqualTo(existingMessage);
    }

    @Test
    void domainEventReceived_withFullUserData() {
        doReturn(messageIdentity).when(message).getIdentity();
        doReturn(messageType).when(message).getType();
        MessageUser messageUser = AvroMessageUser.newBuilder()
                .setFamilyName("myFamilyName")
                .setBusinessPartnerId("myBusinessPartnerId")
                .setId("myId")
                .setGivenName("myGivenName")
                .setBusinessPartnerName("myBusinessPartnerName")
                .setPropertiesMap(Map.of("custom", "myCustomProperty"))
                .build();
        doReturn(Optional.of(messageUser)).when(message).getOptionalUser();
        doReturn(EVENT_NAME).when(messageType).getName();
        doReturn(EVENT_ID).when(messageIdentity).getId();
        doReturn(IDEMPOTENCE_ID).when(messageIdentity).getIdempotenceId();

        doReturn(Optional.empty()).when(messageRepository).findByMessageNameAndIdempotenceId(EVENT_NAME, IDEMPOTENCE_ID);
        TraceContext traceContext = new TraceContext(1L, 1L, 1L, 1L, "66016cec9b6734e17b88d21c0466c6e7");
        doReturn(traceContext).when(traceContextProvider).getTraceContext();

        doReturn(mock(Message.class)).when(messageRepository).save(any());
        domainEventService.messageReceived(message);

        verify(messageRepository, times(1)).save(eventCaptor.capture());
        verifyNoMoreInteractions(messageRepository);
        List<Message> capturedMessages = eventCaptor.getAllValues();

        Message messageSaved = capturedMessages.getFirst();
        Set<MessageUserData> userData = messageSaved.getUserData();
        assertThat(userData)
                .hasSize(6)
                .containsExactlyInAnyOrder(
                        new MessageUserData(MessageUserData.KEY_ID, "myId"),
                        new MessageUserData(MessageUserData.KEY_FAMILY_NAME, "myFamilyName"),
                        new MessageUserData(MessageUserData.KEY_BUSINESS_PARTNER_ID, "myBusinessPartnerId"),
                        new MessageUserData(MessageUserData.KEY_GIVEN_NAME, "myGivenName"),
                        new MessageUserData(MessageUserData.KEY_BUSINESS_PARTNER_NAME, "myBusinessPartnerName"),
                        new MessageUserData("custom", "myCustomProperty")
                );
    }

    @Test
    void domainEventReceived_withMinimalUserData() {
        doReturn(messageIdentity).when(message).getIdentity();
        doReturn(messageType).when(message).getType();
        MessageUser messageUser = AvroMessageUser.newBuilder()
                .setFamilyName("myFamilyName")
                .setBusinessPartnerId("myBusinessPartnerId")
                .setId("myId")
                .build();
        doReturn(Optional.of(messageUser)).when(message).getOptionalUser();
        doReturn(EVENT_NAME).when(messageType).getName();
        doReturn(EVENT_ID).when(messageIdentity).getId();
        doReturn(IDEMPOTENCE_ID).when(messageIdentity).getIdempotenceId();

        doReturn(Optional.empty()).when(messageRepository).findByMessageNameAndIdempotenceId(EVENT_NAME, IDEMPOTENCE_ID);
        TraceContext traceContext = new TraceContext(1L, 1L, 1L, 1L, "66016cec9b6734e17b88d21c0466c6e7");
        doReturn(traceContext).when(traceContextProvider).getTraceContext();

        doReturn(mock(Message.class)).when(messageRepository).save(any());
        domainEventService.messageReceived(message);

        verify(messageRepository, times(1)).save(eventCaptor.capture());
        verifyNoMoreInteractions(messageRepository);
        List<Message> capturedMessages = eventCaptor.getAllValues();

        Message messageSaved = capturedMessages.getFirst();
        Set<MessageUserData> userData = messageSaved.getUserData();
        assertThat(userData)
                .hasSize(3)
                .containsExactlyInAnyOrder(
                        new MessageUserData(MessageUserData.KEY_ID, "myId"),
                        new MessageUserData(MessageUserData.KEY_FAMILY_NAME, "myFamilyName"),
                        new MessageUserData(MessageUserData.KEY_BUSINESS_PARTNER_ID, "myBusinessPartnerId")
                );
    }

    @BeforeEach
    void setUp() {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        domainEventService = new MessageService(processTemplateRepository,
                messageConsumerFactory,
                messageRepository,
                processUpdateService,
                processInstanceQueryRepository,
                transactionManager,
                new StubMetricsListener(),
                traceContextProvider);
    }

    private static ProcessTemplate createFirstProcessTemplateWithEventReference(String eventName, String topicName, String clusterName) {
        TaskType taskType = TaskType.builder()
                .name("task").cardinality(TaskCardinality.SINGLE_INSTANCE).lifecycle(TaskLifecycle.STATIC)
                .build();
        return ProcessTemplate.builder()
                .name("first").taskTypes(List.of(taskType))
                .templateHash("hash")
                .messageReferences(List.of(MessageReference.builder()
                        .messageName(eventName)
                        .topicName(topicName)
                        .clusterName(clusterName)
                        .correlationProvider(CORRELATION_PROVIDER_FIRST_TEMPLATE)
                        .payloadExtractor(PAYLOAD_EXTRACTOR_FIRST_TEMPLATE)
                        .referenceExtractor(REFERENCE_EXTRACTOR_FIRST_TEMPLATE)
                        .processInstantiationCondition(new NeverProcessInstantiationCondition())
                        .build()))
                .build();
    }

    private static ProcessTemplate createSecondProcessTemplateWithEventReference(String eventName, String topicName) {
        TaskType taskType = TaskType.builder()
                .name("task").cardinality(TaskCardinality.SINGLE_INSTANCE).lifecycle(TaskLifecycle.STATIC)
                .build();
        return ProcessTemplate.builder()
                .name("second").taskTypes(List.of(taskType))
                .templateHash("hash")
                .messageReferences(List.of(MessageReference.builder()
                        .messageName(eventName)
                        .topicName(topicName)
                        .correlationProvider(CORRELATION_PROVIDER_SECOND_TEMPLATE)
                        .payloadExtractor(PAYLOAD_EXTRACTOR_SECOND_TEMPLATE)
                        .referenceExtractor(REFERENCE_EXTRACTOR_SECOND_TEMPLATE)
                        .processInstantiationCondition(new NeverProcessInstantiationCondition())
                        .build()))
                .build();
    }

    private static ProcessTemplate createProcessDataCorrelationTemplate(String eventName, String topicName) {
        TaskType taskType = TaskType.builder()
                .name("task").cardinality(TaskCardinality.SINGLE_INSTANCE).lifecycle(TaskLifecycle.STATIC)
                .build();
        return ProcessTemplate.builder()
                .name("processDataCorrelation").taskTypes(List.of(taskType))
                .templateHash("hash")
                .messageReferences(List.of(MessageReference.builder()
                        .messageName(eventName)
                        .topicName(topicName)
                        .payloadExtractor(PAYLOAD_EXTRACTOR_FOR_PROCESS_DATA_CORRELATION_TEMPLATE)
                        .referenceExtractor(new EmptySetReferenceExtractor())
                        .correlationProvider(new MessageProcessIdCorrelationProvider())
                        .processInstantiationCondition(new NeverProcessInstantiationCondition())
                        .correlatedByProcessData(CorrelatedByProcessData.builder()
                                .processDataKey("process-data-key")
                                .messageDataKey("process-data-event-key")
                                .build())
                        .build()))
                .build();
    }

    private static ProcessTemplate createProcessDataCorrelationWithoutRoleTemplate(String eventName, String topicName) {
        TaskType taskType = TaskType.builder()
                .name("task").cardinality(TaskCardinality.SINGLE_INSTANCE).lifecycle(TaskLifecycle.STATIC)
                .build();
        return ProcessTemplate.builder()
                .name("processDataCorrelationWithoutRole").taskTypes(List.of(taskType))
                .templateHash("hash")
                .messageReferences(List.of(MessageReference.builder()
                        .messageName(eventName)
                        .topicName(topicName)
                        .payloadExtractor(PAYLOAD_EXTRACTOR_FOR_PROCESS_DATA_CORRELATION_WITHOUT_ROLE_TEMPLATE)
                        .referenceExtractor(new EmptySetReferenceExtractor())
                        .correlationProvider(new MessageProcessIdCorrelationProvider())
                        .processInstantiationCondition(new NeverProcessInstantiationCondition())
                        .correlatedByProcessData(CorrelatedByProcessData.builder()
                                .processDataKey("process-data-without-role-key")
                                .messageDataKey("process-data-without-role-event-key")
                                .build())
                        .build()))
                .build();
    }

}
