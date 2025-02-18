package ch.admin.bit.jeap.processcontext.adapter.objectstorage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.utils.Md5Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class S3ObjectStorageRepository {

    private final TimedS3Client s3Client;

    public ResponseBytes<GetObjectResponse> getObjectAsBytes(String bucketName, String objectKey) {
        log.trace("Getting object with key '{}' from bucket '{}'.", objectKey, bucketName);
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        try {
            ResponseBytes<GetObjectResponse> getObjectResponseBytes = s3Client.getObjectAsBytes(getObjectRequest);
            GetObjectResponse getObjectResponse = getObjectResponseBytes.response();
            log.debug("Successfully got object with key '{}' from bucket '{}' with version id '{}'.",
                    objectKey, bucketName, getObjectResponse.versionId());
            return getObjectResponseBytes;
        } catch (NoSuchKeyException nske) {
            throw S3ObjectStorageRepositoryObjectNotFoundException.keyNotFoundInBucket(objectKey, bucketName, nske);
        } catch (S3Exception s3e) {
            if (s3e.statusCode() == HttpStatus.NOT_FOUND.value()) {
                throw S3ObjectStorageRepositoryObjectNotFoundException.objectNotFound(objectKey, bucketName, s3e);
            } else {
                throw s3e;
            }
        }
    }

    public String putObject(String bucketName, String objectKey, byte[] payload, String contentType, Map<String, String> metadata, Set<Tag> tags) {
        log.trace("Putting object with key '{}' and size '{}' into bucket '{}'.", objectKey, payload.length, bucketName);
        Map<String, String> objectMetadata = new HashMap<>();
        if (metadata != null) {
            objectMetadata.putAll(metadata);
        }
        PutObjectRequest.Builder putObjectRequestBuilder = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentLength((long) payload.length)
                .contentMD5(computeMD5HashBase64(payload))
                .contentType(contentType)
                .metadata(objectMetadata);
        if ((tags != null) && !tags.isEmpty()) {
            putObjectRequestBuilder.tagging(Tagging.builder().tagSet(tags).build());
        }
        PutObjectRequest putObjectRequest = putObjectRequestBuilder.build();

        PutObjectResponse putObjectResponse = s3Client.putObject(putObjectRequest, RequestBody.fromBytes(payload));

        log.debug("Successfully put object with key '{}' and size '{}' into bucket '{}' with payload having md5 hash '{}' and version id '{}'.",
                objectKey, payload.length, bucketName, putObjectRequest.contentMD5(), putObjectResponse.versionId());
        return putObjectResponse.versionId();
    }

    public List<LifecycleRule> getBucketLifecycleRules(String bucketName) {
        try {
            GetBucketLifecycleConfigurationResponse getBucketLifecycleConfigurationResponse =
                    s3Client.getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder()
                            .bucket(bucketName)
                            .build());
            return getBucketLifecycleConfigurationResponse.rules();
        } catch (S3Exception s3e) {
            if (s3e.statusCode() == HttpStatus.NOT_FOUND.value()) {
                return List.of();
            } else {
                log.error("Failed to get the bucket lifecycle configuration of the bucket '{}'.", bucketName, s3e);
                throw s3e;
            }
        }
    }

    public void setBucketLifecycleConfiguration(String bucketName, BucketLifecycleConfiguration bucketLifeCycleConfiguration) {
        var putBucketLifecycleConfigurationRequest = PutBucketLifecycleConfigurationRequest.builder()
                .bucket(bucketName)
                .lifecycleConfiguration(bucketLifeCycleConfiguration)
                .build();
        s3Client.putBucketLifecycleConfiguration(putBucketLifecycleConfigurationRequest);
    }

    public void checkAccessToBucket(String bucketName) {
        log.info("Verifying access to bucket '{}'.", bucketName);
        s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
        log.info("Bucket access verified.");
    }

    private static String computeMD5HashBase64(byte[] object) {
        return Md5Utils.md5AsBase64(object);
    }

    public List<String> listObjects(String bucketName, String prefix) {
        var request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .build();
        var response = s3Client.listObjects(request);
        return response.contents().stream()
                .map(s3Object -> withoutPrefix(prefix, s3Object.key()))
                .toList();
    }

    private String withoutPrefix(String prefix, String key) {
        return key.substring(prefix.length());
    }
}
