package ch.admin.bit.jeap.processcontext.adapter.restapi.model;

import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessRelation;
import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessRelationRole;
import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessSnapshot;
import ch.admin.bit.jeap.processcontext.domain.TranslateService;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processrelation.ProcessRelationView;
import ch.admin.bit.jeap.processcontext.domain.processrelation.ProcessRelationsService;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessRelationRoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessRelationDTOFactoryTest {

    private ProcessRelationsService processRelationsService;
    private TranslateService translateService;
    private ProcessRelationDTOFactory factory;

    @BeforeEach
    void setUp() {
        processRelationsService = mock(ProcessRelationsService.class);
        translateService = mock(TranslateService.class);
        factory = new ProcessRelationDTOFactory(processRelationsService, translateService);
    }

    @Test
    void createProcessRelationDTOPage_returnsPageOfDTOs() {
        ProcessInstance processInstance = mock(ProcessInstance.class);
        Pageable pageable = PageRequest.of(0, 10);

        ProcessRelationView view1 = ProcessRelationView.builder()
                .relationName("testRelation")
                .originRole("origin")
                .targetRole("target")
                .relationRole(ProcessRelationRoleType.ORIGIN)
                .relation(Map.of("de", "Test"))
                .processTemplateName("template")
                .processName(Map.of("de", "Process"))
                .processId("related-1")
                .processState("IN_PROGRESS")
                .build();
        ProcessRelationView view2 = ProcessRelationView.builder()
                .relationName("externalRelation")
                .originRole("origin")
                .targetRole("target")
                .relationRole(ProcessRelationRoleType.TARGET)
                .relation(Map.of("de", "Extern"))
                .processTemplateName("template2")
                .processName(Map.of("de", "External Process"))
                .processId("related-2")
                .processState("COMPLETED")
                .build();
        when(processRelationsService.createProcessRelationsPaged(processInstance, pageable))
                .thenReturn(new PageImpl<>(List.of(view1, view2), pageable, 2));

        Page<ProcessRelationDTO> result = factory.createProcessRelationDTOPage(processInstance, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        ProcessRelationDTO dto1 = result.getContent().getFirst();
        assertThat(dto1.getRelation()).containsEntry("de", "Test");
        assertThat(dto1.getProcessId()).isEqualTo("related-1");
        ProcessRelationDTO dto2 = result.getContent().get(1);
        assertThat(dto2.getRelation()).containsEntry("de", "Extern");
        assertThat(dto2.getProcessId()).isEqualTo("related-2");
    }

    @Test
    void createProcessRelationDTOPage_paginatesCorrectly() {
        ProcessInstance processInstance = mock(ProcessInstance.class);
        Pageable pageable = PageRequest.of(1, 1); // second page, page size 1

        ProcessRelationView view2 = ProcessRelationView.builder()
                .relationName("relation2")
                .originRole("origin")
                .targetRole("target")
                .relationRole(ProcessRelationRoleType.TARGET)
                .relation(Map.of("de", "Second"))
                .processTemplateName("template2")
                .processName(Map.of("de", "Process 2"))
                .processId("related-2")
                .processState("COMPLETED")
                .build();
        when(processRelationsService.createProcessRelationsPaged(processInstance, pageable))
                .thenReturn(new PageImpl<>(List.of(view2), pageable, 2));

        Page<ProcessRelationDTO> result = factory.createProcessRelationDTOPage(processInstance, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getProcessId()).isEqualTo("related-2");
        assertThat(result.getNumber()).isEqualTo(1);
    }

    @Test
    void createProcessRelationDTOPage_noRelations_returnsEmptyPage() {
        ProcessInstance processInstance = mock(ProcessInstance.class);
        Pageable pageable = PageRequest.of(0, 10);

        when(processRelationsService.createProcessRelationsPaged(processInstance, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<ProcessRelationDTO> result = factory.createProcessRelationDTOPage(processInstance, pageable);

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void createProcessRelationDTOPageFromSnapshot_withRelations_returnsPageOfDTOs() {
        ProcessRelation originRelation = new ProcessRelation(
                ProcessRelationRole.ORIGIN, "rel1", "originRole", "targetRole",
                "proc-1", "template1", "Template 1 Label", "IN_PROGRESS");
        ProcessRelation targetRelation = new ProcessRelation(
                ProcessRelationRole.TARGET, "rel2", "originRole", "targetRole",
                "proc-2", "template2", "Template 2 Label", "COMPLETED");

        ProcessSnapshot snapshot = mock(ProcessSnapshot.class);
        when(snapshot.getProcessRelations()).thenReturn(List.of(originRelation, targetRelation));

        when(translateService.translateProcessRelationOriginRole("template1", "rel1"))
                .thenReturn(Map.of("de", "Origin Beziehung"));
        when(translateService.translateProcessRelationTargetRole("template2", "rel2"))
                .thenReturn(Map.of("de", "Target Beziehung"));
        when(translateService.translateProcessTemplateName("template1", "Template 1 Label"))
                .thenReturn(Map.of("de", "Vorlage 1"));
        when(translateService.translateProcessTemplateName("template2", "Template 2 Label"))
                .thenReturn(Map.of("de", "Vorlage 2"));

        Pageable pageable = PageRequest.of(0, 10);
        Page<ProcessRelationDTO> result = factory.createProcessRelationDTOPageFromSnapshot(snapshot, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        ProcessRelationDTO dto1 = result.getContent().getFirst();
        assertThat(dto1.getRelation()).containsEntry("de", "Origin Beziehung");
        assertThat(dto1.getProcessName()).containsEntry("de", "Vorlage 1");
        assertThat(dto1.getProcessId()).isEqualTo("proc-1");
        assertThat(dto1.getProcessState()).isEqualTo("IN_PROGRESS");
        ProcessRelationDTO dto2 = result.getContent().get(1);
        assertThat(dto2.getRelation()).containsEntry("de", "Target Beziehung");
        assertThat(dto2.getProcessName()).containsEntry("de", "Vorlage 2");
        assertThat(dto2.getProcessId()).isEqualTo("proc-2");
        assertThat(dto2.getProcessState()).isEqualTo("COMPLETED");
    }

    @Test
    void createProcessRelationDTOPageFromSnapshot_nullRelations_returnsEmptyPage() {
        ProcessSnapshot snapshot = mock(ProcessSnapshot.class);
        when(snapshot.getProcessRelations()).thenReturn(null);

        Pageable pageable = PageRequest.of(0, 10);
        Page<ProcessRelationDTO> result = factory.createProcessRelationDTOPageFromSnapshot(snapshot, pageable);

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void createProcessRelationDTOPageFromSnapshot_paginatesCorrectly() {
        ProcessRelation relation1 = new ProcessRelation(
                ProcessRelationRole.ORIGIN, "rel1", "o", "t", "proc-1", "tmpl", "Label", "IN_PROGRESS");
        ProcessRelation relation2 = new ProcessRelation(
                ProcessRelationRole.ORIGIN, "rel2", "o", "t", "proc-2", "tmpl", "Label", "COMPLETED");

        ProcessSnapshot snapshot = mock(ProcessSnapshot.class);
        when(snapshot.getProcessRelations()).thenReturn(List.of(relation1, relation2));

        when(translateService.translateProcessRelationOriginRole("tmpl", "rel1"))
                .thenReturn(Map.of("de", "Rel 1"));
        when(translateService.translateProcessRelationOriginRole("tmpl", "rel2"))
                .thenReturn(Map.of("de", "Rel 2"));
        when(translateService.translateProcessTemplateName("tmpl", "Label"))
                .thenReturn(Map.of("de", "Template"));

        Pageable pageable = PageRequest.of(0, 1);
        Page<ProcessRelationDTO> result = factory.createProcessRelationDTOPageFromSnapshot(snapshot, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getProcessId()).isEqualTo("proc-1");
    }
}
