package ch.admin.bit.jeap.processcontext.plugin.api.context;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Value
public class Task {

    TaskType type;

    Optional<String> originTaskId;

    TaskState state;

    String id;

    @Builder
    public Task(@NonNull TaskType type, String originTaskId, @NonNull TaskState state, @NonNull String id) {
        this.originTaskId = Optional.ofNullable(originTaskId);
        this.state = state;
        this.type = type;
        this.id = id;
    }
}
