package ch.admin.bit.jeap.processcontext.adapter.restapi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Data
public class NewProcessInstanceDTO {

    @NotNull
    @Schema(
            description = "Name of the process template to use when instantiating the process",
            requiredMode = REQUIRED,
            example = "order-process-template")
    private String processTemplateName;

    @Valid
    @Schema(description = "A set of external references linking this process instance to external things.")
    private Set<ExternalReferenceDTO> externalReferences;

}
