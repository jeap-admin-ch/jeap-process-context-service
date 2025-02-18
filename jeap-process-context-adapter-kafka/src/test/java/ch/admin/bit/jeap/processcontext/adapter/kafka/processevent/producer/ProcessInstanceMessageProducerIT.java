package ch.admin.bit.jeap.processcontext.adapter.kafka.processevent.producer;

import ch.admin.bit.jeap.messaging.api.MessageListener;
import ch.admin.bit.jeap.processcontext.adapter.kafka.KafkaAdapterIntegrationTestBase;
import ch.admin.bit.jeap.processcontext.domain.port.ProcessInstanceEventProducer;
import ch.admin.bit.jeap.processcontext.event.process.instance.completed.ProcessInstanceCompletedEvent;
import ch.admin.bit.jeap.processcontext.event.process.instance.created.ProcessInstanceCreatedEvent;
import ch.admin.bit.jeap.processcontext.event.process.milestone.reached.ProcessMilestoneReachedEvent;
import ch.admin.bit.jeap.processcontext.event.process.snapshot.created.ProcessSnapshotCreatedEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProcessInstanceMessageProducerIT extends KafkaAdapterIntegrationTestBase {
    private static final String TEST_ORIGIN_ID = "test-origin-id";
    private static final String MILESTONE_NAME = "milestone";
    private static final int SNAPSHOT_VERSION = 2;

    @Captor
    ArgumentCaptor<ProcessInstanceCreatedEvent> processInstanceCreatedEventCaptor;
    @Captor
    ArgumentCaptor<ProcessInstanceCompletedEvent> processInstanceCompletedEventCaptor;
    @Captor
    ArgumentCaptor<ProcessMilestoneReachedEvent> processMilestoneReachedEventCaptor;
    @Captor
    ArgumentCaptor<ProcessSnapshotCreatedEvent> processSnapshotCreatedEventCaptor;
    @Autowired
    ProcessInstanceEventProducer processInstanceEventProducer;
    @MockitoBean
    private MessageListener<ProcessInstanceCreatedEvent> processInstanceCreatedEventListener;
    @MockitoBean
    private MessageListener<ProcessInstanceCompletedEvent> processInstanceCompletedEventListener;
    @MockitoBean
    private MessageListener<ProcessMilestoneReachedEvent> processMilestoneReachedEventListener;
    @MockitoBean
    private MessageListener<ProcessSnapshotCreatedEvent> processSnapshotCreatedEventListener;

    @Test
    void testProduceProcessInstanceCreatedEventSynchronously() {
        String processName = "processName";
        processInstanceEventProducer.produceProcessInstanceCreatedEventSynchronously(TEST_ORIGIN_ID, processName);

        Mockito.verify(processInstanceCreatedEventListener, Mockito.timeout(TEST_TIMEOUT)).receive(processInstanceCreatedEventCaptor.capture());
        assertEquals(TEST_ORIGIN_ID, processInstanceCreatedEventCaptor.getValue().getOptionalProcessId().orElseThrow());
        assertEquals(TEST_ORIGIN_ID + "-created", processInstanceCreatedEventCaptor.getValue().getIdentity().getIdempotenceId());
        assertEquals(processName, processInstanceCreatedEventCaptor.getValue().getPayload().getProcessName());
        Mockito.verifyNoMoreInteractions(processInstanceCreatedEventListener);
    }

    @Test
    void testProduceProcessInstanceCompletedEventSynchronously() {
        processInstanceEventProducer.produceProcessInstanceCompletedEventSynchronously(TEST_ORIGIN_ID);

        Mockito.verify(processInstanceCompletedEventListener, Mockito.timeout(TEST_TIMEOUT)).receive(processInstanceCompletedEventCaptor.capture());
        assertEquals(TEST_ORIGIN_ID, processInstanceCompletedEventCaptor.getValue().getOptionalProcessId().orElseThrow());
        assertEquals(TEST_ORIGIN_ID + "-completed", processInstanceCompletedEventCaptor.getValue().getIdentity().getIdempotenceId());
        Mockito.verifyNoMoreInteractions(processInstanceCompletedEventListener);
    }

    @Test
    void testProduceProcessMilestoneReachedEventSynchronously() {
        processInstanceEventProducer.produceProcessMilestoneReachedEventSynchronously(TEST_ORIGIN_ID, MILESTONE_NAME);

        Mockito.verify(processMilestoneReachedEventListener, Mockito.timeout(TEST_TIMEOUT)).receive(processMilestoneReachedEventCaptor.capture());
        assertEquals(TEST_ORIGIN_ID, processMilestoneReachedEventCaptor.getValue().getOptionalProcessId().orElseThrow());
        assertEquals(MILESTONE_NAME, processMilestoneReachedEventCaptor.getValue().getPayload().getMilestoneName());
        assertEquals(TEST_ORIGIN_ID + "-milestone-reached-" + MILESTONE_NAME, processMilestoneReachedEventCaptor.getValue().getIdentity().getIdempotenceId());
        Mockito.verifyNoMoreInteractions(processMilestoneReachedEventListener);
    }

    @Test
    void testProduceProcessSnapshotCreatedEventSynchronously() {
        processInstanceEventProducer.produceProcessSnapshotCreatedEventSynchronously(TEST_ORIGIN_ID, SNAPSHOT_VERSION);

        Mockito.verify(processSnapshotCreatedEventListener, Mockito.timeout(TEST_TIMEOUT)).receive(processSnapshotCreatedEventCaptor.capture());
        assertEquals(TEST_ORIGIN_ID, processSnapshotCreatedEventCaptor.getValue().getOptionalProcessId().orElseThrow());
        assertEquals(SNAPSHOT_VERSION, processSnapshotCreatedEventCaptor.getValue().getReferences().getReference().getSnapshotVersion());
        assertEquals(TEST_ORIGIN_ID + "-snapshot-created-" + SNAPSHOT_VERSION, processSnapshotCreatedEventCaptor.getValue().getIdentity().getIdempotenceId());
        Mockito.verifyNoMoreInteractions(processSnapshotCreatedEventListener);
    }
}
