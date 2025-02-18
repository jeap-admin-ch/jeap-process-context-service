package ch.admin.bit.jeap.processcontext.adapter.restapi.model;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessData;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Value
@AllArgsConstructor
@Builder(access = AccessLevel.PRIVATE)
public class ProcessDataDTO {

    @NotNull
    @Schema(
            description = "Key of the process data,",
            requiredMode = REQUIRED)
    String key;

    @NotNull
    @Schema(
            description = "Value of the process data.",
            requiredMode = REQUIRED)
    String value;

    @Schema(
            description = "Role of the process data.",
            requiredMode = REQUIRED)
    String role;

    static ProcessDataDTO create(ProcessData processData) {
        return ProcessDataDTO.builder()
                .key(processData.getKey())
                .value(processData.getValue())
                .role(processData.getRole())
                .build();
    }

    static ProcessDataDTO create(ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessData processData) {
        return ProcessDataDTO.builder()
                .key(processData.getKey())
                .value(processData.getValue())
                .role(processData.getRole())
                .build();
    }
}
