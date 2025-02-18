package ch.admin.bit.jeap.processcontext.adapter.restapi.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class TaskDataDTO {
    @NonNull
    String key;
    @NonNull
    String value;
    @NonNull
    @EqualsAndHashCode.Exclude
    Map<String, String> labels;
}
