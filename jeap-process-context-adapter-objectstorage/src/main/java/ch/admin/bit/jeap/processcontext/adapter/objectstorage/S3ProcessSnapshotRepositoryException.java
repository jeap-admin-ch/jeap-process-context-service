package ch.admin.bit.jeap.processcontext.adapter.objectstorage;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessSnapshotArchiveData;

public class S3ProcessSnapshotRepositoryException extends RuntimeException {

    private S3ProcessSnapshotRepositoryException(String message, Throwable t) {
        super(message, t);
    }

    static S3ProcessSnapshotRepositoryException serializationException(ProcessSnapshotArchiveData processSnapshotArchiveData, Throwable t) {
        return new S3ProcessSnapshotRepositoryException(
                "Unable to serialize the process snapshot with origin process id '%s' and snapshot version %s.".
                        formatted(processSnapshotArchiveData.getProcessSnapshot().getOriginProcessId(),
                                processSnapshotArchiveData.getMetadata().getSnapshotVersion()), t);
    }

    static S3ProcessSnapshotRepositoryException s3MetadataReadException(String processOriginId, int snapshotVersion, Throwable t) {
        return new S3ProcessSnapshotRepositoryException(
                "Unable to parse the required metadata from the S3 object for the process snapshot with origin process id '%s' and snapshot version %s.".
                        formatted(processOriginId, snapshotVersion), t);
    }

}
