package ch.admin.bit.jeap.processcontext.domain.processinstance.relation;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessData;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.Relation;
import ch.admin.bit.jeap.processcontext.domain.processinstance.RelationRepository;
import ch.admin.bit.jeap.processcontext.plugin.api.relation.RelationListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RelationService {

    private final RelationRepository relationRepository;
    private final RelationFactory relationFactory;
    private final RelationListener relationListener;

    public void onNewProcessData(ProcessInstance processInstance, List<ProcessData> newProcessData) {
        Collection<Relation> relations = relationFactory.createNewRelations(processInstance, newProcessData);

        relationRepository.saveAll(relations);

        notifyRelationListeners(processInstance, relations);
    }

    private void notifyRelationListeners(ProcessInstance processInstance, Collection<Relation> relations) {
        var apiRelations = relations.stream()
                .map(rel -> RelationMapper.toApiObject(processInstance.getOriginProcessId(), rel))
                .toList();
        relationListener.relationsAdded(apiRelations);
    }
}
