package ch.admin.bit.jeap.processcontext.adapter.restapi;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReevaluationJobRequest(
        @NotBlank String processTemplateName,
        @NotEmpty List<@NotNull @Valid ProcessRequest> processes) {

    public record ProcessRequest(@NotBlank String originProcessId) {
    }
}
