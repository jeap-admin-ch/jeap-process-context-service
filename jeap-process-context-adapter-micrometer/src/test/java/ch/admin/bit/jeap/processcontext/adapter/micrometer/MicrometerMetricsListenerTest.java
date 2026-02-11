package ch.admin.bit.jeap.processcontext.adapter.micrometer;

import ch.admin.bit.jeap.messaging.avro.AvroMessageType;
import ch.admin.bit.jeap.messaging.model.MessageType;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskCardinality;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskLifecycle;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MicrometerMetricsListenerTest {

    private static final String PROCESS_TEMPLATE_NAME = "test-process-template";
    private static final String MESSAGE_TYPE_NAME = "TestMessageType";
    private static final String EVENT_TYPE_NAME = "TestEventType";

    private MeterRegistry meterRegistry;
    private MicrometerMetricsListener metricsListener;

    @Mock
    private MessageType messageType;

    @Mock
    private AvroMessageType avroMessageType;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsListener = new MicrometerMetricsListener(meterRegistry);
        metricsListener.init();
    }

    @Test
    void processUpdateFailed_shouldIncrementCounter() {
        metricsListener.processUpdateFailed();

        Counter counter = meterRegistry.find("pcs_failed_process_updates").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void processUpdateFailed_calledMultipleTimes_shouldIncrementCounter() {
        metricsListener.processUpdateFailed();
        metricsListener.processUpdateFailed();
        metricsListener.processUpdateFailed();

        Counter counter = meterRegistry.find("pcs_failed_process_updates").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(3.0);
    }

    @Test
    void processInstanceCreated_shouldRegisterCounterWithTag() {
        metricsListener.processInstanceCreated(PROCESS_TEMPLATE_NAME);

        Counter counter = meterRegistry.find("pcs_process_instances_created")
                .tag("process_template", PROCESS_TEMPLATE_NAME)
                .counter();
        assertThat(counter).isNotNull();
    }

    @Test
    void messageReceived_firstProcessing_shouldIncrementCounterWithTags() {
        when(messageType.getName()).thenReturn(MESSAGE_TYPE_NAME);

        metricsListener.messageReceived(messageType, true);

        Counter counter = meterRegistry.find("pcs_messages_received")
                .tag("message_type", MESSAGE_TYPE_NAME)
                .tag("first_processing", "true")
                .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void messageReceived_reprocessing_shouldIncrementCounterWithTags() {
        when(messageType.getName()).thenReturn(MESSAGE_TYPE_NAME);

        metricsListener.messageReceived(messageType, false);

        Counter counter = meterRegistry.find("pcs_messages_received")
                .tag("message_type", MESSAGE_TYPE_NAME)
                .tag("first_processing", "false")
                .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void processUpdateProcessed_shouldIncrementCounterWithTags() {
        ProcessTemplate template = createProcessTemplate();

        metricsListener.processUpdateProcessed(template);
        metricsListener.processUpdateProcessed(template);

        Counter counter = meterRegistry.find("pcs_process_updates_processed")
                .tag("process_template", PROCESS_TEMPLATE_NAME)
                .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(2.0);
    }

    @Test
    void processCompleted_shouldIncrementCounterWithTag() {
        ProcessTemplate template = createProcessTemplate();

        metricsListener.processCompleted(template);

        Counter counter = meterRegistry.find("pcs_processes_completed")
                .tag("process_template", PROCESS_TEMPLATE_NAME)
                .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void snapshotCreated_shouldIncrementCounterWithTag() {
        ProcessTemplate template = createProcessTemplate();

        metricsListener.snapshotCreated(template);

        Counter counter = meterRegistry.find("pcs_snapshot_created")
                .tag("process_template", PROCESS_TEMPLATE_NAME)
                .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void timed_shouldRecordTimerWithTags() {
        AtomicBoolean executed = new AtomicBoolean(false);

        metricsListener.timed("test_timer", Map.of("tag1", "value1", "tag2", "value2"), () -> executed.set(true));

        assertThat(executed).isTrue();
        Timer timer = meterRegistry.find("test_timer")
                .tag("tag1", "value1")
                .tag("tag2", "value2")
                .timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void timedWithReturnValue_shouldRecordTimer() {
        boolean executed = metricsListener.timedWithReturnValue("test_timer_retval", () -> true);

        assertThat(executed).isTrue();
        Timer timer = meterRegistry.find("test_timer_retval")
                .timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    private ProcessTemplate createProcessTemplate() {
        TaskType taskType = TaskType.builder()
                .name("task")
                .lifecycle(TaskLifecycle.STATIC)
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .build();
        return ProcessTemplate.builder()
                .name(MicrometerMetricsListenerTest.PROCESS_TEMPLATE_NAME)
                .templateHash("hash")
                .taskTypes(List.of(taskType))
                .build();
    }
}
