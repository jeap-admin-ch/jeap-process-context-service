package ch.admin.bit.jeap.processcontext.domain.processinstance.relation;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessData;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.Relation;
import ch.admin.bit.jeap.processcontext.domain.processinstance.RelationRepository;
import ch.admin.bit.jeap.processcontext.plugin.api.relation.RelationListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.util.NamedFeature;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RelationService {

    private final RelationRepository relationRepository;
    private final RelationFactory relationFactory;
    private final RelationListener relationListener;
    private final FeatureManager featureManager;

    public void onNewProcessData(ProcessInstance processInstance, List<ProcessData> newProcessData) {
        Set<Relation> relations = relationFactory.createNewRelations(processInstance, newProcessData);

        relations.forEach(Relation::onPrePersist);
        Set<Relation> newRelations = relationRepository.saveAllNewRelations(relations);

        notifyRelationListeners(processInstance, newRelations);
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
