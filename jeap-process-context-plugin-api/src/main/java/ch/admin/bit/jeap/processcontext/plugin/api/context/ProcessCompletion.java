package ch.admin.bit.jeap.processcontext.plugin.api.context;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@Builder
public class ProcessCompletion {

    @NonNull
    ProcessCompletionConclusion conclusion;

    @NonNull
    String name;

    @NonNull
    ZonedDateTime completedAt;

}
