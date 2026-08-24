package ch.admin.bit.jeap.processcontext.adapter.restapi;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BackfillJobRequest(
        @JsonProperty("process-template-name") @JsonAlias("processTemplateName") @NotBlank String processTemplateName,
        @NotEmpty List<@NotNull @Valid EntryRequest> entries) {

    public record EntryRequest(
            @JsonProperty("origin-process-id") @JsonAlias("originProcessId") @NotBlank String originProcessId,
            @JsonProperty("process-data") @JsonAlias("processData")
            @NotEmpty List<@NotNull @Valid ProcessDataRequest> processData) {
    }

    public record ProcessDataRequest(
            @NotBlank String key,
            @NotBlank String value,
            String role) {
    }
}
