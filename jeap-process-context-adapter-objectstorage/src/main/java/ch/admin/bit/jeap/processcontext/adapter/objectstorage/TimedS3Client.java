package ch.admin.bit.jeap.processcontext.adapter.objectstorage;

import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.signer.AwsS3V4Signer;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.ProxyConfiguration;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;

import java.net.URI;
import java.util.Map;

import static org.springframework.util.StringUtils.hasText;

@Slf4j
public class TimedS3Client {

    private final S3Client s3Client;

    public TimedS3Client(S3ObjectStorageConnectionProperties connectionProperties, AwsCredentialsProvider awsCredentialsProvider) {
        log.info("Initializing the S3 client with the connection properties {}.", connectionProperties);

        // if access credentials are configured explicitly, use a provider reflecting the given credentials instead of the given provider
        if (connectionProperties.accessCredentialsConfigured()) {
            log.info("Using a static credentials provider configured with the access credentials from the connection properties.");
            awsCredentialsProvider = StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(connectionProperties.getAccessKey(), connectionProperties.getSecretKey()));
        } else {
            log.info("Using the provided aws credentials provider.");
        }

        ClientOverrideConfiguration.Builder overrideConfig = ClientOverrideConfiguration.builder();
        overrideConfig.advancedOptions(Map.of(SdkAdvancedClientOption.SIGNER, AwsS3V4Signer.create()));

        S3ClientBuilder s3ClientBuilder = S3Client.builder()
                .region(connectionProperties.getRegion())
                .forcePathStyle(true)
                .credentialsProvider(awsCredentialsProvider)
                .httpClient(UrlConnectionHttpClient.builder()
                        .proxyConfiguration(ProxyConfiguration.builder() // Configure proxy to work around the issue https://github.com/aws/aws-sdk-java-v2/issues/4728 which is coming with the aws sdk update
                                .useSystemPropertyValues(false)
                                .useEnvironmentVariablesValues(false)
                                .build())
                        .build())
                .overrideConfiguration(overrideConfig.build());
        if (hasText(connectionProperties.getAccessUrl())) {
            log.info("Overriding S3 API endpoint URL in S3 client with {}.", connectionProperties.getAccessUrl());
            s3ClientBuilder.endpointOverride(createEndpointURI(connectionProperties.getAccessUrl()));
        }
        s3Client = s3ClientBuilder.build();

        log.info("S3Client initialized successfully.");
    }

    @Timed(value = "jeap_pcss_s3_client_put_object", description = "Put object to object store", percentiles = {0.5, 0.8, 0.95, 0.99})
    public PutObjectResponse putObject(PutObjectRequest request, RequestBody requestBody) {
        return s3Client.putObject(request, requestBody);
    }

    @Timed(value = "jeap_pcs_s3_client_get_object", description = "Get object from object store", percentiles = {0.5, 0.80, 0.95, 0.99})
    public ResponseBytes<GetObjectResponse> getObjectAsBytes(GetObjectRequest getObjectRequest) {
        return s3Client.getObjectAsBytes(getObjectRequest);
    }

    @Timed(value = "jeap_pcs_s3_client_head_bucket", description = "Head bucket", percentiles = {0.5, 0.80, 0.95, 0.99})
    public HeadBucketResponse headBucket(HeadBucketRequest headBucketRequest) {
        return s3Client.headBucket(headBucketRequest);
    }


    @Timed(value = "jeap_pcs_s3_client_get_bucketlifecycleconfiguration", description = "Get  bucket lifecycle configuration", percentiles = {0.5, 0.80, 0.95, 0.99})
    public GetBucketLifecycleConfigurationResponse getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest getBucketLifecycleConfigurationRequest) {
        return s3Client.getBucketLifecycleConfiguration(getBucketLifecycleConfigurationRequest);
    }

    @Timed(value = "jeap_pcs_s3_client_put_bucketlifecycleconfiguration", description = "Put  bucket lifecycle configuration", percentiles = {0.5, 0.80, 0.95, 0.99})
    public PutBucketLifecycleConfigurationResponse putBucketLifecycleConfiguration(PutBucketLifecycleConfigurationRequest putBucketLifecycleConfigurationRequest) {
        return s3Client.putBucketLifecycleConfiguration(putBucketLifecycleConfigurationRequest);
    }

    //
    // Only for tests
    //

    public void createBucket(CreateBucketRequest cbr) {
        s3Client.createBucket(cbr);
    }

    public GetObjectTaggingResponse getObjectTagging(GetObjectTaggingRequest getObjectTaggingRequest) {
        return s3Client.getObjectTagging(getObjectTaggingRequest);
    }

    private URI createEndpointURI(String accessUrl) {
        if (accessUrl.startsWith("http://") || accessUrl.startsWith("https://")) {
            return URI.create(accessUrl);
        } else {
            return URI.create("https://" + accessUrl);
        }
    }

    @Timed(value = "jeap_pcs_s3_client_list_objects", description = "List objects in prefix", percentiles = {0.5, 0.80, 0.95, 0.99})
    public ListObjectsV2Response listObjects(ListObjectsV2Request request) {
        return s3Client.listObjectsV2(request);
    }
}
