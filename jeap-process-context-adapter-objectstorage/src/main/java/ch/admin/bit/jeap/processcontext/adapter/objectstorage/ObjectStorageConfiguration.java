package ch.admin.bit.jeap.processcontext.adapter.objectstorage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

@Slf4j
@AutoConfiguration
@EnableConfigurationProperties({S3ObjectStorageProperties.class})
@ConditionalOnProperty(prefix = "jeap.processcontext.objectstorage", name = "snapshot-bucket")
public class ObjectStorageConfiguration {

    @Bean
    @ConditionalOnMissingBean(AwsCredentialsProvider.class)
    public DefaultCredentialsProvider awsCredentialsProvider() {
        log.info("Creating AWS DefaultCredentialsProvider.");
        return DefaultCredentialsProvider.create();
    }

    @Bean
    public TimedS3Client timedS3Client(S3ObjectStorageProperties s3ObjectStorageProperties, AwsCredentialsProvider awsCredentialsProvider) {
        return new TimedS3Client(s3ObjectStorageProperties.connection, awsCredentialsProvider);
    }

    @Bean
    public S3ObjectStorageRepository s3ObjectStorageRepository(TimedS3Client timedS3Client) {
        return new S3ObjectStorageRepository(timedS3Client);
    }

    @Bean
    public S3ProcessSnapshotRepository s3ProcessSnapshotRepository(S3ObjectStorageRepository s3ObjectStorageRepository, S3ObjectStorageProperties s3ObjectStorageProperties) {
        return new S3ProcessSnapshotRepository(s3ObjectStorageRepository, s3ObjectStorageProperties);
    }

}