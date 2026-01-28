package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.Relation;
import ch.admin.bit.jeap.processcontext.domain.processinstance.RelationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@RequiredArgsConstructor
class RelationRepositoryImpl implements RelationRepository {

    private final RelationJpaRepository relationJpaRepository;

    @Override
    public void saveAll(Collection<Relation> relations) {
        relationJpaRepository.saveAll(relations);
    }
}
