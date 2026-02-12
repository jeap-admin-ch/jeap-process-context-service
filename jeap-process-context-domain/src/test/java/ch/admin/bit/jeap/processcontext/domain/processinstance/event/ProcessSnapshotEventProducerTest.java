package ch.admin.bit.jeap.processcontext.domain.processinstance.event;

import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessSnapshot;
import ch.admin.bit.jeap.processcontext.domain.port.MetricsListener;
import ch.admin.bit.jeap.processcontext.domain.port.ProcessInstanceEventProducer;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessSnapshotArchiveData;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessSnapshotMetadata;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessSnapshotEventProducerTest {

    @Mock
    private MetricsListener metricsListener;

    @Mock
    private ProcessInstanceEventProducer processInstanceEventProducer;

    @Mock
    private ProcessTemplate processTemplate;

    private ProcessSnapshotEventProducer target;

    @BeforeEach
    void setUp() {
        target = new ProcessSnapshotEventProducer(metricsListener, processInstanceEventProducer);
    }

    @Test
    void onSnapshotCreated_shouldProduceSnapshotEvent() {
        String originProcessId = "test-origin-process-id";
        int snapshotVersion = 3;

        ProcessSnapshot processSnapshot = mock(ProcessSnapshot.class);
        when(processSnapshot.getOriginProcessId()).thenReturn(originProcessId);

        ProcessSnapshotMetadata metadata = ProcessSnapshotMetadata.builder()
                .snapshotVersion(snapshotVersion)
                .schemaName("ProcessSnapshot")
                .schemaVersion(2)
                .retentionPeriodMonths(12)
                .systemName("JEAP")
                .build();

        ProcessSnapshotArchiveData processSnapshotArchiveData = mock(ProcessSnapshotArchiveData.class);
        when(processSnapshotArchiveData.getMetadata()).thenReturn(metadata);
        when(processSnapshotArchiveData.getProcessSnapshot()).thenReturn(processSnapshot);

        target.onSnapshotCreated(processSnapshotArchiveData, processTemplate);

        verify(processInstanceEventProducer).produceProcessSnapshotCreatedEventSynchronously(originProcessId, snapshotVersion);
        verify(metricsListener).snapshotCreated(processTemplate);
    }
}
