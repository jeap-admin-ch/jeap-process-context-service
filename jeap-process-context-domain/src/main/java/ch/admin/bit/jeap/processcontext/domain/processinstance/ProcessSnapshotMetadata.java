package ch.admin.bit.jeap.processcontext.domain.processinstance;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class ProcessSnapshotMetadata {
    @NonNull
    Integer snapshotVersion;
    @NonNull
    String schemaName;
    int schemaVersion;
    @NonNull
    Integer retentionPeriodMonths;
    @NonNull
    String systemName;
}
