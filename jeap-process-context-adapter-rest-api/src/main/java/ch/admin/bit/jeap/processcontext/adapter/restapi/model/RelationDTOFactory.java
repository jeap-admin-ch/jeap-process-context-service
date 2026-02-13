package ch.admin.bit.jeap.processcontext.adapter.restapi.model;

import ch.admin.bit.jeap.processcontext.domain.processinstance.Relation;
import ch.admin.bit.jeap.processcontext.domain.processinstance.RelationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RelationDTOFactory {
    private static final Comparator<Relation> RELATIONS_COMPARATOR = Comparator
            .comparing(Relation::getCreatedAt);

    private final RelationRepository relationRepository;

    public Page<RelationDTO> createRelationDTOPage(UUID processInstanceId, Pageable pageable) {
        return relationRepository.findByProcessInstanceId(processInstanceId, pageable)
                .map(RelationDTO::create);
    }

    static List<RelationDTO> createRelations(Set<Relation> relations) {
        return relations.stream()
                .sorted(RELATIONS_COMPARATOR)
                .map(RelationDTO::create)
                .toList();
    }

}
