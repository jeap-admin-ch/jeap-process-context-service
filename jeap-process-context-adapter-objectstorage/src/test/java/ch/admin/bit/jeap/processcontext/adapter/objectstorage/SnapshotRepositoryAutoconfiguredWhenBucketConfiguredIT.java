package ch.admin.bit.jeap.processcontext.adapter.objectstorage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "jeap.processcontext.objectstorage.snapshot-bucket=test-bucket",
        "jeap.processcontext.objectstorage.snapshot-retention-days=1"})
@ContextConfiguration(classes = {ObjectStorageConfiguration.class})
class SnapshotRepositoryAutoconfiguredWhenBucketConfiguredIT {

    @Autowired
    private S3ProcessSnapshotRepository s3ProcessSnapshotRepository;

    @SuppressWarnings("unused")
    @MockitoBean
    private S3ObjectStorageRepository s3ObjectStorageRepository;

    @Test
    void testS3ProcessSnapshotRepositoryProvided() {
        assertThat(s3ProcessSnapshotRepository).isNotNull();
    }

}
