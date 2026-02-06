package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.PcsConfigProperties;
import ch.admin.bit.jeap.processcontext.domain.StubMetricsListener;
import ch.admin.bit.jeap.processcontext.domain.message.*;
import ch.admin.bit.jeap.processcontext.domain.port.InternalMessageProducer;
import ch.admin.bit.jeap.processcontext.domain.port.MetricsListener;
import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextFactory;
import ch.admin.bit.jeap.processcontext.domain.processinstance.relation.RelationService;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.*;
import ch.admin.bit.jeap.processcontext.domain.processupdate.ProcessUpdate;
import ch.admin.bit.jeap.processcontext.domain.processupdate.ProcessUpdateQueryRepository;
import ch.admin.bit.jeap.processcontext.domain.processupdate.ProcessUpdateRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessInstanceServiceTest {

    private static final String ORIGIN_PROCESS_ID = "originProcessId";
    private static final String TEMPLATE_NAME = "templateName";
    private static final String DOMAIN_EVENT_NAME = "myDomainEvent";

    @Mock
    private ProcessInstanceRepository processInstanceRepository;
    @Mock
    private TaskService taskService;
    @Mock
    private ProcessDataService processDataService;
    @Mock
    private ProcessTemplateRepository processTemplateRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private MessageReferenceRepository messageReferenceRepository;
    @Mock
    private InternalMessageProducer internalMessageProducer;
    @Mock
    private ProcessUpdateQueryRepository processUpdateQueryRepository;
    @Mock
    private ProcessUpdateRepository processUpdateRepository;
    @Mock
    private ProcessSnapshotService processSnapshotService;
    @Mock
    private RelationService relationService;
    @Mock
    private PlatformTransactionManager transactionManager;

    private ProcessInstanceService target;
    private ProcessContextFactory processContextFactory;
    private ProcessContextRepositoryFacadeStub processContextRepositoryFacadeStub;

    @BeforeEach
    void setUp() {
        PcsConfigProperties pcsConfigProperties = new PcsConfigProperties();
        Transactions transactions = new Transactions(transactionManager);
        MetricsListener metricsListener = new StubMetricsListener();
        processContextRepositoryFacadeStub = new ProcessContextRepositoryFacadeStub();
        processContextFactory = new ProcessContextFactory(processContextRepositoryFacadeStub);

        // Setup transaction manager to execute callbacks synchronously
        lenient().when(transactionManager.getTransaction(any())).thenAnswer(invocation -> new SimpleTransactionStatus());
        lenient().doNothing().when(transactionManager).commit(any());
        lenient().doNothing().when(transactionManager).rollback(any());

        target = new ProcessInstanceService(
                internalMessageProducer,
                processUpdateQueryRepository,
                processInstanceRepository,
                taskService,
                processDataService,
                processTemplateRepository,
                messageRepository,
                messageReferenceRepository,
                processUpdateRepository,
                processSnapshotService,
                relationService,
                processContextFactory,
                transactions,
                metricsListener,
                pcsConfigProperties
        );
    }

    @Test
    void updateProcessState_noUpdatesToProcess_doesNothing() {
        when(processUpdateQueryRepository.findByOriginProcessIdAndHandledFalse(ORIGIN_PROCESS_ID))
                .thenReturn(List.of());

        target.updateProcessState(ORIGIN_PROCESS_ID);

        verify(processInstanceRepository, never()).save(any());
    }

    @Test
    void updateProcessState_createProcess_createsNewProcessInstance() {
        ProcessTemplate processTemplate = createSimpleProcessTemplate();
        Message message = createMessage();
        ProcessUpdate processUpdate = createProcessUpdate(message);

        when(processUpdateQueryRepository.findByOriginProcessIdAndHandledFalse(ORIGIN_PROCESS_ID))
                .thenReturn(List.of(processUpdate));
        when(processTemplateRepository.findByName(TEMPLATE_NAME))
                .thenReturn(Optional.of(processTemplate));
        when(messageRepository.findById(message.getId()))
                .thenReturn(Optional.of(message));
        when(processInstanceRepository.findByOriginProcessId(ORIGIN_PROCESS_ID))
                .thenReturn(Optional.empty());
        when(processInstanceRepository.save(any(ProcessInstance.class)))
                .thenAnswer(invocation -> {
                    ProcessInstance pi = invocation.getArgument(0);
                    // Simulate JPA behavior: clear transient fields and return the entity
                    clearTransientFields(pi);
                    processContextRepositoryFacadeStub.setProcessInstance(pi);
                    return pi;
                });
        when(messageReferenceRepository.save(any(MessageReference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        target.updateProcessState(ORIGIN_PROCESS_ID);

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
        ProcessUpdate processUpdate = createDomainEventUpdate(message);
        ProcessInstance processInstance = createProcessInstanceWithTemplate(processTemplate);

        when(processUpdateQueryRepository.findByOriginProcessIdAndHandledFalse(ORIGIN_PROCESS_ID))
                .thenReturn(List.of(processUpdate));
        when(processInstanceRepository.findByOriginProcessId(ORIGIN_PROCESS_ID))
                .thenReturn(Optional.of(processInstance));
        when(messageRepository.findById(message.getId()))
                .thenReturn(Optional.of(message));
        when(messageReferenceRepository.save(any(MessageReference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        target.updateProcessState(ORIGIN_PROCESS_ID);

        verify(messageReferenceRepository).save(any(MessageReference.class));
        verify(processUpdateRepository).markHandled(processUpdate.getId());
    }

    @Test
    void updateProcessState_domainEventPlansDynamicTask_plansSingleInstanceTask() {
        TaskType dynamicTaskType = TaskType.builder()
                .name("dynamicTask")
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .lifecycle(TaskLifecycle.DYNAMIC)
                .plannedByDomainEvent(DOMAIN_EVENT_NAME)
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name(TEMPLATE_NAME)
                .templateHash("hash")
                .taskTypes(List.of(dynamicTaskType))
                .processRelationPatterns(List.of())
                .build();
        Message message = createMessage();
        ProcessUpdate processUpdate = createDomainEventUpdate(message);
        ProcessInstance processInstance = createProcessInstanceWithTemplate(processTemplate);

        when(processUpdateQueryRepository.findByOriginProcessIdAndHandledFalse(ORIGIN_PROCESS_ID))
                .thenReturn(List.of(processUpdate));
        when(processInstanceRepository.findByOriginProcessId(ORIGIN_PROCESS_ID))
                .thenReturn(Optional.of(processInstance));
        when(messageRepository.findById(message.getId()))
                .thenReturn(Optional.of(message));
        when(messageReferenceRepository.save(any(MessageReference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        target.updateProcessState(ORIGIN_PROCESS_ID);

        // Verify a task was planned
        assertThat(processInstance.getTasks()).hasSize(1);
        assertThat(processInstance.getTasks().getFirst().getTaskTypeName()).isEqualTo("dynamicTask");
        assertThat(processInstance.getTasks().getFirst().getState()).isEqualTo(TaskState.PLANNED);
    }

    @Test
    void updateProcessState_domainEventPlansMultiInstanceTask_plansTasksForEachOriginTaskId() {
        TaskType multiInstanceTaskType = TaskType.builder()
                .name("multiTask")
                .cardinality(TaskCardinality.MULTI_INSTANCE)
                .lifecycle(TaskLifecycle.DYNAMIC)
                .plannedByDomainEvent(DOMAIN_EVENT_NAME)
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name(TEMPLATE_NAME)
                .templateHash("hash")
                .taskTypes(List.of(multiInstanceTaskType))
                .processRelationPatterns(List.of())
                .build();
        // Message with multiple origin task ids
        Message message = createMessageWithOriginTaskIds(Set.of("taskId1", "taskId2"));
        ProcessUpdate processUpdate = createDomainEventUpdate(message);
        ProcessInstance processInstance = createProcessInstanceWithTemplate(processTemplate);

        when(processUpdateQueryRepository.findByOriginProcessIdAndHandledFalse(ORIGIN_PROCESS_ID))
                .thenReturn(List.of(processUpdate));
        when(processInstanceRepository.findByOriginProcessId(ORIGIN_PROCESS_ID))
                .thenReturn(Optional.of(processInstance));
        when(messageRepository.findById(message.getId()))
                .thenReturn(Optional.of(message));
        when(messageReferenceRepository.save(any(MessageReference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        target.updateProcessState(ORIGIN_PROCESS_ID);

        // Two tasks should be planned (one for each origin task id)
        assertThat(processInstance.getTasks()).hasSize(2);
        Set<String> originTaskIds = Set.of(
                processInstance.getTasks().get(0).getOriginTaskId(),
                processInstance.getTasks().get(1).getOriginTaskId()
        );
        assertThat(originTaskIds).containsExactlyInAnyOrder("taskId1", "taskId2");
    }

    @Test
    void updateProcessState_domainEventWithInstantiationConditionFalse_doesNotPlanTask() {
        TaskType dynamicTaskType = TaskType.builder()
                .name("conditionalTask")
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .lifecycle(TaskLifecycle.DYNAMIC)
                .plannedByDomainEvent(DOMAIN_EVENT_NAME)
                .instantiationCondition(msg -> false)
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name(TEMPLATE_NAME)
                .templateHash("hash")
                .taskTypes(List.of(dynamicTaskType))
                .processRelationPatterns(List.of())
                .build();
        Message message = createMessage();
        ProcessUpdate processUpdate = createDomainEventUpdate(message);
        ProcessInstance processInstance = createProcessInstanceWithTemplate(processTemplate);

        when(processUpdateQueryRepository.findByOriginProcessIdAndHandledFalse(ORIGIN_PROCESS_ID))
                .thenReturn(List.of(processUpdate));
        when(processInstanceRepository.findByOriginProcessId(ORIGIN_PROCESS_ID))
                .thenReturn(Optional.of(processInstance));
        when(messageRepository.findById(message.getId()))
                .thenReturn(Optional.of(message));
        when(messageReferenceRepository.save(any(MessageReference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        target.updateProcessState(ORIGIN_PROCESS_ID);

        // No task should be planned due to condition returning false
        assertThat(processInstance.getTasks()).isEmpty();
    }

    @Test
    void updateProcessState_observedTask_createsCompletedObservationTask() {
        TaskType observedTaskType = TaskType.builder()
                .name("observedTask")
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .lifecycle(TaskLifecycle.OBSERVED)
                .observesMessage(DOMAIN_EVENT_NAME)
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name(TEMPLATE_NAME)
                .templateHash("hash")
                .taskTypes(List.of(observedTaskType))
                .processRelationPatterns(List.of())
                .build();
        Message message = createMessage();
        ProcessUpdate processUpdate = createDomainEventUpdate(message);
        ProcessInstance processInstance = createProcessInstanceWithTemplate(processTemplate);

        when(processUpdateQueryRepository.findByOriginProcessIdAndHandledFalse(ORIGIN_PROCESS_ID))
                .thenReturn(List.of(processUpdate));
        when(processInstanceRepository.findByOriginProcessId(ORIGIN_PROCESS_ID))
                .thenReturn(Optional.of(processInstance));
        when(messageRepository.findById(message.getId()))
                .thenReturn(Optional.of(message));
        when(messageReferenceRepository.save(any(MessageReference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        target.updateProcessState(ORIGIN_PROCESS_ID);

        // Observed task should be created in COMPLETED state
        assertThat(processInstance.getTasks()).hasSize(1);
        assertThat(processInstance.getTasks().getFirst().getTaskTypeName()).isEqualTo("observedTask");
        assertThat(processInstance.getTasks().getFirst().getState()).isEqualTo(TaskState.COMPLETED);
    }

    @Test
    void updateProcessState_observedTaskWithConditionFalse_doesNotCreateTask() {
        TaskType observedTaskType = TaskType.builder()
                .name("conditionalObservedTask")
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .lifecycle(TaskLifecycle.OBSERVED)
                .observesMessage(DOMAIN_EVENT_NAME)
                .instantiationCondition(msg -> false)
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name(TEMPLATE_NAME)
                .templateHash("hash")
                .taskTypes(List.of(observedTaskType))
                .processRelationPatterns(List.of())
                .build();
        Message message = createMessage();
        ProcessUpdate processUpdate = createDomainEventUpdate(message);
        ProcessInstance processInstance = createProcessInstanceWithTemplate(processTemplate);

        when(processUpdateQueryRepository.findByOriginProcessIdAndHandledFalse(ORIGIN_PROCESS_ID))
                .thenReturn(List.of(processUpdate));
        when(processInstanceRepository.findByOriginProcessId(ORIGIN_PROCESS_ID))
                .thenReturn(Optional.of(processInstance));
        when(messageRepository.findById(message.getId()))
                .thenReturn(Optional.of(message));
        when(messageReferenceRepository.save(any(MessageReference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        target.updateProcessState(ORIGIN_PROCESS_ID);

        // No task should be created due to condition returning false
        assertThat(processInstance.getTasks()).isEmpty();
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
        ProcessUpdate processUpdate = createDomainEventUpdate(message);
        ProcessInstance processInstance = createProcessInstanceWithTemplate(processTemplate);
        ProcessData processData = new ProcessData("myProcessDataKey", "myValue");
        processData.setProcessInstance(processInstance);

        Message correlatedMessage = createMessageWithName("correlatedEvent");

        when(processUpdateQueryRepository.findByOriginProcessIdAndHandledFalse(ORIGIN_PROCESS_ID))
                .thenReturn(List.of(processUpdate));
        when(processInstanceRepository.findByOriginProcessId(ORIGIN_PROCESS_ID))
                .thenReturn(Optional.of(processInstance));
        when(messageRepository.findById(message.getId()))
                .thenReturn(Optional.of(message));
        when(messageReferenceRepository.save(any(MessageReference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
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

        target.updateProcessState(ORIGIN_PROCESS_ID);

        // Verify the correlated message was directly applied (message reference saved for both original and correlated message)
        verify(messageReferenceRepository, times(2)).save(any(MessageReference.class));
        // Verify no ProcessUpdate was saved and no outdated event was produced
        verify(processUpdateRepository, never()).save(any(ProcessUpdate.class));
        verify(internalMessageProducer, never()).produceProcessContextOutdatedEventSynchronously(any());
    }

    @Test
    void updateProcessState_noCorrelationByProcessDataInAnyTemplate_skipsCorrelation() {
        ProcessTemplate processTemplate = createSimpleProcessTemplate();
        Message message = createMessage();
        ProcessUpdate processUpdate = createDomainEventUpdate(message);
        ProcessInstance processInstance = createProcessInstanceWithTemplate(processTemplate);
        ProcessData processData = new ProcessData("someKey", "someValue");
        processData.setProcessInstance(processInstance);

        when(processUpdateQueryRepository.findByOriginProcessIdAndHandledFalse(ORIGIN_PROCESS_ID))
                .thenReturn(List.of(processUpdate));
        when(processInstanceRepository.findByOriginProcessId(ORIGIN_PROCESS_ID))
                .thenReturn(Optional.of(processInstance));
        when(messageRepository.findById(message.getId()))
                .thenReturn(Optional.of(message));
        when(messageReferenceRepository.save(any(MessageReference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(processDataService.copyMessageDataToProcessData(processInstance, message))
                .thenReturn(List.of(processData));
        when(processTemplateRepository.isAnyTemplateHasEventsCorrelatedByProcessData())
                .thenReturn(false);

        target.updateProcessState(ORIGIN_PROCESS_ID);

        // Verify that correlation queries were not made (early exit because no template has process data correlation)
        verify(messageReferenceRepository, never()).findByProcessInstanceId(any());
    }

    @Test
    void updateProcessState_staticTaskCompletedByDomainEvent_completesTaskAndProcess() {
        TaskType staticTask = TaskType.builder()
                .name("staticTask")
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .lifecycle(TaskLifecycle.STATIC)
                .completedByDomainEvent(DOMAIN_EVENT_NAME)
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name(TEMPLATE_NAME)
                .templateHash("hash")
                .taskTypes(List.of(staticTask))
                .processRelationPatterns(List.of())
                .build();
        Message message = createMessage();
        ProcessUpdate processUpdate = createDomainEventUpdate(message);

        // Create and start the process instance to create the static task
        ProcessInstance processInstance = createProcessInstanceWithTemplate(processTemplate);
        processInstance.start();
        processContextRepositoryFacadeStub.setProcessInstance(processInstance);

        // Verify the task was created
        assertThat(processInstance.getTasks()).hasSize(1);
        assertThat(processInstance.getTasks().getFirst().getState()).isEqualTo(TaskState.PLANNED);

        when(processUpdateQueryRepository.findByOriginProcessIdAndHandledFalse(ORIGIN_PROCESS_ID))
                .thenReturn(List.of(processUpdate));
        when(processInstanceRepository.findByOriginProcessId(ORIGIN_PROCESS_ID))
                .thenReturn(Optional.of(processInstance));
        when(messageRepository.findById(message.getId()))
                .thenReturn(Optional.of(message));
        when(messageReferenceRepository.save(any(MessageReference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        target.updateProcessState(ORIGIN_PROCESS_ID);

        // Task should be completed and process should be completed
        assertThat(processInstance.getTasks()).hasSize(1);
        assertThat(processInstance.getTasks().getFirst().getState()).isEqualTo(TaskState.COMPLETED);
        assertThat(processInstance.getState()).isEqualTo(ProcessState.COMPLETED);
    }

    @Test
    void updateProcessState_templateMigrationAddsNewTask_plansNewStaticTask() {
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
        ProcessUpdate processUpdate = createDomainEventUpdate(message);

        // Create process instance with old template and start it
        ProcessInstance processInstance = createProcessInstanceWithTemplate(oldTemplate);
        processInstance.start();
        assertThat(processInstance.getTasks()).hasSize(1);

        // Simulate loading from DB with new template (template hash changed)
        clearTransientFields(processInstance);
        processInstance.onAfterLoadFromPersistentState(newTemplate, processContextFactory);

        when(processUpdateQueryRepository.findByOriginProcessIdAndHandledFalse(ORIGIN_PROCESS_ID))
                .thenReturn(List.of(processUpdate));
        when(processInstanceRepository.findByOriginProcessId(ORIGIN_PROCESS_ID))
                .thenReturn(Optional.of(processInstance));
        when(messageRepository.findById(message.getId()))
                .thenReturn(Optional.of(message));
        when(messageReferenceRepository.save(any(MessageReference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        target.updateProcessState(ORIGIN_PROCESS_ID);

        // New task should have been planned by migration
        assertThat(processInstance.getTasks()).hasSize(2);
        Set<String> taskNames = Set.of(
                processInstance.getTasks().get(0).getTaskTypeName(),
                processInstance.getTasks().get(1).getTaskTypeName()
        );
        assertThat(taskNames).containsExactlyInAnyOrder("existingTask", "newTask");
    }

    // Helper methods to create test data

    private ProcessTemplate createSimpleProcessTemplate() {
        TaskType staticTask = TaskType.builder()
                .name("staticTask")
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .lifecycle(TaskLifecycle.STATIC)
                .build();
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
        ProcessInstance processInstance = ProcessInstance.createProcessInstance(ORIGIN_PROCESS_ID, processTemplate, processContextFactory);
        processContextRepositoryFacadeStub.setProcessInstance(processInstance);
        return processInstance;
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

    private Message createMessageWithOriginTaskIds(Set<String> originTaskIds) {
        return Message.messageBuilder()
                .messageId(Generators.timeBasedEpochGenerator().generate().toString())
                .idempotenceId(Generators.timeBasedEpochGenerator().generate().toString())
                .messageName(DOMAIN_EVENT_NAME)
                .messageCreatedAt(ZonedDateTime.now())
                .createdAt(ZonedDateTime.now())
                .originTaskIds(OriginTaskId.from(TEMPLATE_NAME, originTaskIds))
                .messageData(Set.of(MessageData.builder()
                        .key("dataKey")
                        .value("dataValue")
                        .templateName(TEMPLATE_NAME)
                        .build()))
                .build();
    }

    private ProcessUpdate createProcessUpdate(Message message) {
        return ProcessUpdate.createProcessReceived()
                .originProcessId(ORIGIN_PROCESS_ID)
                .template(TEMPLATE_NAME)
                .messageReference(message.getId())
                .messageName(message.getMessageName())
                .idempotenceId(message.getIdempotenceId())
                .build();
    }

    private ProcessUpdate createDomainEventUpdate(Message message) {
        return ProcessUpdate.messageReceived()
                .originProcessId(ORIGIN_PROCESS_ID)
                .messageReference(message.getId())
                .messageName(message.getMessageName())
                .idempotenceId(message.getIdempotenceId())
                .build();
    }

    /**
     * Clears the transient fields to simulate JPA behavior after persistence.
     */
    private void clearTransientFields(ProcessInstance processInstance) {
        ReflectionTestUtils.setField(processInstance, "processTemplate", null);
        for (TaskInstance task : processInstance.getTasks()) {
            ReflectionTestUtils.setField(task, "taskType", Optional.empty());
        }
    }

}
