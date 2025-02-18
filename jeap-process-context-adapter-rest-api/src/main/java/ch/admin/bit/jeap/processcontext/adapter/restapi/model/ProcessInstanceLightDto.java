package ch.admin.bit.jeap.processcontext.adapter.restapi.model;

import ch.admin.bit.jeap.processcontext.domain.TranslateService;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
public class ProcessInstanceLightDto {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    String originProcessId;
    String createdAt;
    String state;
    String lastMessageCreatedAt;
    Map<String, String> name;

    public static ProcessInstanceLightDto create(ProcessInstance processInstance, TranslateService translateService) {
        Map<String, String> name = new HashMap<>();
        if (processInstance.getProcessTemplate() != null) {
            name = translateService.translateProcessTemplateName(processInstance.getProcessTemplate().getName());
        }

        return ProcessInstanceLightDto.builder()
                .originProcessId(processInstance.getOriginProcessId())
                .name(name)
                .state(processInstance.getState().name())
                .createdAt(formatZoneDateTime(processInstance.getCreatedAt()))
                .lastMessageCreatedAt(formatZoneDateTime(processInstance.getLastMessageDateTime().orElse(null)))
                .build();
    }

    static String formatZoneDateTime(ZonedDateTime zonedDateTime) {
        if (zonedDateTime != null) {
            return zonedDateTime.format(DATE_TIME_FORMATTER);
        }
        return "";
    }
}
