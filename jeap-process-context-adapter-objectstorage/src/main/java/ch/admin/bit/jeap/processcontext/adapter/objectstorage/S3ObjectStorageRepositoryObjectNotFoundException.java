package ch.admin.bit.jeap.processcontext.adapter.objectstorage;

public class S3ObjectStorageRepositoryObjectNotFoundException extends RuntimeException {

    private S3ObjectStorageRepositoryObjectNotFoundException(String message, Throwable t) {
        super(message, t);
    }

    static S3ObjectStorageRepositoryObjectNotFoundException keyNotFoundInBucket(String key, String bucket, Throwable t) {
        return new S3ObjectStorageRepositoryObjectNotFoundException(
                "There is no key '%s' in bucket '%s'.".formatted(key, bucket), t);
    }

    static S3ObjectStorageRepositoryObjectNotFoundException objectNotFound(String key, String bucket, Throwable t) {
        return new S3ObjectStorageRepositoryObjectNotFoundException(
                "S3 object not found for key '%s' and bucket '%s'. Maybe the bucket does not exist?".formatted(key, bucket), t);
    }

}
