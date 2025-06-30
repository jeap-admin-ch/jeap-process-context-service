package ch.admin.bit.jeap.processcontext.domain.processevent;

import ch.admin.bit.jeap.processcontext.domain.port.MetricsListener;
import ch.admin.bit.jeap.processcontext.domain.port.ProcessInstanceEventProducer;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceQueryRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.Relation;
import ch.admin.bit.jeap.processcontext.plugin.api.relation.RelationListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.util.NamedFeature;

import java.time.ZonedDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessEventServiceTest {

    @Mock
    ProcessInstanceQueryRepository processInstanceQueryRepository;

    @Mock
    @SuppressWarnings("unused")
    ProcessInstanceEventProducer processInstanceEventProducer;

    @Mock
    ProcessEventRepository processEventRepository;

    @Mock
    RelationListener relationListener;

    @Mock
    MetricsListener metricsListener;

    @Mock
    FeatureManager featureManager;

    @InjectMocks
    ProcessEventService service;

    @Captor
    private ArgumentCaptor<Collection<ch.admin.bit.jeap.processcontext.plugin.api.relation.Relation>> relationListCaptor;

    @Captor
    private ArgumentCaptor<Collection<ProcessEvent>> newProducedEventCaptor;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> {
            Runnable callback = invocation.getArgument(2);
            callback.run();
            return null;
        }).when(metricsListener).timed(anyString(), any(), any(Runnable.class));
    }


    @Test
    void producesRelationAddedEventsForNewRelations() {
        String originProcessId = "process-123";
        String relationNamefeatureFlagActive = "featureFlagActive";
        String relationNamefeatureFlagInactive = "featureFlagInactive";
        UUID activeRelationIdempotenceId = UUID.randomUUID();
        UUID inactiveRelationIdempotenceId = UUID.randomUUID();
        ProcessInstance processInstance = mockProcessInstanceWithRelations(Set.of(
                mockRelation(activeRelationIdempotenceId, relationNamefeatureFlagActive, true),
                mockRelation(inactiveRelationIdempotenceId, relationNamefeatureFlagInactive, false)));
        when(processInstanceQueryRepository.findByOriginProcessIdWithoutLoadingMessages(originProcessId)).thenReturn(Optional.of(processInstance));
        when(processEventRepository.findByOriginProcessId(originProcessId)).thenReturn(List.of());

        when(featureManager.isActive(new NamedFeature(relationNamefeatureFlagActive))).thenReturn(true);
        when(featureManager.isActive(new NamedFeature(relationNamefeatureFlagInactive))).thenReturn(false);

        service.reactToProcessStateChange(originProcessId);

        verify(relationListener, times(1)).relationsAdded(relationListCaptor.capture());
        verify(processEventRepository, times(1)).saveAll(newProducedEventCaptor.capture());

        Collection<ch.admin.bit.jeap.processcontext.plugin.api.relation.Relation> relationListCaptorValue = relationListCaptor.getValue();
        assertThat(relationListCaptorValue).hasSize(1);
        assertThat(relationListCaptorValue.iterator().next().getPredicateType()).isEqualTo("predicate-type-" + relationNamefeatureFlagActive);

        Collection<ProcessEvent> newProducedEventCaptorValue = newProducedEventCaptor.getValue();
        assertThat(newProducedEventCaptorValue).hasSize(3);

        List<EventType> eventTypesList = newProducedEventCaptorValue.stream().map(ProcessEvent::getEventType).toList();
        assertThat(eventTypesList).containsExactly(EventType.PROCESS_STARTED, EventType.RELATION_ADDED, EventType.RELATION_PROHIBITED);
        assertEventTypeAndName(newProducedEventCaptorValue, EventType.RELATION_ADDED, activeRelationIdempotenceId.toString());
        assertEventTypeAndName(newProducedEventCaptorValue, EventType.RELATION_PROHIBITED, inactiveRelationIdempotenceId.toString());
    }

    @Test
    void producesRelationAddedEventsForNewRelationsWithoutFeatureFlag() {
        String originProcessId = "process-123";
        UUID activeRelationIdempotenceId = UUID.randomUUID();
        ProcessInstance processInstance = mockProcessInstanceWithRelations(Set.of(mockRelation(activeRelationIdempotenceId, null, true)));
        when(processInstanceQueryRepository.findByOriginProcessIdWithoutLoadingMessages(originProcessId)).thenReturn(Optional.of(processInstance));
        when(processEventRepository.findByOriginProcessId(originProcessId)).thenReturn(List.of());

        service.reactToProcessStateChange(originProcessId);

        verify(relationListener, times(1)).relationsAdded(relationListCaptor.capture());
        verify(processEventRepository, times(1)).saveAll(newProducedEventCaptor.capture());

        Collection<ch.admin.bit.jeap.processcontext.plugin.api.relation.Relation> relationListCaptorValue = relationListCaptor.getValue();
        assertThat(relationListCaptorValue).hasSize(1);

        Collection<ProcessEvent> newProducedEventCaptorValue = newProducedEventCaptor.getValue();
        assertThat(newProducedEventCaptorValue).hasSize(2);

        List<EventType> eventTypesList = newProducedEventCaptorValue.stream().map(ProcessEvent::getEventType).toList();
        assertThat(eventTypesList).containsExactly(EventType.PROCESS_STARTED, EventType.RELATION_ADDED);
        assertEventTypeAndName(newProducedEventCaptorValue, EventType.RELATION_ADDED, activeRelationIdempotenceId.toString());
    }

    @Test
    void noRelationAddedEventProducedForAlreadyProhibitedOrAddedRelations() {
        String originProcessId = "process-123";
        UUID relationIdempotenceId1 = UUID.randomUUID();
        UUID relationIdempotenceId2 = UUID.randomUUID();
        ProcessInstance processInstance = mockProcessInstanceWithRelations(Set.of(mockSimpleRelation(relationIdempotenceId1), mockSimpleRelation(relationIdempotenceId2)));
        when(processInstanceQueryRepository.findByOriginProcessIdWithoutLoadingMessages(originProcessId)).thenReturn(Optional.of(processInstance));
        ProcessEvent processEvent1 = mockProcessEvent(relationIdempotenceId1.toString(), EventType.RELATION_ADDED);
        ProcessEvent processEvent2 = mockProcessEvent(relationIdempotenceId2.toString(), EventType.RELATION_PROHIBITED);
        when(processEventRepository.findByOriginProcessId(originProcessId)).thenReturn(List.of(processEvent1, processEvent2));

        service.reactToProcessStateChange(originProcessId);

        verify(relationListener, never()).relationsAdded(any());
        verify(processEventRepository, times(1)).saveAll(newProducedEventCaptor.capture());

        Collection<ProcessEvent> newProducedEventCaptorValue = newProducedEventCaptor.getValue();
        assertThat(newProducedEventCaptorValue).hasSize(1);

        List<EventType> eventTypesList = newProducedEventCaptorValue.stream().map(ProcessEvent::getEventType).toList();
        assertThat(eventTypesList).containsExactly(EventType.PROCESS_STARTED);
    }

    private ProcessEvent mockProcessEvent(String name, EventType eventType) {
        ProcessEvent processEvent = mock(ProcessEvent.class);
        when(processEvent.getEventType()).thenReturn(eventType);
        when(processEvent.getName()).thenReturn(name);
        return processEvent;
    }

    private ProcessInstance mockProcessInstanceWithRelations(Set<Relation> relation) {
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getRelations()).thenReturn(relation);
        return processInstance;
    }


    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private void assertEventTypeAndName(Collection<ProcessEvent> events, EventType eventType, String eventTypeName) {
        assertThat(events.stream().filter(e -> e.getEventType().equals(eventType)).findFirst().get().getName())
                .isEqualTo(eventTypeName);
    }

    private Relation mockRelation(UUID idempotenceId, String featureFlagName, boolean active) {
        Relation relation = mock(Relation.class);
        if (active) {
            when(relation.getSystemId()).thenReturn("system-id");
            when(relation.getObjectId()).thenReturn("object-id");
            when(relation.getObjectType()).thenReturn("object-type");
            when(relation.getSubjectId()).thenReturn("subject-id");
            when(relation.getSubjectType()).thenReturn("subject-type");
            when(relation.getPredicateType()).thenReturn("predicate-type-" + featureFlagName);
            when(relation.getCreatedAt()).thenReturn(ZonedDateTime.now());
        }
        when(relation.getIdempotenceId()).thenReturn(idempotenceId);
        if (featureFlagName != null) {
            when(relation.getFeatureFlag()).thenReturn(featureFlagName);
        }
        return relation;
    }

    private Relation mockSimpleRelation(UUID idempotenceId) {
        Relation relation = mock(Relation.class);
        when(relation.getIdempotenceId()).thenReturn(idempotenceId);
        return relation;
    }

}
