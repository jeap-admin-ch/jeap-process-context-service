package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessSnapshot;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProcessSnapshotArchiveData {

    private static final String SNAPSHOT_SCHEMA_NAME = "ProcessSnapshot";
    private static final int SNAPSHOT_SCHEMA_VERSION = 2;
    private static final String SNAPSHOT_SYSTEM_NAME = "JEAP";

    @NonNull
    ProcessSnapshot processSnapshot;
    @NonNull
    ProcessSnapshotMetadata metadata;

    public static ProcessSnapshotArchiveData from(ProcessSnapshot processSnapshot, int snapshotVersion, int retentionPeriodMonths) {
        return new ProcessSnapshotArchiveData(processSnapshot, ProcessSnapshotMetadata.builder().
                        snapshotVersion(snapshotVersion).
                        schemaName(SNAPSHOT_SCHEMA_NAME).
                        schemaVersion(SNAPSHOT_SCHEMA_VERSION).
                        systemName(SNAPSHOT_SYSTEM_NAME).
                        retentionPeriodMonths(retentionPeriodMonths).
                        build());
    }

}
