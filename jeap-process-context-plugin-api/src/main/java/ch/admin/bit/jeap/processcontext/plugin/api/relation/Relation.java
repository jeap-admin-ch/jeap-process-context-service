package ch.admin.bit.jeap.processcontext.plugin.api.relation;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.UUID;

@Value
@Builder
public class Relation {
    @NonNull
    String originProcessId;
    @NonNull
    String subjectType;
    @NonNull
    String subjectId;
    @NonNull
    String systemId;
    @NonNull
    String objectType;
    @NonNull
    String objectId;
    @NonNull
    String predicateType;
    @NonNull
    @EqualsAndHashCode.Exclude
    ZonedDateTime createdAt;
    @NonNull
    @EqualsAndHashCode.Exclude
    UUID idempotenceId;
}
