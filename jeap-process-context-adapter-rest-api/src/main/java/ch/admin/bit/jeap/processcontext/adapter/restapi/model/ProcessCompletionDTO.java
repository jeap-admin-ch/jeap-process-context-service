package ch.admin.bit.jeap.processcontext.adapter.restapi.model;

import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessSnapshot;
import ch.admin.bit.jeap.processcontext.domain.TranslateService;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessCompletion;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

import static ch.admin.bit.jeap.processcontext.adapter.restapi.model.ProcessInstanceDTOFactory.toZonedDateTime;

@Data
@AllArgsConstructor
@Builder(access = AccessLevel.PACKAGE)
@Slf4j
public class ProcessCompletionDTO {

    String conclusion;
    ZonedDateTime completedAt;
    Map<String, String> reason;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static ProcessCompletionDTO create(Optional<ProcessCompletion> processCompletion, String processTemplate, TranslateService translateService) {
        return processCompletion.map(c -> ProcessCompletionDTO.builder()
                        .conclusion(c.getConclusion().name())
                        .completedAt(c.getCompletedAt())
                        .reason(translateService.translateProcessCompletionName(processTemplate, c.getName()))
                        .build())
                .orElse(null);
    }

    static ProcessCompletionDTO create(ProcessSnapshot snap, TranslateService translateService) {
        return ProcessCompletionDTO.builder()
                .conclusion(snap.getCompletionConclusion())
                .completedAt(toZonedDateTime(snap.getOptionalDateTimeCompleted()))
                .reason(translateService.translateProcessCompletionNameFromSnapshot(snap.getTemplateName(), snap.getCompletionName(), snap.getCompletionReasonLabel()))
                .build();
    }
}
