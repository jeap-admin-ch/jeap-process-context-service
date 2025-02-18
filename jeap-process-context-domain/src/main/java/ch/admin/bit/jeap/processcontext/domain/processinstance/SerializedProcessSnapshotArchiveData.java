package ch.admin.bit.jeap.processcontext.domain.processinstance;

import lombok.Value;

import java.util.Objects;

@Value
public class SerializedProcessSnapshotArchiveData {

    byte[] serializedProcessSnapshot;
    ProcessSnapshotMetadata metadata;

    public SerializedProcessSnapshotArchiveData(byte[] serializedProcessSnapshot, ProcessSnapshotMetadata metadata) {
        this.serializedProcessSnapshot = Objects.requireNonNull(serializedProcessSnapshot, "serializedProcessSnapshot must be provided");
        this.metadata = Objects.requireNonNull(metadata, "metadata must be provided");
    }

}
