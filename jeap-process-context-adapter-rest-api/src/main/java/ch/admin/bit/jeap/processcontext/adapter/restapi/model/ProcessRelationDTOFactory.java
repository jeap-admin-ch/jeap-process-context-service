package ch.admin.bit.jeap.processcontext.adapter.restapi.model;

import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessRelation;
import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessSnapshot;
import ch.admin.bit.jeap.processcontext.domain.TranslateService;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processrelation.ProcessRelationsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProcessRelationDTOFactory {

    private final ProcessRelationsService processRelationsService;
    private final TranslateService translateService;

    public Page<ProcessRelationDTO> createProcessRelationDTOPage(ProcessInstance processInstance, Pageable pageable) {
        return processRelationsService.createProcessRelationsPaged(processInstance, pageable)
                .map(ProcessRelationDTO::fromView);
    }

    public Page<ProcessRelationDTO> createProcessRelationDTOPageFromSnapshot(ProcessSnapshot snapshot, Pageable pageable) {
        List<ProcessRelation> processRelations = snapshot.getProcessRelations();
        if (processRelations == null) {
            return Page.empty(pageable);
        }
        return PageUtils.toPage(createProcessRelations(processRelations), pageable);
    }

    private List<ProcessRelationDTO> createProcessRelations(List<ProcessRelation> processRelations) {
        return processRelations.stream()
                .map((ProcessRelation relation) -> ProcessRelationDTO.fromSnapshot(relation, translateService))
                .toList();
    }
}
