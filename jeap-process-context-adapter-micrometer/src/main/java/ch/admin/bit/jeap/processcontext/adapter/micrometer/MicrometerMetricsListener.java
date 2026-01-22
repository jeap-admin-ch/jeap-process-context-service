package ch.admin.bit.jeap.processcontext.adapter.micrometer;

import ch.admin.bit.jeap.messaging.avro.AvroMessageType;
import ch.admin.bit.jeap.messaging.model.MessageType;
import ch.admin.bit.jeap.processcontext.domain.port.MetricsListener;
import ch.admin.bit.jeap.processcontext.domain.processevent.EventType;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processupdate.ProcessUpdate;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class MicrometerMetricsListener implements MetricsListener {

    private static final String PCS_FAILED_PROCESS_UPDATES = "pcs_failed_process_updates";
    private static final String PCS_PROCESS_INSTANCE_CREATED = "pcs_process_instances_created";
    private static final String PCS_MESSAGES_RECEIVED = "pcs_messages_received";
    private static final String PCS_COMMAND_RECEIVED = "pcs_commands_received";
    private static final String PCS_PROCESS_UPDATE_PROCESSED = "pcs_process_updates_processed";
    private static final String PCS_PROCESS_EVENT_CREATED = "pcs_process_events_created";
    private static final String PCS_PROCESS_COMPLETED = "pcs_processes_completed";
    private static final String PCS_SNAPSHOT_CREATED = "pcs_snapshot_created";

    private final MeterRegistry meterRegistry;
    private Counter failedProcessUpdates;

    @PostConstruct
    void init() {
        failedProcessUpdates = Counter.builder(PCS_FAILED_PROCESS_UPDATES)
                .description("Failed process updates")
                .register(meterRegistry);
    }

    @Override
    public void processUpdateFailed(ProcessUpdate update, Exception ex) {
        failedProcessUpdates.increment();
    }

    @Override
    public void processInstanceCreated(String processTemplateName) {
        Counter.builder(PCS_PROCESS_INSTANCE_CREATED)
                .description("Created Process instances")
                .tag("process_template", processTemplateName)
                .register(meterRegistry);
    }

    @Override
    public void messageReceived(MessageType messageType, boolean firstProcessing) {
        Counter.builder(PCS_MESSAGES_RECEIVED)
                .description("Received messages")
                .tag("message_type", messageType.getName())
                .tag("first_processing", firstProcessing ? "true" : "false")
                .register(meterRegistry)
                .increment();
    }

    @Override
    public void commandReceived(AvroMessageType eventType) {
        Counter.builder(PCS_COMMAND_RECEIVED)
                .description("Received commands")
                .tag("event_type", eventType.getName())
                .register(meterRegistry)
                .increment();
    }

    @Override
    public void processUpdateProcessed(ProcessTemplate template, boolean successful, int count) {
        Counter.builder(PCS_PROCESS_UPDATE_PROCESSED)
                .description("Processed process updates")
                .tag("process_template", template.getName())
                .tag("successful", toString(successful))
                .register(meterRegistry)
                .increment(count);
    }

    @Override
    public void processEventCreated(ProcessTemplate template, EventType eventType) {
        Counter.builder(PCS_PROCESS_EVENT_CREATED)
                .description("Created process events")
                .tag("process_template", template.getName())
                .tag("event_type", eventType.name())
                .register(meterRegistry)
                .increment();
    }

    @Override
    public void processCompleted(ProcessTemplate template) {
        Counter.builder(PCS_PROCESS_COMPLETED)
                .description("Completed processes")
                .tag("process_template", template.getName())
                .register(meterRegistry)
                .increment();
    }

    @Override
    public void snapshotCreated(ProcessTemplate template) {
        Counter.builder(PCS_SNAPSHOT_CREATED)
                .description("Snapshot created")
                .tag("process_template", template.getName())
                .register(meterRegistry)
                .increment();
    }

    @Override
    public void timed(String name, Map<String, String> tags, Runnable runnable) {
        timer(name, tags).record(runnable);
    }

    private Timer timer(String name, Map<String, String> tags) {
        Timer.Builder builder = Timer.builder(name);
        tags.forEach(builder::tag);
        builder.publishPercentiles(0.5, 0.8, 0.95, 0.99);
        return builder.register(meterRegistry);
    }

    private String toString(boolean successful) {
        return successful ? "true" : "false";
    }
}
