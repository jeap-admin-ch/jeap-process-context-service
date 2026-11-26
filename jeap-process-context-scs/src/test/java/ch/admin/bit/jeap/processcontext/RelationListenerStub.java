package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.plugin.api.relation.Relation;
import ch.admin.bit.jeap.processcontext.plugin.api.relation.RelationListener;
import ch.admin.bit.jeap.processcontext.plugin.api.relation.RelationType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class RelationListenerStub implements RelationListener {

    private final List<Relation> relations = new ArrayList<>();
    private final List<Set<RelationType>> declaredRelationTypes = new ArrayList<>();

    List<Relation> getRelations(String originProcessId) {
        return relations.stream()
                .filter(r -> r.getOriginProcessId().equals(originProcessId))
                .toList();
    }

    /**
     * @return The relation types reported by each invocation of {@link #relationTypesDeclared(Set)}
     */
    List<Set<RelationType>> getDeclaredRelationTypes() {
        return List.copyOf(declaredRelationTypes);
    }

    @Override
    public void relationsAdded(Collection<Relation> newRelations) {
        relations.addAll(newRelations);
    }

    @Override
    public void relationTypesDeclared(Set<RelationType> relationTypes) {
        declaredRelationTypes.add(relationTypes);
    }
}
