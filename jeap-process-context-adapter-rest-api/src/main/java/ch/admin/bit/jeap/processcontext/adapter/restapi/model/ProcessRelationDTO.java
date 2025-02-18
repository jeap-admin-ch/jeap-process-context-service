package ch.admin.bit.jeap.processcontext.adapter.restapi.model;

import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessRelation;
import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessRelationRole;
import ch.admin.bit.jeap.processcontext.domain.TranslateService;
import ch.admin.bit.jeap.processcontext.domain.processrelation.ProcessRelationView;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@AllArgsConstructor
@Builder(access = AccessLevel.PUBLIC)
public class ProcessRelationDTO {

    Map<String, String> relation;
    Map<String, String> processName;
    String processId;
    String processState;

    public static ProcessRelationDTO fromView(ProcessRelationView view) {
        return ProcessRelationDTO.builder()
                .relation(view.getRelation())
                .processName(view.getProcessName())
                .processId(view.getProcessId())
                .processState(view.getProcessState())
                .build();
    }

    public static ProcessRelationDTO fromSnapshot(ProcessRelation relation, TranslateService translateService) {
        Map<String, String> relationNameByLanguage = translateRelationName(relation, translateService);
        return ProcessRelationDTO.builder()
                .relation(relationNameByLanguage)
                .processName(translateService.translateProcessTemplateName(relation.getProcessName(), relation.getProcessLabel()))
                .processId(relation.getOriginProcessId())
                .processState(relation.getProcessState())
                .build();
    }

    private static Map<String, String> translateRelationName(ProcessRelation relation, TranslateService translateService) {
        String processTemplateName = relation.getProcessName();
        if (relation.getRelationRole() == ProcessRelationRole.ORIGIN) {
            return translateService.translateProcessRelationOriginRole(processTemplateName, relation.getRelationName());
        } else {
            return translateService.translateProcessRelationTargetRole(processTemplateName, relation.getRelationName());
        }
    }
}
