package ch.admin.bit.jeap.processcontext.domain.processevent;

import ch.admin.bit.jeap.processcontext.domain.port.MetricsListener;
import ch.admin.bit.jeap.processcontext.domain.port.ProcessInstanceEventProducer;
import ch.admin.bit.jeap.processcontext.domain.processinstance.*;
import ch.admin.bit.jeap.processcontext.plugin.api.relation.RelationListener;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.togglz.core.context.FeatureContext;
import org.togglz.core.util.NamedFeature;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessEventService {
    private final ProcessInstanceQueryRepository processInstanceQueryRepository;
    private final ProcessEventRepository processEventRepository;
    private final ProcessInstanceEventProducer processInstanceEventProducer;
    private final RelationListener relationListener;
    private final MetricsListener metricsListener;

    @Transactional
    @Timed(value = "jeap_pcs_react_to_process_state_change", description = "Produce new process events", percentiles = {0.5, 0.8, 0.95, 0.99})
    public void reactToProcessStateChange(String originProcessId) {
        Map<EventType, List<ProcessEvent>> previouslyProducedEventsByType = findPreviouslyProducedEvents(originProcessId);

        // Produce events, then commit produced events to database and ack the event triggering the state change reactions
        List<ProcessEvent> newProducedEvents = produceProcessEvents(originProcessId, previouslyProducedEventsByType);
        processEventRepository.saveAll(newProducedEvents);
    }

    private List<ProcessEvent> produceProcessEvents(String originProcessId, Map<EventType, List<ProcessEvent>> producedEvents) {
        ProcessInstance processInstance = processInstanceQueryRepository.findByOriginProcessIdWithoutLoadingMessages(originProcessId)
                .orElseThrow(NotFoundException.processNotFound(originProcessId));
        log.debug("Producing process events for process instance {}", processInstance.toString());

        String processName = processInstance.getProcessTemplateName();

        List<ProcessEvent> events = new ArrayList<>();
        metricsListener.timed("jeap_pcs_produce_process_state_changed_events", Map.of(), () ->
                produceProcessStateEvents(originProcessId, producedEvents, processInstance, processName, events));

        metricsListener.timed("jeap_pcs_produce_milestone_events", Map.of(), () ->
                produceMilestoneNotifications(originProcessId, producedEvents, processInstance, events));

        metricsListener.timed("jeap_pcs_produce_relation_events", Map.of(), () ->
                produceRelationNotifications(originProcessId, producedEvents, processInstance, events));

        metricsListener.timed("jeap_pcs_produce_snapshot_events", Map.of(), () ->
                produceSnapshotNotifications(originProcessId, producedEvents, processInstance, events));

        events.forEach(event -> metricsListener.processEventCreated(processInstance.getProcessTemplate(), event.getEventType()));
        return events;
    }

    private void produceProcessStateEvents(String originProcessId, Map<EventType, List<ProcessEvent>> producedEvents, ProcessInstance processInstance, String processName, List<ProcessEvent> events) {
        boolean isProcessStartedProduced = producedEvents.containsKey(EventType.PROCESS_STARTED);
        if (!isProcessStartedProduced) {
            log.debug("Producing process instance created event for process {}", originProcessId);
            processInstanceEventProducer.produceProcessInstanceCreatedEventSynchronously(originProcessId, processName);
            events.add(ProcessEvent.createProcessStarted(originProcessId));
        }

        boolean isProcessCompletedProduced = producedEvents.containsKey(EventType.PROCESS_COMPLETED);
        if (processInstance.getState() == ProcessState.COMPLETED && !isProcessCompletedProduced) {
            log.debug("Producing process instance completed event for process {}", originProcessId);
            metricsListener.processCompleted(processInstance.getProcessTemplate());
            processInstanceEventProducer.produceProcessInstanceCompletedEventSynchronously(originProcessId);
            events.add(ProcessEvent.createProcessCompleted(originProcessId));
        }
    }

    private void produceRelationNotifications(String originProcessId, Map<EventType, List<ProcessEvent>> producedEvents, ProcessInstance processInstance, List<ProcessEvent> events) {
        Set<String> notifiedRelations = producedEvents.getOrDefault(EventType.RELATION_ADDED, List.of()).stream()
                .map(ProcessEvent::getName)
                .collect(toSet());
        Set<Relation> processInstanceRelations = processInstance.getRelations();
        log.debug("Found {} relations for process {}", processInstanceRelations.size(), originProcessId);
        processInstanceRelations = processInstanceRelations.stream()
                .filter(r -> !notifiedRelations.contains(r.getIdempotenceId().toString())).collect(Collectors.toSet());
        log.debug("Found {} new relations for process {}", processInstanceRelations.size(), originProcessId);
        Map<String, Relation> relations = processInstanceRelations.stream()
                .filter(this::isFeatureFlagActive)
                .collect(toMap(r -> r.getIdempotenceId().toString(), r -> r));

        if (!relations.isEmpty()) {
            log.debug("Notifying relation listener, relations added: {}", relations);
            metricsListener.timed("jeap_pcs_notify_relations_added", Map.of("relationsCount", Integer.toString(relations.size())), () ->
                    relationListener.relationsAdded(
                            relations.values().stream().map(relation -> RelationMapper.toApiObject(originProcessId, relation)).toList()));
            events.addAll(relations.values().stream().map(relation -> ProcessEvent.createRelationAdded(originProcessId, relation.getIdempotenceId())).toList());
        }
    }

    private boolean isFeatureFlagActive(Relation relation) {
        if (relation.getFeatureFlag() != null) {
            boolean active = FeatureContext.getFeatureManager().isActive(new NamedFeature(relation.getFeatureFlag()));
            log.debug("FeatureFlag={} relation={} state={}", relation.getFeatureFlag(), relation.getId(), active);
            return active;
        }
        return true;
    }

    private void produceMilestoneNotifications(String originProcessId, Map<EventType, List<ProcessEvent>> producedEvents, ProcessInstance processInstance, List<ProcessEvent> events) {
        Set<String> reachedMilestones = processInstance.getReachedMilestones();
        Set<String> milestonesWithProducedEvent = producedEvents.getOrDefault(EventType.MILESTONE_REACHED, List.of()).stream()
                .map(ProcessEvent::getName)
                .collect(toSet());

        Set<String> milestonesRequiringEvent = new HashSet<>(reachedMilestones);
        milestonesRequiringEvent.removeAll(milestonesWithProducedEvent);
        milestonesRequiringEvent.forEach(milestoneName -> {
            log.debug("Producing milestone reached event for milestone {} in process {}", milestoneName, originProcessId);
            metricsListener.milestoneReached(processInstance.getProcessTemplate(), milestoneName);
            processInstanceEventProducer.produceProcessMilestoneReachedEventSynchronously(originProcessId, milestoneName);
            events.add(ProcessEvent.createMilestoneReached(originProcessId, milestoneName));
        });
    }

    private void produceSnapshotNotifications(String originProcessId, Map<EventType, List<ProcessEvent>> producedEvents, ProcessInstance processInstance, List<ProcessEvent> events) {
        Set<String> snapshotsWithProducedEvent = producedEvents.getOrDefault(EventType.SNAPSHOT_CREATED, List.of()).stream()
                .map(ProcessEvent::getName)
                .collect(toSet());
        for (int version = 1; version <= processInstance.getLatestSnapshotVersion(); version++) {
            String versionString = Integer.toString(version);
            if (!snapshotsWithProducedEvent.contains(versionString)) {
                log.debug("Producing snapshot created event for snapshot version {} in process {}", versionString, originProcessId);
                processInstanceEventProducer.produceProcessSnapshotCreatedEventSynchronously(originProcessId, version);
                metricsListener.snapshotCreated(processInstance.getProcessTemplate());
                events.add(ProcessEvent.createSnapshotCreated(originProcessId, versionString));
            }
        }
    }

    private Map<EventType, List<ProcessEvent>> findPreviouslyProducedEvents(String originProcessId) {
        return processEventRepository.findByOriginProcessId(originProcessId)
                .stream()
                .collect(Collectors.groupingBy(ProcessEvent::getEventType));
    }
}
