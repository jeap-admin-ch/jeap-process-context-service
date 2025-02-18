package ch.admin.bit.jeap.processcontext.plugin.api.context;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class TaskType {

    @NonNull
    String name;

    @NonNull
    TaskLifecycle lifecycle;

    @NonNull
    TaskCardinality cardinality;

}
