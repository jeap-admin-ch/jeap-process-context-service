package ch.admin.bit.jeap.processcontext.adapter.objectstorage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@Data
@ConfigurationProperties("jeap.processcontext.objectstorage")
public class S3ObjectStorageProperties {
    String snapshotBucket;
    int snapshotRetentionDays;
    @NestedConfigurationProperty
    S3ObjectStorageConnectionProperties connection = new S3ObjectStorageConnectionProperties();
}
