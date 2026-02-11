package ch.admin.bit.jeap.processcontext.adapter.test.kafka.config;

import ch.admin.bit.jeap.messaging.model.MessageType;
import ch.admin.bit.jeap.processcontext.domain.port.MetricsListener;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.Map;
import java.util.function.Supplier;


public class StubMetricsListener implements MetricsListener {

    @Override
    public void processUpdateFailed() {
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
    public void processUpdateProcessed(ProcessTemplate template) {
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

    @Override
    public <T> T timedWithReturnValue(String name, Supplier<T> supplier) {
        return Timer.builder(name).register(new SimpleMeterRegistry())
                .record(supplier);
    }
}
