package ch.admin.bit.jeap.processcontext.adapter.objectstorage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(classes={ObjectStorageConfiguration.class})
class NoSnapshotRepositoryAutoconfiguredWhenBucketNotConfiguredIT {

    @Autowired
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<S3ProcessSnapshotRepository> s3ProcessSnapshotRepository;

    @Test
    void testS3ProcessSnapshotRepositoryNotProvided() {
        assertThat(s3ProcessSnapshotRepository).isEmpty();
    }

}
