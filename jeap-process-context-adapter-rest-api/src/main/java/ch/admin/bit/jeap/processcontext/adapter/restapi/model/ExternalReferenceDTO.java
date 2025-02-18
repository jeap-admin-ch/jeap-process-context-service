package ch.admin.bit.jeap.processcontext.adapter.restapi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@AllArgsConstructor
@Builder(access = AccessLevel.PRIVATE)
public class ExternalReferenceDTO {

    @NotNull
    @Schema(
            description = "Name of the external reference.",
            required = true,
            example = "MRN")
    String name;

    @NotNull
    @Schema(
            description = "Value of the external reference.",
            required = true,
            example = "07DE33021234567890")
    String value;

}
