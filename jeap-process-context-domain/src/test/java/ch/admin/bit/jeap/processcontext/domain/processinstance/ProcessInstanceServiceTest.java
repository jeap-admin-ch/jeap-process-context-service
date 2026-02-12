package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.message.MessageData;
import ch.admin.bit.jeap.processcontext.domain.message.MessageReferenceRepository;
import ch.admin.bit.jeap.processcontext.domain.message.MessageRepository;
import ch.admin.bit.jeap.processcontext.domain.port.MetricsListener;
import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextFactory;
import ch.admin.bit.jeap.processcontext.domain.processinstance.relation.RelationService;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.*;
import ch.admin.bit.jeap.processcontext.domain.tx.Transactions;
import ch.admin.bit.jeap.processcontext.plugin.api.message.EmptySetPayloadExtractor;
import ch.admin.bit.jeap.processcontext.plugin.api.message.EmptySetReferenceExtractor;
import ch.admin.bit.jeap.processcontext.plugin.api.message.MessageProcessIdCorrelationProvider;
import ch.admin.bit.jeap.processcontext.plugin.api.message.NeverProcessInstantiationCondition;
import com.fasterxml.uuid.Generators;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessInstanceServiceTest {

    private static final String ORIGIN_PROCESS_ID = "originProcessId";
    private static final String TEMPLATE_NAME = "templateName";
    private static final String DOMAIN_EVENT_NAME = "myDomainEvent";

    @Mock
    private ProcessInstanceRepository processInstanceRepository;
    @Mock
    private TaskInstanceRepository taskInstanceRepository;
    @Mock
    private PendingMessageRepository pendingMessageRepository;
    @Mock
    private TaskService taskService;
    @Mock
    private ProcessInstanceMigrationService processInstanceMigrationService;
    @Mock
    private ProcessDataService processDataService;
    @Mock
    private ProcessTemplateRepository processTemplateRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private MessageReferenceRepository messageReferenceRepository;
    @Mock
    private ProcessSnapshotService processSnapshotService;
    @Mock
    private RelationService relationService;
    @Mock
    private ProcessRelationRepository processRelationRepository;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private MetricsListener metricsListener;

    private ProcessInstanceService target;
    private ProcessContextFactory processContextFactory;

    @BeforeEach
    void setUp() {
        Transactions transactions = new Transactions(transactionManager);
        ProcessContextRepositoryFacadeStub processContextRepositoryFacadeStub = new ProcessContextRepositoryFacadeStub();
        processContextFactory = new ProcessContextFactory(processContextRepositoryFacadeStub);

        // Setup transaction manager to execute callbacks synchronously
        lenient().when(transactionManager.getTransaction(any())).thenAnswer(invocation -> new SimpleTransactionStatus());
        lenient().doNothing().when(transactionManager).commit(any());
        lenient().doNothing().when(transactionManager).rollback(any());

        // Setup metricsListener.timed to execute the callback
        lenient().doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(2)).run();
            return null;
        }).when(metricsListener).timed(anyString(), anyMap(), any(Runnable.class));

        // Common defaults for process update handling
        lenient().when(processInstanceMigrationService.applyTemplateMigrationIfChanged(any()))
                .thenReturn(Optional.empty());
        lenient().when(processDataService.copyMessageDataToProcessData(any(), any()))
                .thenReturn(List.of());
        lenient().when(taskService.planDomainEventTasks(any(), any(), any()))
                .thenReturn(List.of());
        lenient().when(messageReferenceRepository.save(any(MessageReference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        target = new ProcessInstanceService(
                processInstanceRepository,
                taskService,
                processInstanceMigrationService,
                processDataService,
                processTemplateRepository,
                messageRepository,
                messageReferenceRepository,
                processSnapshotService,
                relationService,
                processRelationRepository,
                transactions,
                metricsListener,
                new ProcessInstanceFactory(processInstanceRepository, processTemplateRepository, processContextFactory, metricsListener, taskInstanceRepository),
                pendingMessageRepository);
    }

    @Test
    void handleMessage_messageNotFound_throwsNotFoundException() {
        UUID unknownMessageId = UUID.randomUUID();
        when(messageRepository.findById(unknownMessageId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> target.handleMessage(ORIGIN_PROCESS_ID, unknownMessageId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void handleMessage_noProcessInstanceAndNoTemplateName_savesPendingMessage() {
        Message message = createMessage();

        when(messageRepository.findById(message.getId()))
                .thenReturn(Optional.of(message));
        when(processInstanceRepository.findByOriginProcessId(ORIGIN_PROCESS_ID))
                .thenReturn(Optional.empty());

        target.handleMessage(ORIGIN_PROCESS_ID, message.getId());

        ArgumentCaptor<PendingMessage> captor = ArgumentCaptor.forClass(PendingMessage.class);
        verify(pendingMessageRepository).saveIfNew(captor.capture());
        PendingMessage saved = captor.getValue();
        assertThat(saved.getOriginProcessId()).isEqualTo(ORIGIN_PROCESS_ID);
        assertThat(saved.getMessageId()).isEqualTo(message.getId());
    }

    @Test
    void updateProcessState_createProcess_createsNewProcessInstance() {
        ProcessTemplate processTemplate = createSimpleProcessTemplate();
        Message message = createMessage();

        when(messageRepository.findById(message.getId()))
                .thenReturn(Optional.of(message));
        when(processInstanceRepository.findByOriginProcessId(ORIGIN_PROCESS_ID))
                .thenReturn(Optional.empty());
        when(processTemplateRepository.findByName(TEMPLATE_NAME))
                .thenReturn(Optional.of(processTemplate));
        when(processInstanceRepository.save(any(ProcessInstance.class)))
                .thenAnswer(invocation -> {
                    ProcessInstance pi = invocation.getArgument(0);
                    // Simulate JPA behavior: clear transient fields and return the entity
                    clearTransientFields(pi);
                    return pi;
                });

        target.handleMessage(ORIGIN_PROCESS_ID, message.getId(), TEMPLATE_NAME);

        ArgumentCaptor<ProcessInstance> processInstanceCaptor = ArgumentCaptor.forClass(ProcessInstance.class);
        verify(processInstanceRepository, atLeastOnce()).save(processInstanceCaptor.capture());
        ProcessInstance savedInstance = processInstanceCaptor.getValue();
        assertThat(savedInstance.getOriginProcessId()).isEqualTo(ORIGIN_PROCESS_ID);
        assertThat(savedInstance.getProcessTemplateName()).isEqualTo(TEMPLATE_NAME);
    }

    @Test
    void updateProcessState_domainEvent_addsMessageReferenceToExistingProcess() {
        ProcessTemplate processTemplate = createSimpleProcessTemplate();
        Message message = createMessage();
        ProcessInstance processInstance = createProcessInstanceWithTemplate(processTemplate);

        when(messageRepository.findById(message.getId()))
                .thenReturn(Optional.of(message));
        when(processInstanceRepository.findByOriginProcessId(ORIGIN_PROCESS_ID))
                .thenReturn(Optional.of(processInstance));

        target.handleMessage(ORIGIN_PROCESS_ID, message.getId());

        verify(messageReferenceRepository).save(any(MessageReference.class));
    }

    @Test
    void handleMessage_domainEvent_delegatesTaskPlanningToTaskService() {
        ProcessTemplate processTemplate = createSimpleProcessTemplate();
        Message message = createMessage();
        ProcessInstance processInstance = createProcessInstanceWithTemplate(processTemplate);

        when(messageRepository.findById(message.getId()))
                .thenReturn(Optional.of(message));
        when(processInstanceRepository.findByOriginProcessId(ORIGIN_PROCESS_ID))
                .thenReturn(Optional.of(processInstance));

        target.handleMessage(ORIGIN_PROCESS_ID, message.getId());

        // Verify task orchestration is delegated to TaskService
        verify(taskService).planDomainEventTasks(eq(processInstance), any(MessageReferenceMessageDTO.class), eq(message));
        verify(taskService).completeObservationTasks(eq(processInstance), any(MessageReferenceMessageDTO.class), eq(message));
        verify(taskService).evaluateCompletedTasks(eq(processInstance), any(MessageReferenceMessageDTO.class));
    }

    @Test
    void updateProcessState_correlatesByProcessData_correlatesMessageDirectly() {
        ch.admin.bit.jeap.processcontext.domain.processtemplate.MessageReference correlatedMessageReference =
                ch.admin.bit.jeap.processcontext.domain.processtemplate.MessageReference.builder()
                        .messageName("correlatedEvent")
                        .topicName("topicName")
                        .correlationProvider(new MessageProcessIdCorrelationProvider())
                        .payloadExtractor(new EmptySetPayloadExtractor())
                        .referenceExtractor(new EmptySetReferenceExtractor())
                        .processInstantiationCondition(new NeverProcessInstantiationCondition())
                        .correlatedByProcessData(CorrelatedByProcessData.builder()
                                .processDataKey("myProcessDataKey")
                                .messageDataKey("myMessageDataKey")
                                .build())
                        .build();
        ProcessDataTemplate processDataTemplate = ProcessDataTemplate.builder()
                .key("myProcessDataKey")
                .sourceMessageName(DOMAIN_EVENT_NAME)
                .sourceMessageDataKey("myMessageDataKey")
                .build();
        TaskType staticTask = TaskType.builder()
                .name("staticTask")
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .lifecycle(TaskLifecycle.STATIC)
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name(TEMPLATE_NAME)
                .templateHash("hash")
                .taskTypes(List.of(staticTask))
                .messageReferences(List.of(correlatedMessageReference))
                .processDataTemplates(List.of(processDataTemplate))
                .processRelationPatterns(List.of())
                .build();

        Message message = createMessage();
        ProcessInstance processInstance = createProcessInstanceWithTemplate(processTemplate);
        ProcessData processData = new ProcessData("myProcessDataKey", "myValue");
        processData.setProcessInstance(processInstance);

        Message correlatedMessage = createMessageWithName("correlatedEvent");

        when(messageRepository.findById(message.getId()))
                .thenReturn(Optional.of(message));
        when(processInstanceRepository.findByOriginProcessId(ORIGIN_PROCESS_ID))
                .thenReturn(Optional.of(processInstance));
        when(processDataService.copyMessageDataToProcessData(processInstance, message))
                .thenReturn(List.of(processData));
        when(processTemplateRepository.isAnyTemplateHasEventsCorrelatedByProcessData())
                .thenReturn(true);
        when(processTemplateRepository.findByName(TEMPLATE_NAME))
                .thenReturn(Optional.of(processTemplate));
        when(messageReferenceRepository.findByProcessInstanceId(processInstance.getId()))
                .thenReturn(List.of());
        when(messageRepository.findMessagesToCorrelate("correlatedEvent", TEMPLATE_NAME, "myMessageDataKey", "myValue"))
                .thenReturn(List.of(correlatedMessage));

        target.handleMessage(ORIGIN_PROCESS_ID, message.getId());

        // Verify the correlated message was directly applied (message reference saved for both original and correlated message)
        verify(messageReferenceRepository, times(2)).save(any(MessageReference.class));
    }

    @Test
    void updateProcessState_noCorrelationByProcessDataInAnyTemplate_skipsCorrelation() {
        ProcessTemplate processTemplate = createSimpleProcessTemplate();
        Message message = createMessage();
        ProcessInstance processInstance = createProcessInstanceWithTemplate(processTemplate);
        ProcessData processData = new ProcessData("someKey", "someValue");
        processData.setProcessInstance(processInstance);

        when(messageRepository.findById(message.getId()))
                .thenReturn(Optional.of(message));
        when(processInstanceRepository.findByOriginProcessId(ORIGIN_PROCESS_ID))
                .thenReturn(Optional.of(processInstance));
        when(processDataService.copyMessageDataToProcessData(processInstance, message))
                .thenReturn(List.of(processData));
        when(processTemplateRepository.isAnyTemplateHasEventsCorrelatedByProcessData())
                .thenReturn(false);

        target.handleMessage(ORIGIN_PROCESS_ID, message.getId());

        // Verify that correlation queries were not made (early exit because no template has process data correlation)
        verify(messageReferenceRepository, never()).findByProcessInstanceId(any());
    }

    @Test
    void handleMessage_delegatesEvaluatePlannedTasksCompletedByExistingMessages() {
        ProcessTemplate processTemplate = createSimpleProcessTemplate();
        Message message = createMessage();
        ProcessInstance processInstance = createProcessInstanceWithTemplate(processTemplate);

        TaskInstance plannedTask = TaskInstance.createInitialTaskInstance(
                createStaticTaskType(), processInstance, ZonedDateTime.now());
        when(taskService.planDomainEventTasks(any(), any(), any()))
                .thenReturn(List.of(plannedTask));

        when(messageRepository.findById(message.getId()))
                .thenReturn(Optional.of(message));
        when(processInstanceRepository.findByOriginProcessId(ORIGIN_PROCESS_ID))
                .thenReturn(Optional.of(processInstance));

        target.handleMessage(ORIGIN_PROCESS_ID, message.getId());

        // Planned tasks should be evaluated for completion by existing messages
        verify(taskService).evaluatePlannedTasksCompletedByExistingMessages(List.of(plannedTask));
    }

    @Test
    void handleMessage_templateMigrationAddsNewTask_plansNewStaticTask() {
        // Create process with old template
        TaskType existingTask = TaskType.builder()
                .name("existingTask")
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .lifecycle(TaskLifecycle.STATIC)
                .build();
        ProcessTemplate oldTemplate = ProcessTemplate.builder()
                .name(TEMPLATE_NAME)
                .templateHash("oldHash")
                .taskTypes(List.of(existingTask))
                .processRelationPatterns(List.of())
                .build();

        // New template with additional task
        TaskType newStaticTask = TaskType.builder()
                .name("newTask")
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .lifecycle(TaskLifecycle.STATIC)
                .build();
        ProcessTemplate newTemplate = ProcessTemplate.builder()
                .name(TEMPLATE_NAME)
                .templateHash("newHash")
                .taskTypes(List.of(existingTask, newStaticTask))
                .processRelationPatterns(List.of())
                .build();

        Message message = createMessage();

        // Create process instance with old template
        ProcessInstance processInstance = createProcessInstanceWithTemplate(oldTemplate);

        // Simulate loading from DB with new template (template hash changed)
        clearTransientFields(processInstance);
        processInstance.onAfterLoadFromPersistentState(newTemplate, processContextFactory);

        when(messageRepository.findById(message.getId()))
                .thenReturn(Optional.of(message));
        when(processInstanceRepository.findByOriginProcessId(ORIGIN_PROCESS_ID))
                .thenReturn(Optional.of(processInstance));

        target.handleMessage(ORIGIN_PROCESS_ID, message.getId());

        // Migration service should have been called
        verify(processInstanceMigrationService).applyTemplateMigrationIfChanged(processInstance);
    }

    @Test
    void updateProcessState_matchingProcessRelationPattern_savesRelationViaRepository() {
        ProcessRelationPattern pattern = ProcessRelationPattern.builder()
                .name("myRelation")
                .roleType(ProcessRelationRoleType.ORIGIN)
                .originRole("originRole")
                .targetRole("targetRole")
                .visibility(ProcessRelationRoleVisibility.BOTH)
                .source(ProcessRelationSource.builder()
                        .messageName(DOMAIN_EVENT_NAME)
                        .messageDataKey("dataKey")
                        .build())
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name(TEMPLATE_NAME)
                .templateHash("hash")
                .taskTypes(List.of(createStaticTaskType()))
                .processRelationPatterns(List.of(pattern))
                .build();
        Message message = createMessage();
        ProcessInstance processInstance = createProcessInstanceWithTemplate(processTemplate);

        when(messageRepository.findById(message.getId()))
                .thenReturn(Optional.of(message));
        when(processInstanceRepository.findByOriginProcessId(ORIGIN_PROCESS_ID))
                .thenReturn(Optional.of(processInstance));

        target.handleMessage(ORIGIN_PROCESS_ID, message.getId());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProcessRelation>> captor = ArgumentCaptor.forClass(List.class);
        verify(processRelationRepository).saveAll(captor.capture());
        List<ProcessRelation> savedRelations = captor.getValue();
        assertThat(savedRelations).hasSize(1);
        assertThat(savedRelations.getFirst().getName()).isEqualTo("myRelation");
        assertThat(savedRelations.getFirst().getRelatedProcessId()).isEqualTo("dataValue");
        assertThat(savedRelations.getFirst().getProcessInstance()).isSameAs(processInstance);
    }

    @Test
    void updateProcessState_duplicateProcessRelation_doesNotSaveAgain() {
        ProcessRelationPattern pattern = ProcessRelationPattern.builder()
                .name("myRelation")
                .roleType(ProcessRelationRoleType.ORIGIN)
                .originRole("originRole")
                .targetRole("targetRole")
                .visibility(ProcessRelationRoleVisibility.BOTH)
                .source(ProcessRelationSource.builder()
                        .messageName(DOMAIN_EVENT_NAME)
                        .messageDataKey("dataKey")
                        .build())
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name(TEMPLATE_NAME)
                .templateHash("hash")
                .taskTypes(List.of(createStaticTaskType()))
                .processRelationPatterns(List.of(pattern))
                .build();
        Message message = createMessage();
        ProcessInstance processInstance = createProcessInstanceWithTemplate(processTemplate);

        when(messageRepository.findById(message.getId()))
                .thenReturn(Optional.of(message));
        when(processInstanceRepository.findByOriginProcessId(ORIGIN_PROCESS_ID))
                .thenReturn(Optional.of(processInstance));
        when(processRelationRepository.exists(eq(processInstance.getId()), any(ProcessRelation.class)))
                .thenReturn(true);

        target.handleMessage(ORIGIN_PROCESS_ID, message.getId());

        verify(processRelationRepository, never()).saveAll(any());
    }

    @Test
    void updateProcessState_noProcessRelationPatterns_doesNotSaveRelations() {
        ProcessTemplate processTemplate = createSimpleProcessTemplate(); // has empty processRelationPatterns
        Message message = createMessage();
        ProcessInstance processInstance = createProcessInstanceWithTemplate(processTemplate);

        when(messageRepository.findById(message.getId()))
                .thenReturn(Optional.of(message));
        when(processInstanceRepository.findByOriginProcessId(ORIGIN_PROCESS_ID))
                .thenReturn(Optional.of(processInstance));

        target.handleMessage(ORIGIN_PROCESS_ID, message.getId());

        verify(processRelationRepository, never()).saveAll(any());
    }

    @Test
    void updateProcessState_processRelationPatternNonMatchingMessage_doesNotSaveRelations() {
        ProcessRelationPattern pattern = ProcessRelationPattern.builder()
                .name("myRelation")
                .roleType(ProcessRelationRoleType.ORIGIN)
                .originRole("originRole")
                .targetRole("targetRole")
                .visibility(ProcessRelationRoleVisibility.BOTH)
                .source(ProcessRelationSource.builder()
                        .messageName("otherEvent")
                        .messageDataKey("dataKey")
                        .build())
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name(TEMPLATE_NAME)
                .templateHash("hash")
                .taskTypes(List.of(createStaticTaskType()))
                .processRelationPatterns(List.of(pattern))
                .build();
        Message message = createMessage();
        ProcessInstance processInstance = createProcessInstanceWithTemplate(processTemplate);

        when(messageRepository.findById(message.getId()))
                .thenReturn(Optional.of(message));
        when(processInstanceRepository.findByOriginProcessId(ORIGIN_PROCESS_ID))
                .thenReturn(Optional.of(processInstance));

        target.handleMessage(ORIGIN_PROCESS_ID, message.getId());

        verify(processRelationRepository, never()).saveAll(any());
    }

    @Test
    void updateProcessState_processRelationPatternNonMatchingDataKey_doesNotSaveRelations() {
        ProcessRelationPattern pattern = ProcessRelationPattern.builder()
                .name("myRelation")
                .roleType(ProcessRelationRoleType.ORIGIN)
                .originRole("originRole")
                .targetRole("targetRole")
                .visibility(ProcessRelationRoleVisibility.BOTH)
                .source(ProcessRelationSource.builder()
                        .messageName(DOMAIN_EVENT_NAME)
                        .messageDataKey("nonExistentKey")
                        .build())
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name(TEMPLATE_NAME)
                .templateHash("hash")
                .taskTypes(List.of(createStaticTaskType()))
                .processRelationPatterns(List.of(pattern))
                .build();
        Message message = createMessage();
        ProcessInstance processInstance = createProcessInstanceWithTemplate(processTemplate);

        when(messageRepository.findById(message.getId()))
                .thenReturn(Optional.of(message));
        when(processInstanceRepository.findByOriginProcessId(ORIGIN_PROCESS_ID))
                .thenReturn(Optional.of(processInstance));

        target.handleMessage(ORIGIN_PROCESS_ID, message.getId());

        verify(processRelationRepository, never()).saveAll(any());
    }

    // Helper methods to create test data

    private static TaskType createStaticTaskType() {
        return TaskType.builder()
                .name("staticTask")
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .lifecycle(TaskLifecycle.STATIC)
                .build();
    }

    private ProcessTemplate createSimpleProcessTemplate() {
        TaskType staticTask = createStaticTaskType();
        return ProcessTemplate.builder()
                .name(TEMPLATE_NAME)
                .templateHash("hash")
                .taskTypes(List.of(staticTask))
                .processRelationPatterns(List.of())
                .build();
    }

    /**
     * Creates a ProcessInstance with the processTemplate set (as if it had been loaded from DB and restored).
     * The processContextFactory from the service setup is used.
     */
    private ProcessInstance createProcessInstanceWithTemplate(ProcessTemplate processTemplate) {
        return ProcessInstance.createProcessInstance(ORIGIN_PROCESS_ID, processTemplate, processContextFactory);
    }

    private Message createMessage() {
        return createMessageWithName(DOMAIN_EVENT_NAME);
    }

    private Message createMessageWithName(String messageName) {
        return Message.messageBuilder()
                .messageId(Generators.timeBasedEpochGenerator().generate().toString())
                .idempotenceId(Generators.timeBasedEpochGenerator().generate().toString())
                .messageName(messageName)
                .messageCreatedAt(ZonedDateTime.now())
                .createdAt(ZonedDateTime.now())
                .messageData(Set.of(MessageData.builder()
                        .key("dataKey")
                        .value("dataValue")
                        .templateName(TEMPLATE_NAME)
                        .build()))
                .build();
    }

    // Clears the transient fields to simulate JPA behavior after persistence.
    private void clearTransientFields(ProcessInstance processInstance) {
        ReflectionTestUtils.setField(processInstance, "processTemplate", null);
    }
}
