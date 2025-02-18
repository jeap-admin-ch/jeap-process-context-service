package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.PcsConfigProperties;
import ch.admin.bit.jeap.processcontext.domain.StubMetricsListener;
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
import ch.admin.bit.jeap.processcontext.plugin.api.event.EmptySetPayloadExtractor;
import ch.admin.bit.jeap.processcontext.plugin.api.event.EmptySetReferenceExtractor;
import ch.admin.bit.jeap.processcontext.plugin.api.event.MessageProcessIdCorrelationProvider;
import ch.admin.bit.jeap.processcontext.plugin.api.event.NeverProcessInstantiationCondition;
import com.fasterxml.uuid.Generators;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessInstanceServiceTest {
    @Mock
    private ProcessInstanceRepository processInstanceRepository;
    @Mock
    private ProcessTemplateRepository processTemplateRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private InternalMessageProducer internalMessageProducer;
    @Mock
    private ProcessUpdateQueryRepository processUpdateQueryRepository;
    @Captor
    private ArgumentCaptor<Message> eventArgumentCaptor;
    @Mock
    private ProcessUpdateRepository processUpdateRepository;
    @Mock
    private ProcessSnapshotService processSnapshotService;

    private PcsConfigProperties pcsConfigProperties;

    private ProcessInstanceService target;

    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        transactionManager = mock(PlatformTransactionManager.class);
        pcsConfigProperties = new PcsConfigProperties();
        Transactions transactions = new Transactions(transactionManager);
        MetricsListener metricsListener = new StubMetricsListener();
        target = new ProcessInstanceService(internalMessageProducer,
                processUpdateQueryRepository, processInstanceRepository, processTemplateRepository, messageRepository,
                processUpdateRepository, processSnapshotService, transactions, metricsListener, pcsConfigProperties);
    }

    @Test
    void createProcessInstance_alreadyExists() {
        String originProcessId = "originProcessId";
        String processTemplateName = "originProcessId";
        doReturn(true).when(processInstanceRepository).existsByOriginProcessId(originProcessId);

        target.createProcessInstance(originProcessId, processTemplateName, emptySet());

        verify(processInstanceRepository, never()).save(any());
        verify(processUpdateRepository, never()).save(any());
        verify(internalMessageProducer).produceProcessContextOutdatedEventSynchronously(originProcessId);
    }

    @Test
    void createProcessInstance_notYetExisting() {
        String originProcessId = "originProcessId";
        String processTemplateName = "processTemplateName";
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleDynamicTaskInstance();
        ProcessTemplate processTemplate = processInstance.getProcessTemplate();
        Set<ProcessData> processData = processInstance.getProcessData();

        ArgumentCaptor<ProcessInstance> processInstanceArgumentCaptor = ArgumentCaptor.forClass(ProcessInstance.class);
        doReturn(processInstance).when(processInstanceRepository).save(processInstanceArgumentCaptor.capture());
        doReturn(Optional.of(processTemplate)).when(processTemplateRepository).findByName(processTemplateName);

        target.createProcessInstance(originProcessId, processTemplateName, processData);

        assertEquals(originProcessId, processInstanceArgumentCaptor.getValue().getOriginProcessId());
        assertEquals(processTemplate, processInstanceArgumentCaptor.getValue().getProcessTemplate());
        assertEquals(processData, processInstanceArgumentCaptor.getValue().getProcessData());
        verify(internalMessageProducer).produceProcessContextOutdatedEventSynchronously(originProcessId);
        verify(processUpdateRepository).save(any());
    }

    @Test
    void createProcessInstance_templateNotExisting() {
        String originProcessId = "originProcessId";
        String processTemplateName = "processTemplateName";
        doReturn(Optional.empty()).when(processTemplateRepository).findByName(processTemplateName);

        assertThrows(NotFoundException.class, () -> target.createProcessInstance(originProcessId, processTemplateName, emptySet()));

        verify(processInstanceRepository, never()).save(any());
        verify(processUpdateRepository, never()).save(any());
        verify(internalMessageProducer, never()).produceProcessContextOutdatedEventSynchronously(any());
    }

    @Test
    void updateProcessState_processNotKnown_ignored() {
        String originProcessId = "originProcessId";
        ProcessInstance processInstance = mock(ProcessInstance.class);
        ProcessUpdate update = ProcessUpdate.messageReceived()
                .originProcessId(originProcessId)
                .idempotenceId("id")
                .messageName("event")
                .messageReference(Generators.timeBasedEpochGenerator().generate())
                .build();

        verify(processInstance, never()).addMessage(any());
        verify(internalMessageProducer, never()).produceProcessContextStateChangedEventSynchronously(any());
        verify(processUpdateRepository, never()).markHandlingFailed(update.getId());
    }

    @Test
    void updateProcessState_noUpdateToRun() {
        String originProcessId = "originProcessId";
        ProcessInstance processInstance = mock(ProcessInstance.class);
        doReturn(List.of()).when(processUpdateQueryRepository).findByOriginProcessIdAndHandledFalse(originProcessId);
        when(processInstanceRepository.existsByOriginProcessId(originProcessId)).thenReturn(true);

        target.updateProcessState(originProcessId);

        verify(processInstance, never()).addMessage(any());
        verify(internalMessageProducer).produceProcessContextStateChangedEventSynchronously(originProcessId);
        verify(processInstance, never()).evaluateReachedMilestones();
    }

    @Test
    void updateProcessState_domainEvent() {
        String originProcessId = "originProcessId";
        UUID eventReference = Generators.timeBasedEpochGenerator().generate();
        ProcessInstance processInstance = mock(ProcessInstance.class);
        ProcessUpdate processUpdate = mock(ProcessUpdate.class);
        Message message = mock(Message.class);
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name("templateName")
                .templateHash("hash")
                .taskTypes(List.of(TaskType.builder()
                        .name("task")
                        .cardinality(TaskCardinality.SINGLE_INSTANCE)
                        .lifecycle(TaskLifecycle.DYNAMIC)
                        .build()))
                .build();

        doReturn(Optional.of(processInstance)).when(processInstanceRepository).findByOriginProcessIdLoadingMessages(originProcessId);
        doReturn(processTemplate).when(processInstance).getProcessTemplate();
        doReturn(true).when(processInstanceRepository).existsByOriginProcessId(originProcessId);
        doReturn(List.of(processUpdate)).when(processUpdateQueryRepository).findByOriginProcessIdAndHandledFalse(originProcessId);
        when(messageRepository.findById(eventReference)).thenReturn(Optional.of(message));
        when(processUpdate.getProcessUpdateType()).thenReturn(ProcessUpdateType.DOMAIN_EVENT);
        when(processUpdate.getMessageName()).thenReturn("myDomainEvent");
        when(processUpdate.getMessageReference()).thenReturn(Optional.of(eventReference));
        ZonedDateTime now = ZonedDateTime.now();
        when(message.getMessageCreatedAt()).thenReturn(now);

        target.updateProcessState(originProcessId);

        verify(processInstance).addMessage(eventArgumentCaptor.capture());
        verify(processInstance).evaluateCompletedTasksAndReachedMilestones(now);
        verify(internalMessageProducer).produceProcessContextStateChangedEventSynchronously(originProcessId);
    }

    @Test
    void updateProcessState_domainEvent_createSingleTask() {
        String originProcessId = "originProcessId";
        UUID eventReference = Generators.timeBasedEpochGenerator().generate();
        ProcessInstance processInstance = mock(ProcessInstance.class);
        ProcessUpdate processUpdate = mock(ProcessUpdate.class);
        Message message = mock(Message.class);
        MessageReferenceMessageDTO messageReferenceMessageDTO = mock(MessageReferenceMessageDTO.class);
        TaskType taskType = TaskType.builder()
                .name("task")
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .lifecycle(TaskLifecycle.DYNAMIC)
                .plannedByDomainEvent("myDomainEvent")
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name("templateName")
                .templateHash("hash")
                .taskTypes(List.of(taskType))
                .build();

        doReturn(Optional.of(processInstance)).when(processInstanceRepository).findByOriginProcessIdLoadingMessages(originProcessId);
        doReturn(processTemplate).when(processInstance).getProcessTemplate();
        doReturn(messageReferenceMessageDTO).when(processInstance).addMessage(message);
        doReturn(true).when(processInstanceRepository).existsByOriginProcessId(originProcessId);
        doReturn(List.of(processUpdate)).when(processUpdateQueryRepository).findByOriginProcessIdAndHandledFalse(originProcessId);
        when(messageRepository.findById(eventReference)).thenReturn(Optional.of(message));
        when(processUpdate.getProcessUpdateType()).thenReturn(ProcessUpdateType.DOMAIN_EVENT);
        when(processUpdate.getMessageName()).thenReturn("myDomainEvent");
        when(processUpdate.getMessageReference()).thenReturn(Optional.of(eventReference));
        ZonedDateTime now = ZonedDateTime.now();
        when(message.getMessageCreatedAt()).thenReturn(now);

        target.updateProcessState(originProcessId);

        verify(processInstance).planDomainEventTask(eq(taskType), eq(null), any(), eq(null));
        verify(processInstance, times(0)).addObservationTask(any(), any(), any(), eq(null));
        verify(processInstance).addMessage(eventArgumentCaptor.capture());
        verify(processInstance).evaluateCompletedTasksAndReachedMilestones(now);
        verify(internalMessageProducer).produceProcessContextStateChangedEventSynchronously(originProcessId);
    }

    @Test
    void updateProcessState_domainEvent_createSingleTask_withConditionResolvingToTrue() {
        String originProcessId = "originProcessId";
        UUID eventReference = Generators.timeBasedEpochGenerator().generate();
        ProcessInstance processInstance = mock(ProcessInstance.class);
        ProcessUpdate processUpdate = mock(ProcessUpdate.class);
        Message message = mock(Message.class);
        MessageReferenceMessageDTO messageReferenceMessageDTO = mock(MessageReferenceMessageDTO.class);
        TaskType taskType = TaskType.builder()
                .name("task")
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .lifecycle(TaskLifecycle.DYNAMIC)
                .plannedByDomainEvent("myDomainEvent")
                .instantiationCondition(incomingMessage -> true)
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name("templateName")
                .templateHash("hash")
                .taskTypes(List.of(taskType))
                .build();

        doReturn(Optional.of(processInstance)).when(processInstanceRepository).findByOriginProcessIdLoadingMessages(originProcessId);
        doReturn(processTemplate).when(processInstance).getProcessTemplate();
        doReturn(messageReferenceMessageDTO).when(processInstance).addMessage(message);
        doReturn(true).when(processInstanceRepository).existsByOriginProcessId(originProcessId);
        doReturn(List.of(processUpdate)).when(processUpdateQueryRepository).findByOriginProcessIdAndHandledFalse(originProcessId);
        when(messageRepository.findById(eventReference)).thenReturn(Optional.of(message));
        when(processUpdate.getProcessUpdateType()).thenReturn(ProcessUpdateType.DOMAIN_EVENT);
        when(processUpdate.getMessageName()).thenReturn("myDomainEvent");
        when(processUpdate.getMessageReference()).thenReturn(Optional.of(eventReference));
        ZonedDateTime now = ZonedDateTime.now();
        when(message.getMessageCreatedAt()).thenReturn(now);

        target.updateProcessState(originProcessId);

        verify(processInstance).planDomainEventTask(eq(taskType), eq(null), any(), eq(null));
        verify(processInstance, times(0)).addObservationTask(any(), any(), any(), eq(null));
        verify(processInstance).addMessage(eventArgumentCaptor.capture());
        verify(processInstance).evaluateCompletedTasksAndReachedMilestones(now);
        verify(internalMessageProducer).produceProcessContextStateChangedEventSynchronously(originProcessId);
    }

    @Test
    void updateProcessState_domainEvent_doNotCreateSingleTask_withConditionResolvingToFalse() {
        String originProcessId = "originProcessId";
        UUID eventReference = Generators.timeBasedEpochGenerator().generate();
        ProcessInstance processInstance = mock(ProcessInstance.class);
        ProcessUpdate processUpdate = mock(ProcessUpdate.class);
        Message message = mock(Message.class);
        MessageReferenceMessageDTO messageReferenceMessageDTO = mock(MessageReferenceMessageDTO.class);
        TaskType taskType = TaskType.builder()
                .name("task")
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .lifecycle(TaskLifecycle.DYNAMIC)
                .plannedByDomainEvent("myDomainEvent")
                .instantiationCondition(incomingMessage -> false)
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name("templateName")
                .templateHash("hash")
                .taskTypes(List.of(taskType))
                .build();

        doReturn(Optional.of(processInstance)).when(processInstanceRepository).findByOriginProcessIdLoadingMessages(originProcessId);
        doReturn(processTemplate).when(processInstance).getProcessTemplate();
        doReturn(messageReferenceMessageDTO).when(processInstance).addMessage(message);
        doReturn(true).when(processInstanceRepository).existsByOriginProcessId(originProcessId);
        doReturn(List.of(processUpdate)).when(processUpdateQueryRepository).findByOriginProcessIdAndHandledFalse(originProcessId);
        when(messageRepository.findById(eventReference)).thenReturn(Optional.of(message));
        when(processUpdate.getProcessUpdateType()).thenReturn(ProcessUpdateType.DOMAIN_EVENT);
        when(processUpdate.getMessageName()).thenReturn("myDomainEvent");
        when(processUpdate.getMessageReference()).thenReturn(Optional.of(eventReference));
        ZonedDateTime now = ZonedDateTime.now();
        when(message.getMessageCreatedAt()).thenReturn(now);

        target.updateProcessState(originProcessId);

        verify(processInstance, times(0)).planDomainEventTask(any(), any(), any(), eq(null));
        verify(processInstance, times(0)).addObservationTask(any(), any(), any(), eq(null));
        verify(processInstance).addMessage(eventArgumentCaptor.capture());
        verify(processInstance).evaluateCompletedTasksAndReachedMilestones(now);
        verify(internalMessageProducer).produceProcessContextStateChangedEventSynchronously(originProcessId);
    }

    @Test
    void updateProcessState_domainEvent_createMultipleTasks() {
        String originProcessId = "originProcessId";
        UUID eventReference = Generators.timeBasedEpochGenerator().generate();
        ProcessInstance processInstance = mock(ProcessInstance.class);
        ProcessUpdate processUpdate = mock(ProcessUpdate.class);
        Message message = mock(Message.class);
        MessageReferenceMessageDTO messageReferenceMessageDTO = mock(MessageReferenceMessageDTO.class);
        TaskType taskType = TaskType.builder()
                .name("task")
                .cardinality(TaskCardinality.MULTI_INSTANCE)
                .lifecycle(TaskLifecycle.DYNAMIC)
                .plannedByDomainEvent("myDomainEvent")
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name("templateName")
                .templateHash("hash")
                .taskTypes(List.of(taskType))
                .build();

        doReturn(Optional.of(processInstance)).when(processInstanceRepository).findByOriginProcessIdLoadingMessages(originProcessId);
        doReturn(processTemplate).when(processInstance).getProcessTemplate();
        doReturn(messageReferenceMessageDTO).when(processInstance).addMessage(message);
        doReturn(true).when(processInstanceRepository).existsByOriginProcessId(originProcessId);
        doReturn(List.of(processUpdate)).when(processUpdateQueryRepository).findByOriginProcessIdAndHandledFalse(originProcessId);
        doReturn(Sets.newHashSet("1", "2")).when(messageReferenceMessageDTO).getRelatedOriginTaskIds();
        when(messageRepository.findById(eventReference)).thenReturn(Optional.of(message));
        when(processUpdate.getProcessUpdateType()).thenReturn(ProcessUpdateType.DOMAIN_EVENT);
        when(processUpdate.getMessageName()).thenReturn("myDomainEvent");
        when(processUpdate.getMessageReference()).thenReturn(Optional.of(eventReference));
        ZonedDateTime now = ZonedDateTime.now();
        when(message.getMessageCreatedAt()).thenReturn(now);

        target.updateProcessState(originProcessId);

        verify(processInstance, times(2)).planDomainEventTask(eq(taskType), anyString(), any(), eq(null));
        verify(processInstance, times(0)).addObservationTask(any(), any(), any(), eq(null));
        verify(processInstance).addMessage(eventArgumentCaptor.capture());
        verify(processInstance).evaluateCompletedTasksAndReachedMilestones(now);
        verify(internalMessageProducer).produceProcessContextStateChangedEventSynchronously(originProcessId);
    }

    @Test
    void updateProcessState_domainEvent_createMultipleTasks_withConditionResolvingToTrue() {
        String originProcessId = "originProcessId";
        UUID eventReference = Generators.timeBasedEpochGenerator().generate();
        ProcessInstance processInstance = mock(ProcessInstance.class);
        ProcessUpdate processUpdate = mock(ProcessUpdate.class);
        Message message = mock(Message.class);
        MessageReferenceMessageDTO messageReferenceMessageDTO = mock(MessageReferenceMessageDTO.class);
        TaskType taskType = TaskType.builder()
                .name("task")
                .cardinality(TaskCardinality.MULTI_INSTANCE)
                .lifecycle(TaskLifecycle.DYNAMIC)
                .plannedByDomainEvent("myDomainEvent")
                .instantiationCondition(incomingMessage -> true)
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name("templateName")
                .templateHash("hash")
                .taskTypes(List.of(taskType))
                .build();

        doReturn(Optional.of(processInstance)).when(processInstanceRepository).findByOriginProcessIdLoadingMessages(originProcessId);
        doReturn(processTemplate).when(processInstance).getProcessTemplate();
        doReturn(messageReferenceMessageDTO).when(processInstance).addMessage(message);
        doReturn(true).when(processInstanceRepository).existsByOriginProcessId(originProcessId);
        doReturn(List.of(processUpdate)).when(processUpdateQueryRepository).findByOriginProcessIdAndHandledFalse(originProcessId);
        doReturn(Sets.newHashSet("1", "2")).when(messageReferenceMessageDTO).getRelatedOriginTaskIds();
        when(messageRepository.findById(eventReference)).thenReturn(Optional.of(message));
        when(processUpdate.getProcessUpdateType()).thenReturn(ProcessUpdateType.DOMAIN_EVENT);
        when(processUpdate.getMessageName()).thenReturn("myDomainEvent");
        when(processUpdate.getMessageReference()).thenReturn(Optional.of(eventReference));
        ZonedDateTime now = ZonedDateTime.now();
        when(message.getMessageCreatedAt()).thenReturn(now);

        target.updateProcessState(originProcessId);

        verify(processInstance, times(2)).planDomainEventTask(eq(taskType), anyString(), any(), eq(null));
        verify(processInstance, times(0)).addObservationTask(any(), any(), any(), eq(null));
        verify(processInstance).addMessage(eventArgumentCaptor.capture());
        verify(processInstance).evaluateCompletedTasksAndReachedMilestones(now);
        verify(internalMessageProducer).produceProcessContextStateChangedEventSynchronously(originProcessId);
    }

    @Test
    void updateProcessState_domainEvent_doNotCreateMultipleTasks_withConditionResolvingToFalse() {
        String originProcessId = "originProcessId";
        UUID eventReference = Generators.timeBasedEpochGenerator().generate();
        ProcessInstance processInstance = mock(ProcessInstance.class);
        ProcessUpdate processUpdate = mock(ProcessUpdate.class);
        Message message = mock(Message.class);
        MessageReferenceMessageDTO messageReferenceMessageDTO = mock(MessageReferenceMessageDTO.class);
        TaskType taskType = TaskType.builder()
                .name("task")
                .cardinality(TaskCardinality.MULTI_INSTANCE)
                .lifecycle(TaskLifecycle.DYNAMIC)
                .plannedByDomainEvent("myDomainEvent")
                .instantiationCondition(incomingMessage -> false)
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name("templateName")
                .templateHash("hash")
                .taskTypes(List.of(taskType))
                .build();

        doReturn(Optional.of(processInstance)).when(processInstanceRepository).findByOriginProcessIdLoadingMessages(originProcessId);
        doReturn(processTemplate).when(processInstance).getProcessTemplate();
        doReturn(messageReferenceMessageDTO).when(processInstance).addMessage(message);
        doReturn(true).when(processInstanceRepository).existsByOriginProcessId(originProcessId);
        doReturn(List.of(processUpdate)).when(processUpdateQueryRepository).findByOriginProcessIdAndHandledFalse(originProcessId);
        doReturn(Sets.newHashSet("1", "2")).when(messageReferenceMessageDTO).getRelatedOriginTaskIds();
        when(messageRepository.findById(eventReference)).thenReturn(Optional.of(message));
        when(processUpdate.getProcessUpdateType()).thenReturn(ProcessUpdateType.DOMAIN_EVENT);
        when(processUpdate.getMessageName()).thenReturn("myDomainEvent");
        when(processUpdate.getMessageReference()).thenReturn(Optional.of(eventReference));
        ZonedDateTime now = ZonedDateTime.now();
        when(message.getMessageCreatedAt()).thenReturn(now);

        target.updateProcessState(originProcessId);

        verify(processInstance, times(0)).planDomainEventTask(eq(taskType), anyString(), any(), eq(null));
        verify(processInstance, times(0)).addObservationTask(any(), any(), any(), eq(null));
        verify(processInstance).addMessage(eventArgumentCaptor.capture());
        verify(processInstance).evaluateCompletedTasksAndReachedMilestones(now);
        verify(internalMessageProducer).produceProcessContextStateChangedEventSynchronously(originProcessId);
    }

    @Test
    void updateProcessState_domainEvent_completeSingleObservationTask() {
        String originProcessId = "originProcessId";
        UUID eventReference = Generators.timeBasedEpochGenerator().generate();
        ProcessInstance processInstance = mock(ProcessInstance.class);
        ProcessUpdate processUpdate = mock(ProcessUpdate.class);
        Message message = mock(Message.class);
        MessageReferenceMessageDTO messageReferenceMessageDTO = mock(MessageReferenceMessageDTO.class);
        TaskType taskType = TaskType.builder()
                .name("task")
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .lifecycle(TaskLifecycle.OBSERVED)
                .observesMessage("myDomainEvent")
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name("templateName")
                .templateHash("hash")
                .taskTypes(List.of(taskType))
                .build();

        doReturn(Optional.of(processInstance)).when(processInstanceRepository).findByOriginProcessIdLoadingMessages(originProcessId);
        doReturn(processTemplate).when(processInstance).getProcessTemplate();
        doReturn(messageReferenceMessageDTO).when(processInstance).addMessage(message);
        doReturn(true).when(processInstanceRepository).existsByOriginProcessId(originProcessId);
        doReturn(List.of(processUpdate)).when(processUpdateQueryRepository).findByOriginProcessIdAndHandledFalse(originProcessId);
        when(messageRepository.findById(eventReference)).thenReturn(Optional.of(message));
        when(processUpdate.getProcessUpdateType()).thenReturn(ProcessUpdateType.DOMAIN_EVENT);
        when(processUpdate.getMessageName()).thenReturn("myDomainEvent");
        when(processUpdate.getMessageReference()).thenReturn(Optional.of(eventReference));
        ZonedDateTime now = ZonedDateTime.now();
        when(message.getMessageCreatedAt()).thenReturn(now);

        target.updateProcessState(originProcessId);

        verify(processInstance, times(0)).planDomainEventTask(any(), any(), any(), eq(null));
        verify(processInstance).addObservationTask(eq(taskType), eq(null), any(), eq(null));
        verify(processInstance).addMessage(eventArgumentCaptor.capture());
        verify(processInstance).evaluateCompletedTasksAndReachedMilestones(now);
        verify(internalMessageProducer).produceProcessContextStateChangedEventSynchronously(originProcessId);
    }

    @Test
    void updateProcessState_domainEvent_completeSingleObservationTask_withConditionResolvingToTrue() {
        String originProcessId = "originProcessId";
        UUID eventReference = Generators.timeBasedEpochGenerator().generate();
        ProcessInstance processInstance = mock(ProcessInstance.class);
        ProcessUpdate processUpdate = mock(ProcessUpdate.class);
        Message message = mock(Message.class);
        MessageReferenceMessageDTO messageReferenceMessageDTO = mock(MessageReferenceMessageDTO.class);
        TaskType taskType = TaskType.builder()
                .name("task")
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .lifecycle(TaskLifecycle.OBSERVED)
                .observesMessage("myDomainEvent")
                .instantiationCondition(incomingMessage -> true)
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name("templateName")
                .templateHash("hash")
                .taskTypes(List.of(taskType))
                .build();

        doReturn(Optional.of(processInstance)).when(processInstanceRepository).findByOriginProcessIdLoadingMessages(originProcessId);
        doReturn(processTemplate).when(processInstance).getProcessTemplate();
        doReturn(messageReferenceMessageDTO).when(processInstance).addMessage(message);
        doReturn(true).when(processInstanceRepository).existsByOriginProcessId(originProcessId);
        doReturn(List.of(processUpdate)).when(processUpdateQueryRepository).findByOriginProcessIdAndHandledFalse(originProcessId);
        when(messageRepository.findById(eventReference)).thenReturn(Optional.of(message));
        when(processUpdate.getProcessUpdateType()).thenReturn(ProcessUpdateType.DOMAIN_EVENT);
        when(processUpdate.getMessageName()).thenReturn("myDomainEvent");
        when(processUpdate.getMessageReference()).thenReturn(Optional.of(eventReference));
        ZonedDateTime now = ZonedDateTime.now();
        when(message.getMessageCreatedAt()).thenReturn(now);

        target.updateProcessState(originProcessId);

        verify(processInstance, times(0)).planDomainEventTask(any(), any(), any(), eq(null));
        verify(processInstance).addObservationTask(eq(taskType), eq(null), any(), eq(null));
        verify(processInstance).addMessage(eventArgumentCaptor.capture());
        verify(processInstance).evaluateCompletedTasksAndReachedMilestones(now);
        verify(internalMessageProducer).produceProcessContextStateChangedEventSynchronously(originProcessId);
    }

    @Test
    void updateProcessState_domainEvent_completeSingleObservationTask_withConditionResolvingToFalse() {
        String originProcessId = "originProcessId";
        UUID eventReference = Generators.timeBasedEpochGenerator().generate();
        ProcessInstance processInstance = mock(ProcessInstance.class);
        ProcessUpdate processUpdate = mock(ProcessUpdate.class);
        Message message = mock(Message.class);
        MessageReferenceMessageDTO messageReferenceMessageDTO = mock(MessageReferenceMessageDTO.class);
        TaskType taskType = TaskType.builder()
                .name("task")
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .lifecycle(TaskLifecycle.OBSERVED)
                .observesMessage("myDomainEvent")
                .instantiationCondition(incomingMessage -> false)
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name("templateName")
                .templateHash("hash")
                .taskTypes(List.of(taskType))
                .build();

        doReturn(Optional.of(processInstance)).when(processInstanceRepository).findByOriginProcessIdLoadingMessages(originProcessId);
        doReturn(processTemplate).when(processInstance).getProcessTemplate();
        doReturn(messageReferenceMessageDTO).when(processInstance).addMessage(message);
        doReturn(true).when(processInstanceRepository).existsByOriginProcessId(originProcessId);
        doReturn(List.of(processUpdate)).when(processUpdateQueryRepository).findByOriginProcessIdAndHandledFalse(originProcessId);
        when(messageRepository.findById(eventReference)).thenReturn(Optional.of(message));
        when(processUpdate.getProcessUpdateType()).thenReturn(ProcessUpdateType.DOMAIN_EVENT);
        when(processUpdate.getMessageName()).thenReturn("myDomainEvent");
        when(processUpdate.getMessageReference()).thenReturn(Optional.of(eventReference));
        ZonedDateTime now = ZonedDateTime.now();
        when(message.getMessageCreatedAt()).thenReturn(now);

        target.updateProcessState(originProcessId);

        verify(processInstance, times(0)).planDomainEventTask(any(), any(), any(), eq(null));
        verify(processInstance, times(0)).addObservationTask(any(), any(), any(), eq(null));
        verify(processInstance).addMessage(eventArgumentCaptor.capture());
        verify(processInstance).evaluateCompletedTasksAndReachedMilestones(now);
        verify(internalMessageProducer).produceProcessContextStateChangedEventSynchronously(originProcessId);
    }

    @Test
    void updateProcessState_createProcess() {
        String originProcessId = "originProcessId";
        UUID eventReference = Generators.timeBasedEpochGenerator().generate();
        ProcessInstance processInstance = mock(ProcessInstance.class);
        ProcessUpdate processUpdate = mock(ProcessUpdate.class);
        Message message = mock(Message.class);
        when(message.getMessageId()).thenReturn("messageId");
        final String templateName = "templateName";
        doReturn(Optional.empty(), Optional.of(processInstance)).when(processInstanceRepository).findByOriginProcessIdLoadingMessages(originProcessId);
        when(processInstanceRepository.save(any())).thenReturn(processInstance);
        doReturn(List.of(processUpdate)).when(processUpdateQueryRepository).findByOriginProcessIdAndHandledFalse(originProcessId);
        when(messageRepository.findById(eventReference)).thenReturn(Optional.of(message));
        when(processUpdate.getProcessUpdateType()).thenReturn(ProcessUpdateType.CREATE_PROCESS);
        when(processUpdate.getMessageReference()).thenReturn(Optional.of(eventReference));
        when(processUpdate.getParams()).thenReturn(templateName);
        when(processUpdate.getOriginProcessId()).thenReturn(originProcessId);

        MessageReferenceMessageDTO messageReferenceMessageDTO = mock(MessageReferenceMessageDTO.class);
        TaskType taskType = TaskType.builder()
                .name("task")
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .lifecycle(TaskLifecycle.OBSERVED)
                .observesMessage("myDomainEvent")
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name("templateName")
                .templateHash("hash")
                .taskTypes(List.of(taskType))
                .build();
        when(processTemplateRepository.findByName(templateName)).thenReturn(Optional.of(processTemplate));
        doReturn(processTemplate).when(processInstance).getProcessTemplate();
        doReturn(messageReferenceMessageDTO).when(processInstance).addMessage(message);
        doReturn(true).when(processInstanceRepository).existsByOriginProcessId(originProcessId);
        when(messageRepository.findById(eventReference)).thenReturn(Optional.of(message));
        when(processUpdate.getMessageName()).thenReturn("myDomainEvent");
        ZonedDateTime now = ZonedDateTime.now();
        when(message.getMessageCreatedAt()).thenReturn(now);

        target.updateProcessState(originProcessId);

        verify(processInstanceRepository).save(any());
        verify(processInstance).addObservationTask(eq(taskType), eq("messageId"), any(), eq(null));
    }

    @Test
    void updateProcessState_correlateEvents() {
        String originProcessId = "originProcessId";
        UUID eventReference = Generators.timeBasedEpochGenerator().generate();
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getOriginProcessId()).thenReturn(originProcessId);
        ProcessUpdate processUpdate = mock(ProcessUpdate.class);
        Message message = mock(Message.class);
        doReturn(List.of(processUpdate)).when(processUpdateQueryRepository).findByOriginProcessIdAndHandledFalse(originProcessId);
        when(messageRepository.findById(eventReference)).thenReturn(Optional.of(message));
        when(processUpdate.getProcessUpdateType()).thenReturn(ProcessUpdateType.PROCESS_CREATED);
        when(processUpdate.getMessageReference()).thenReturn(Optional.of(eventReference));

        when(message.getId()).thenReturn(Generators.timeBasedEpochGenerator().generate());
        when(message.getMessageName()).thenReturn("eventName");
        when(message.getIdempotenceId()).thenReturn("test");

        doReturn(Optional.of(processInstance)).when(processInstanceRepository).findByOriginProcessIdLoadingMessages(originProcessId);

        when(processInstance.getProcessData()).thenReturn(Set.of(
                new ProcessData("myPDKey", "myValue", "myRole"),
                new ProcessData("myPDKeyTwo", "myValueTwo")));

        List<MessageReference> messageReferences = List.of(
                MessageReference.builder()
                        .messageName("eventName")
                        .topicName("topicName")
                        .correlationProvider(new MessageProcessIdCorrelationProvider())
                        .payloadExtractor(new EmptySetPayloadExtractor())
                        .referenceExtractor(new EmptySetReferenceExtractor())
                        .processInstantiationCondition(new NeverProcessInstantiationCondition())
                        .correlatedByProcessData(CorrelatedByProcessData.builder()
                                .processDataKey("myPDKey")
                                .messageDataKey("myEventDataKey")
                                .build())
                        .build(),
                MessageReference.builder()
                        .messageName("eventNameTwo")
                        .topicName("topicName")
                        .correlationProvider(new MessageProcessIdCorrelationProvider())
                        .payloadExtractor(new EmptySetPayloadExtractor())
                        .referenceExtractor(new EmptySetReferenceExtractor())
                        .processInstantiationCondition(new NeverProcessInstantiationCondition())
                        .correlatedByProcessData(CorrelatedByProcessData.builder()
                                .processDataKey("myPDKeyTwo")
                                .messageDataKey("myEventDataKeyTwo")
                                .build())
                        .build());


        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name("templateName")
                .templateHash("hash")
                .messageReferences(messageReferences)
                .processDataTemplates(
                        List.of(ProcessDataTemplate.builder()
                                        .key("myPDKey")
                                        .sourceMessageDataKey("myEventDataKey")
                                        .sourceMessageName("eventName")
                                        .build(),
                                ProcessDataTemplate.builder()
                                        .key("myPDKeyTwo")
                                        .sourceMessageDataKey("myEventDataKeyTwo")
                                        .sourceMessageName("eventNameTwo")
                                        .build())
                )
                .taskTypes(List.of(TaskType.builder()
                        .name("task").cardinality(TaskCardinality.SINGLE_INSTANCE).lifecycle(TaskLifecycle.STATIC)
                        .build()))
                .build();

        when(processInstance.getProcessTemplate()).thenReturn(processTemplate);
        when(processInstance.getProcessTemplateName()).thenReturn(processTemplate.getName());

        when(messageRepository.findMessagesToCorrelate(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(List.of(message));

        when(messageRepository.findMessagesToCorrelate(anyString(), anyString(), anyString(), anyString())).thenReturn(List.of(message));

        target.updateProcessState(originProcessId);

        verify(processUpdateRepository, times(2)).save(any());

        verify(internalMessageProducer).produceProcessContextOutdatedEventSynchronously(originProcessId);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void updateProcessState_processCreated() {
        String originProcessId = "originProcessId";
        UUID eventReference = Generators.timeBasedEpochGenerator().generate();
        ProcessInstance processInstance = mock(ProcessInstance.class);
        ProcessUpdate processUpdateProcessCreated = mock(ProcessUpdate.class);
        ProcessUpdate processUpdateOther = mock(ProcessUpdate.class);
        Message message = mock(Message.class);
        doReturn(Optional.of(processInstance)).when(processInstanceRepository).findByOriginProcessIdLoadingMessages(originProcessId);
        doReturn(List.of(processUpdateOther, processUpdateProcessCreated)).when(processUpdateQueryRepository).findByOriginProcessIdAndHandledFalse(originProcessId);
        when(messageRepository.findById(eventReference)).thenReturn(Optional.of(message));
        when(processUpdateProcessCreated.getProcessUpdateType()).thenReturn(ProcessUpdateType.PROCESS_CREATED);
        when(processUpdateOther.getProcessUpdateType()).thenReturn(ProcessUpdateType.PROCESS_CREATED);
        when(processUpdateProcessCreated.getMessageReference()).thenReturn(Optional.of(eventReference));
        when(processUpdateOther.getMessageReference()).thenReturn(Optional.of(eventReference));

        target.updateProcessState(originProcessId);

        InOrder inOrder = inOrder(processUpdateProcessCreated, processUpdateOther);
        inOrder.verify(processUpdateProcessCreated).getId();
    }
}
