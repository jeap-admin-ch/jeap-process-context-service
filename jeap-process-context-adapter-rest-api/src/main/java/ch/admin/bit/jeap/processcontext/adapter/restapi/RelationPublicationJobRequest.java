package ch.admin.bit.jeap.processcontext.adapter.restapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record RelationPublicationJobRequest(
        @JsonProperty("relationIds") @NotEmpty List<@NotNull UUID> relationIds) {
}
