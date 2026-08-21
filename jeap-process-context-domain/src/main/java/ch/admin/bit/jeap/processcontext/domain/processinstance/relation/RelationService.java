package ch.admin.bit.jeap.processcontext.domain.processinstance.relation;

import ch.admin.bit.jeap.processcontext.domain.port.MetricsListener;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceProperties;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessData;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.Relation;
import ch.admin.bit.jeap.processcontext.domain.processinstance.RelationRepository;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.RelationPattern;
import ch.admin.bit.jeap.processcontext.plugin.api.relation.RelationListener;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.util.NamedFeature;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RelationService {

    private final RelationRepository relationRepository;
    private final RelationFactory relationFactory;
    private final RelationListener relationListener;
    private final FeatureManager featureManager;
    private final MetricsListener metricsListener;
    private final RelationCandidateRepository relationCandidateRepository;
    private final MaintenanceProperties maintenanceProperties;


    @Timed(value = "jeap_pcs_relation_service_new_process_data", percentiles = {0.5, 0.8, 0.99})
    public void onNewProcessData(ProcessInstance processInstance, List<ProcessData> newProcessData) {
        Set<Relation> relations = relationFactory.createNewRelations(processInstance, newProcessData);

        saveAndNotify(processInstance, relations);
    }

    @Timed(value = "jeap_pcs_relation_service_reevaluate", percentiles = {0.5, 0.8, 0.99})
    public void reevaluateRelations(ProcessInstance processInstance) {
        List<RelationPattern> patterns = processInstance.getProcessTemplate().getRelationPatterns();
        verifyCandidateLimit(processInstance, patterns);
        patterns.forEach(pattern -> createRelations(processInstance, pattern));
    }

    private void verifyCandidateLimit(ProcessInstance processInstance, List<RelationPattern> patterns) {
        long maxCandidates = maintenanceProperties.getLimits().getMaxRelationCandidatesPerTask();
        long candidates = 0;
        for (RelationPattern pattern : patterns) {
            RelationCandidateCursor cursor = null;
            while (true) {
                long remaining = maxCandidates - candidates;
                int limit = (int) Math.min(maintenanceProperties.getLimits().getRelationReevaluationPageSize(),
                        remaining == Long.MAX_VALUE ? remaining : remaining + 1);
                limit = Math.max(1, limit);
                List<RelationCandidate> page = relationCandidateRepository.findCandidates(
                        processInstance.getId(), pattern, cursor, limit);
                candidates += page.size();
                if (candidates > maxCandidates) {
                    throw new RelationCandidateLimitExceededException(processInstance.getId(), maxCandidates);
                }
                if (page.size() < limit) {
                    break;
                }
                cursor = page.getLast().cursor();
            }
        }
    }

    private void createRelations(ProcessInstance processInstance, RelationPattern pattern) {
        int pageSize = maintenanceProperties.getLimits().getRelationReevaluationPageSize();
        RelationCandidateCursor cursor = null;
        while (true) {
            List<RelationCandidate> page = relationCandidateRepository.findCandidates(
                    processInstance.getId(), pattern, cursor, pageSize);
            if (page.isEmpty()) {
                return;
            }
            Set<Relation> relations = page.stream()
                    .map(candidate -> relationFactory.createRelation(processInstance, pattern, candidate))
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            saveAndNotify(processInstance, relations);
            if (page.size() < pageSize) {
                return;
            }
            cursor = page.getLast().cursor();
        }
    }

    private void saveAndNotify(ProcessInstance processInstance, Set<Relation> relations) {
        relations.forEach(Relation::onPrePersist);
        Set<Relation> newRelations = relationRepository.saveAllNewRelations(relations);

        if (!newRelations.isEmpty()) {
            metricsListener.timed("jeap_pcs_relation_service_notify_listeners", Map.of(), () -> notifyRelationListeners(processInstance, newRelations));
        }
    }

    private void notifyRelationListeners(ProcessInstance processInstance, Collection<Relation> relations) {
        var apiRelations = relations.stream()
                .filter(this::isFeatureFlagActive)
                .map(rel -> RelationMapper.toApiObject(processInstance.getOriginProcessId(), rel))
                .toList();
        relationListener.relationsAdded(apiRelations);
    }

    private boolean isFeatureFlagActive(Relation relation) {
        if (relation.getFeatureFlag() != null) {
            return featureManager.isActive(new NamedFeature(relation.getFeatureFlag()));
        }
        // no feature flag defined, relation is always notified
        return true;
    }
}
