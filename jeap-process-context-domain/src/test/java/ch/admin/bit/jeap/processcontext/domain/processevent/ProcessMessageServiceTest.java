package ch.admin.bit.jeap.processcontext.domain.processevent;

import ch.admin.bit.jeap.processcontext.domain.StubMetricsListener;
import ch.admin.bit.jeap.processcontext.domain.port.MetricsListener;
import ch.admin.bit.jeap.processcontext.domain.port.ProcessInstanceEventProducer;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceQueryRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceStubs;
import ch.admin.bit.jeap.processcontext.plugin.api.relation.Relation;
import ch.admin.bit.jeap.processcontext.plugin.api.relation.RelationListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessMessageServiceTest {
    @Mock
    private ProcessInstanceQueryRepository processInstanceQueryRepository;
    @Mock
    private ProcessEventRepository processEventRepository;
    @Mock
    private ProcessInstanceEventProducer processInstanceEventProducer;
    @Mock
    private RelationListener relationListener;
    @Captor
    private ArgumentCaptor<List<ProcessEvent>> processEventsCaptor;

    private ProcessEventService target;

    @BeforeEach
    void setUp() {
        MetricsListener metricsListener = new StubMetricsListener();
        target = new ProcessEventService(processInstanceQueryRepository, processEventRepository, processInstanceEventProducer, relationListener, metricsListener);
    }

    @Test
    void reactToProcessStateChange_started() {
        String originProcessId = "originProcessId";
        List<ProcessEvent> previouslyProducedEvents = List.of();
        ProcessInstance processInstance = ProcessInstanceStubs.createSimpleProcess();
        doReturn(Optional.of(processInstance)).when(processInstanceQueryRepository).findByOriginProcessIdWithoutLoadingMessages(originProcessId);
        doReturn(previouslyProducedEvents).when(processEventRepository).findByOriginProcessId(originProcessId);
        doNothing().when(processEventRepository).saveAll(processEventsCaptor.capture());

        target.reactToProcessStateChange(originProcessId);

        List<ProcessEvent> processEvents = processEventsCaptor.getValue();
        assertEquals(1, processEvents.size());
        assertSame(EventType.PROCESS_STARTED, processEvents.get(0).getEventType());
        verify(processInstanceEventProducer).produceProcessInstanceCreatedEventSynchronously(originProcessId, ProcessInstanceStubs.template);
        verifyNoMoreInteractions(processInstanceEventProducer);
    }

    @Test
    void reactToProcessStateChange_startedAndCompleted() {
        String originProcessId = "originProcessId";
        List<ProcessEvent> previouslyProducedEvents = List.of();
        ProcessInstance processInstance = ProcessInstanceStubs.createSimpleCompletedProcess();
        doReturn(Optional.of(processInstance)).when(processInstanceQueryRepository).findByOriginProcessIdWithoutLoadingMessages(originProcessId);
        doReturn(previouslyProducedEvents).when(processEventRepository).findByOriginProcessId(originProcessId);
        doNothing().when(processEventRepository).saveAll(processEventsCaptor.capture());

        target.reactToProcessStateChange(originProcessId);

        List<ProcessEvent> processEvents = processEventsCaptor.getValue();
        assertEquals(2, processEvents.size());
        assertSame(EventType.PROCESS_STARTED, processEvents.get(0).getEventType());
        assertSame(EventType.PROCESS_COMPLETED, processEvents.get(1).getEventType());
        verify(processInstanceEventProducer).produceProcessInstanceCreatedEventSynchronously(originProcessId, ProcessInstanceStubs.template);
        verify(processInstanceEventProducer).produceProcessInstanceCompletedEventSynchronously(originProcessId);
        verifyNoMoreInteractions(processInstanceEventProducer);
    }

    @Test
    void reactToProcessStateChange_whenAllEventsAlreadyProduced_thenShouldNotProduceAgain() {
        String originProcessId = "originProcessId";
        List<ProcessEvent> previouslyProducedEvents = List.of(
                ProcessEvent.createProcessStarted(originProcessId),
                ProcessEvent.createProcessCompleted(originProcessId)
        );
        ProcessInstance processInstance = ProcessInstanceStubs.createSimpleCompletedProcess();
        doReturn(Optional.of(processInstance)).when(processInstanceQueryRepository).findByOriginProcessIdWithoutLoadingMessages(originProcessId);
        doReturn(previouslyProducedEvents).when(processEventRepository).findByOriginProcessId(originProcessId);
        doNothing().when(processEventRepository).saveAll(processEventsCaptor.capture());

        target.reactToProcessStateChange(originProcessId);

        List<ProcessEvent> processEvents = processEventsCaptor.getValue();
        assertTrue(processEvents.isEmpty());
        Mockito.verifyNoInteractions(processInstanceEventProducer);
    }

    @Test
    void reactToMilestoneReached_shouldProduceEvent() {
        String originProcessId = "originProcessId";
        List<ProcessEvent> previouslyProducedEvents = List.of(ProcessEvent.createProcessStarted(originProcessId));
        String milestone1 = "milestone-1";
        String milestone2 = "milestone-2";
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithTwoReachedMilestonesAndOneUnreached(milestone1, milestone2);
        doReturn(Optional.of(processInstance)).when(processInstanceQueryRepository).findByOriginProcessIdWithoutLoadingMessages(originProcessId);
        doReturn(previouslyProducedEvents).when(processEventRepository).findByOriginProcessId(originProcessId);
        doNothing().when(processEventRepository).saveAll(processEventsCaptor.capture());

        target.reactToProcessStateChange(originProcessId);

        List<ProcessEvent> processEvents = processEventsCaptor.getValue();
        assertEquals(2, processEvents.size());
        assertSame(EventType.MILESTONE_REACHED, processEvents.get(0).getEventType());
        assertSame(EventType.MILESTONE_REACHED, processEvents.get(1).getEventType());
        verify(processInstanceEventProducer).produceProcessMilestoneReachedEventSynchronously(originProcessId, milestone1);
        verify(processInstanceEventProducer).produceProcessMilestoneReachedEventSynchronously(originProcessId, milestone2);
    }

    @Test
    void reactToMilestoneReached_shouldNotProduceEventIfAlreadyProduced() {
        String originProcessId = "originProcessId";
        String milestone1 = "milestone-1";
        String milestone2 = "milestone-2";
        List<ProcessEvent> previouslyProducedEvents = List.of(
                ProcessEvent.createProcessStarted(originProcessId),
                ProcessEvent.createMilestoneReached(originProcessId, milestone1));
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithTwoReachedMilestonesAndOneUnreached(milestone1, milestone2);
        doReturn(Optional.of(processInstance)).when(processInstanceQueryRepository).findByOriginProcessIdWithoutLoadingMessages(originProcessId);
        doReturn(previouslyProducedEvents).when(processEventRepository).findByOriginProcessId(originProcessId);
        doNothing().when(processEventRepository).saveAll(processEventsCaptor.capture());

        target.reactToProcessStateChange(originProcessId);

        List<ProcessEvent> processEvents = processEventsCaptor.getValue();
        assertEquals(1, processEvents.size());
        assertSame(EventType.MILESTONE_REACHED, processEvents.get(0).getEventType());
        verify(processInstanceEventProducer).produceProcessMilestoneReachedEventSynchronously(originProcessId, milestone2);
    }

    @Test
    void reactToAddedRelation_shouldProduceEvent() {
        String originProcessId = "originProcessId";
        List<ProcessEvent> previouslyProducedEvents = List.of(ProcessEvent.createProcessStarted(originProcessId));
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithRelation();
        doReturn(Optional.of(processInstance)).when(processInstanceQueryRepository).findByOriginProcessIdWithoutLoadingMessages(originProcessId);
        doReturn(previouslyProducedEvents).when(processEventRepository).findByOriginProcessId(originProcessId);
        doNothing().when(processEventRepository).saveAll(processEventsCaptor.capture());
        ch.admin.bit.jeap.processcontext.domain.processinstance.Relation domainRelation = processInstance.getRelations().iterator().next();
        Relation expectedRelation = Relation.builder()
                .systemId("ch.admin.test.System")
                .objectType("ch.admin.bit.entity.Foo")
                .objectId("someValue")
                .subjectType("ch.admin.bit.entity.Bar")
                .subjectId("someValue")
                .predicateType("ch.admin.bit.test.predicate.Knows")
                .idempotenceId(domainRelation.getIdempotenceId())
                .createdAt(domainRelation.getCreatedAt())
                .originProcessId(originProcessId)
                .build();

        target.reactToProcessStateChange(originProcessId);

        List<ProcessEvent> processEvents = processEventsCaptor.getValue();
        assertEquals(1, processEvents.size());
        assertSame(EventType.RELATION_ADDED, processEvents.get(0).getEventType());
        verify(relationListener).relationsAdded(List.of(expectedRelation));
    }

    @Test
    void reactToAddedRelation_shouldNotProduceEventIfAlreadyProduced() {
        String originProcessId = "originProcessId";
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithRelation();
        ch.admin.bit.jeap.processcontext.domain.processinstance.Relation domainRelation = processInstance.getRelations().iterator().next();
        List<ProcessEvent> previouslyProducedEvents = List.of(
                ProcessEvent.createProcessStarted(originProcessId),
                ProcessEvent.createRelationAdded(originProcessId, domainRelation.getIdempotenceId()));
        doReturn(Optional.of(processInstance)).when(processInstanceQueryRepository).findByOriginProcessIdWithoutLoadingMessages(originProcessId);
        doReturn(previouslyProducedEvents).when(processEventRepository).findByOriginProcessId(originProcessId);
        doNothing().when(processEventRepository).saveAll(processEventsCaptor.capture());

        target.reactToProcessStateChange(originProcessId);

        List<ProcessEvent> processEvents = processEventsCaptor.getValue();
        assertEquals(0, processEvents.size());
        verifyNoInteractions(relationListener);
    }

    @Test
    void reactToSnapshotCreated_whenFirstSnapshotVersionCreated_shouldProduceEvent() {
        final String originProcessId = "originProcessId";
        final int snapshotVersion = 1;
        final List<ProcessEvent> previouslyProducedEvents = List.of(ProcessEvent.createProcessStarted(originProcessId));
        ProcessInstance processInstance = ProcessInstanceStubs.createSimpleProcess();
        ReflectionTestUtils.setField(processInstance, "latestSnapshotVersion", snapshotVersion);
        doReturn(Optional.of(processInstance)).when(processInstanceQueryRepository).findByOriginProcessIdWithoutLoadingMessages(originProcessId);
        doReturn(previouslyProducedEvents).when(processEventRepository).findByOriginProcessId(originProcessId);
        doNothing().when(processEventRepository).saveAll(processEventsCaptor.capture());

        target.reactToProcessStateChange(originProcessId);

        List<ProcessEvent> processEvents = processEventsCaptor.getValue();
        assertEquals(1, processEvents.size());
        assertSame(EventType.SNAPSHOT_CREATED, processEvents.get(0).getEventType());
        assertEquals(Integer.toString(snapshotVersion), processEvents.get(0).getName());
    }


    @Test
    void reactToSnapshotCreated_whenAdditionalSnapshotVersionsCreated_shouldProduceEvents() {
        final String originProcessId = "originProcessId";
        final int latestSnapshotVersion = 5;
        final List<ProcessEvent> previouslyProducedEvents = List.of(
                ProcessEvent.createProcessStarted(originProcessId),
                // snapshot creations already notified for versions 1 and 3
                ProcessEvent.createSnapshotCreated(originProcessId, "1"),
                ProcessEvent.createSnapshotCreated(originProcessId, "3"));
        ProcessInstance processInstance = ProcessInstanceStubs.createSimpleProcess();
        // latest version 5 -> snapshot creation notifications are missing for versions 2, 4 and 5
        ReflectionTestUtils.setField(processInstance, "latestSnapshotVersion", latestSnapshotVersion);
        doReturn(Optional.of(processInstance)).when(processInstanceQueryRepository).findByOriginProcessIdWithoutLoadingMessages(originProcessId);
        doReturn(previouslyProducedEvents).when(processEventRepository).findByOriginProcessId(originProcessId);
        doNothing().when(processEventRepository).saveAll(processEventsCaptor.capture());

        target.reactToProcessStateChange(originProcessId);

        List<ProcessEvent> processEvents = processEventsCaptor.getValue();
        assertEquals(3, processEvents.size());
        processEvents.forEach(event -> assertSame(EventType.SNAPSHOT_CREATED, processEvents.get(0).getEventType()));
        assertEquals("2", processEvents.get(0).getName());
        assertEquals("4", processEvents.get(1).getName());
        assertEquals("5", processEvents.get(2).getName());
    }

    @Test
    void reactToSnapshotCreated_whenAllSnapshotCreationsAlreadyNotified_shouldNotProduceEvents() {
        final String originProcessId = "originProcessId";
        final int latestSnapshotVersion = 2;
        final List<ProcessEvent> previouslyProducedEvents = List.of(
                ProcessEvent.createProcessStarted(originProcessId),
                // snapshot creations already notified for versions 1 and 2
                ProcessEvent.createSnapshotCreated(originProcessId, "1"),
                ProcessEvent.createSnapshotCreated(originProcessId, "2"));
        ProcessInstance processInstance = ProcessInstanceStubs.createSimpleProcess();
        // latest version 2 -> no snapshot creation notifications are missing
        ReflectionTestUtils.setField(processInstance, "latestSnapshotVersion", latestSnapshotVersion);
        doReturn(Optional.of(processInstance)).when(processInstanceQueryRepository).findByOriginProcessIdWithoutLoadingMessages(originProcessId);
        doReturn(previouslyProducedEvents).when(processEventRepository).findByOriginProcessId(originProcessId);
        doNothing().when(processEventRepository).saveAll(processEventsCaptor.capture());

        target.reactToProcessStateChange(originProcessId);

        List<ProcessEvent> processEvents = processEventsCaptor.getValue();
        assertEquals(0, processEvents.size());
    }
}
