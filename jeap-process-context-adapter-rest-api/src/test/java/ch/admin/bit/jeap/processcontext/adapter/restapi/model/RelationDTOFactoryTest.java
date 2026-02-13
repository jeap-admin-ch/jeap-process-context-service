package ch.admin.bit.jeap.processcontext.adapter.restapi.model;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.Relation;
import ch.admin.bit.jeap.processcontext.domain.processinstance.RelationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RelationDTOFactoryTest {

    private RelationRepository relationRepository;
    private RelationDTOFactory relationDTOFactory;

    @BeforeEach
    void setUp() {
        relationRepository = mock(RelationRepository.class);
        relationDTOFactory = new RelationDTOFactory(relationRepository);
    }

    @Test
    void createRelationDTOPage_returnsPageOfRelationDTOs() {
        UUID processInstanceId = UUID.randomUUID();
        ProcessInstance processInstance = mock(ProcessInstance.class);

        Relation relation1 = Relation.builder()
                .processInstance(processInstance)
                .systemId("test-system")
                .subjectType("SubjectType")
                .subjectId("subject-1")
                .objectType("ObjectType")
                .objectId("object-1")
                .predicateType("relates-to")
                .build();
        relation1.onPrePersist();

        Relation relation2 = Relation.builder()
                .processInstance(processInstance)
                .systemId("test-system")
                .subjectType("SubjectType")
                .subjectId("subject-2")
                .objectType("ObjectType")
                .objectId("object-2")
                .predicateType("belongs-to")
                .build();
        relation2.onPrePersist();

        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt"));
        Page<Relation> relationPage = new PageImpl<>(List.of(relation1, relation2), pageable, 2);
        when(relationRepository.findByProcessInstanceId(eq(processInstanceId), any(Pageable.class)))
                .thenReturn(relationPage);

        Page<RelationDTO> result = relationDTOFactory.createRelationDTOPage(processInstanceId, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getSubjectId()).isEqualTo("subject-1");
        assertThat(result.getContent().get(0).getPredicateType()).isEqualTo("relates-to");
        assertThat(result.getContent().get(1).getSubjectId()).isEqualTo("subject-2");
        assertThat(result.getContent().get(1).getPredicateType()).isEqualTo("belongs-to");
    }

    @Test
    void createRelationDTOPage_emptyPage_returnsEmptyPage() {
        UUID processInstanceId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        when(relationRepository.findByProcessInstanceId(eq(processInstanceId), any(Pageable.class)))
                .thenReturn(Page.empty(pageable));

        Page<RelationDTO> result = relationDTOFactory.createRelationDTOPage(processInstanceId, pageable);

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }
}
