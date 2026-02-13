package ch.admin.bit.jeap.processcontext.domain.processinstance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface RelationRepository {
    /**
     * Saves all given relations if they do not already exist.
     * @param relations The relations to save.
     * @return The relations that were saved and did not already exist.
     */
    Set<Relation> saveAllNewRelations(Collection<Relation> relations);

    Page<Relation> findByProcessInstanceId(UUID processInstanceId, Pageable pageable);
}
