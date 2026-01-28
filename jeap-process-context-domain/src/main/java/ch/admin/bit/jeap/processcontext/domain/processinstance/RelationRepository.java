package ch.admin.bit.jeap.processcontext.domain.processinstance;

import java.util.Collection;
import java.util.Set;

public interface RelationRepository {
    void saveAll(Collection<Relation> relations);

    Set<Relation> findByProcessInstance(ProcessInstance processInstance);
}
