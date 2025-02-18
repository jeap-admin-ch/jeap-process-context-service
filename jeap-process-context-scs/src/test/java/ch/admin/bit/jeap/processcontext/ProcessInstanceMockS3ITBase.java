package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.adapter.objectstorage.S3ObjectStorageRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public abstract class ProcessInstanceMockS3ITBase extends ProcessInstanceITBase {

    @SuppressWarnings("unused")
    @MockitoBean
    private S3ObjectStorageRepository s3ObjectStorageRepository;

}
