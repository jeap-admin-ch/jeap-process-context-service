package ch.admin.bit.jeap.processcontext.adapter.restapi.model;

import lombok.Builder;
import lombok.NonNull;

import java.util.Map;

@Builder
public record MessageUserDataDTO(@NonNull String key, @NonNull String value, @NonNull Map<String, String> label) {
}
