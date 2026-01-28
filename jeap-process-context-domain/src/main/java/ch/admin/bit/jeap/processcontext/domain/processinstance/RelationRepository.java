package ch.admin.bit.jeap.processcontext.domain.processinstance;

import java.util.Collection;
import java.util.Set;

public interface RelationRepository {
    /**
     * Saves all given relations if they do not already exist.
     * @param relations The relations to save.
     * @return The relations that were saved and did not already exist.
     */
    Set<Relation> saveAllNewRelations(Collection<Relation> relations);

    Set<Relation> findByProcessInstance(ProcessInstance processInstance);
}
