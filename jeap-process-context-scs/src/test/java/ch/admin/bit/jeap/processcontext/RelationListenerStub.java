package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.plugin.api.relation.Relation;
import ch.admin.bit.jeap.processcontext.plugin.api.relation.RelationListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RelationListenerStub implements RelationListener {

    private final List<Relation> relations = new ArrayList<>();

    List<Relation> getRelations(String originProcessId) {
        return relations.stream()
                .filter(r -> r.getOriginProcessId().equals(originProcessId))
                .toList();
    }

    @Override
    public void relationsAdded(Collection<Relation> newRelations) {
        relations.addAll(newRelations);
    }
}
