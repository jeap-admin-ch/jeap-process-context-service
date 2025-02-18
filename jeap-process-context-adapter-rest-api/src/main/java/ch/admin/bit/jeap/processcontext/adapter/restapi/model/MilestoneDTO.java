package ch.admin.bit.jeap.processcontext.adapter.restapi.model;

import ch.admin.bit.jeap.processcontext.domain.TranslateService;
import ch.admin.bit.jeap.processcontext.domain.processinstance.Milestone;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.Map;

@Value
@AllArgsConstructor
@Builder(access = AccessLevel.PRIVATE)
public class MilestoneDTO {

    String name;
    Map<String, String> labels;
    String state;
    ZonedDateTime reachedAt;

    static MilestoneDTO create(Milestone milestone, String processTemplate, TranslateService translateService) {
        return MilestoneDTO.builder()
                .name(milestone.getName())
                .labels(translateService.translateMilestoneName(processTemplate, milestone.getName()))
                .state(milestone.getState().name())
                .reachedAt(milestone.getReachedAt())
                .build();
    }
}
