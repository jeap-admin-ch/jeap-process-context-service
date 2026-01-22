package ch.admin.bit.jeap.processcontext.adapter.test.kafka.config;

import ch.admin.bit.jeap.messaging.avro.AvroMessageType;
import ch.admin.bit.jeap.messaging.model.MessageType;
import ch.admin.bit.jeap.processcontext.domain.port.MetricsListener;
import ch.admin.bit.jeap.processcontext.domain.processevent.EventType;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processupdate.ProcessUpdate;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.Map;


public class StubMetricsListener implements MetricsListener {

    @Override
    public void processUpdateFailed(ProcessUpdate update, Exception ex) {
        // stub method
    }

    @Override
    public void processInstanceCreated(String processTemplateName) {
        // stub method
    }

    @Override
    public void messageReceived(MessageType messageType, boolean firstProcessing) {
        // stub method
    }

    @Override
    public void commandReceived(AvroMessageType eventType) {
        // stub method
    }

    @Override
    public void processUpdateProcessed(ProcessTemplate template, boolean successful, int count) {
        // stub method
    }

    @Override
    public void processEventCreated(ProcessTemplate template, EventType eventType) {
        // stub method
    }

    @Override
    public void processCompleted(ProcessTemplate template) {
        // stub method
    }

    @Override
    public void snapshotCreated(ProcessTemplate template) {
        // stub method
    }

    @Override
    public void timed(String name, Map<String, String> tags, Runnable runnable) {
        Timer.builder(name)
                .register(new SimpleMeterRegistry())
                .record(runnable);
    }
}
