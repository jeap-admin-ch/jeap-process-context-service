package ch.admin.bit.jeap.processcontext.domain.processinstance.event;

import ch.admin.bit.jeap.processcontext.domain.port.MetricsListener;
import ch.admin.bit.jeap.processcontext.domain.port.ProcessInstanceEventProducer;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessSnapshotArchiveData;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProcessSnapshotEventProducer {

    private final MetricsListener metricsListener;
    private final ProcessInstanceEventProducer processInstanceEventProducer;

    public void onSnapshotCreated(ProcessSnapshotArchiveData processSnapshotArchiveData, ProcessTemplate processTemplate) {
        metricsListener.timed("jeap_pcs_produce_snapshot_events", Map.of(), () ->
                produceSnapshotNotifications(processSnapshotArchiveData, processTemplate));

    }

    private void produceSnapshotNotifications(ProcessSnapshotArchiveData processSnapshotArchiveData, ProcessTemplate processTemplate) {
        int version = processSnapshotArchiveData.getMetadata().getSnapshotVersion();
        String originProcessId = processSnapshotArchiveData.getProcessSnapshot().getOriginProcessId();
        log.info("Producing snapshot created event for snapshot version {} in process {}", version, originProcessId);
        processInstanceEventProducer.produceProcessSnapshotCreatedEventSynchronously(originProcessId, version);
        metricsListener.snapshotCreated(processTemplate);
    }
}
