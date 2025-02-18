package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessSnapshot;

import java.util.Optional;

public interface ProcessSnapshotRepository {

    void storeSnapshot(ProcessSnapshotArchiveData processSnapshotArchiveData);

    /**
     * @param processOriginId Origin Process ID of the snapshot process instance
     * @param snapshotVersion Version of the snapshot (1..n) or null. If null, the newest snapshot version is loaded.
     * @return Optional with snapshot avro archive data, empty if not found
     */
    Optional<SerializedProcessSnapshotArchiveData> loadSnapshot(String processOriginId, Integer snapshotVersion);

    Optional<ProcessSnapshot> loadAndDeserializeNewestSnapshot(String processOriginId);
}
