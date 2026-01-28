package ch.admin.bit.jeap.processcontext.domain.processinstance;

import java.util.Collection;

public interface RelationRepository {
    void saveAll(Collection<Relation> relations);
}
