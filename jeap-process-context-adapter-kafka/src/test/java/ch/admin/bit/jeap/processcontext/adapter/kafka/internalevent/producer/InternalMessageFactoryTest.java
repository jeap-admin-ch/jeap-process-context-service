package ch.admin.bit.jeap.processcontext.adapter.kafka.internalevent.producer;

import ch.admin.bit.jeap.messaging.avro.security.AvroClassSecurity;
import ch.admin.bit.jeap.processcontext.domain.maintenance.*;
import ch.admin.bit.jeap.processcontext.internal.event.outdated.ProcessUpdateType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InternalMessageFactoryTest {
    private static final UUID JOB_ID = UUID.fromString("88dbb65f-9634-4685-bc86-17b72d715d3e");
    private static final UUID TASK_ID = UUID.fromString("019c8c72-6fd1-7f25-a9a1-3b3d51fbb321");
    private static final UUID RELATION_ID = UUID.fromString("f8eb7f71-3276-49f2-a221-bb60b2cc0158");

    @BeforeAll
    static void installAvroClassWhitelist() {
        AvroClassSecurity.installDefaultIfMissing();
    }

    @Test
    void maintenanceEvent_containsTaskEnvelopeAndStableIdempotenceId() {
        InternalMessageFactory factory = new InternalMessageFactory();
        ReflectionTestUtils.setField(factory, "systemName", "JEAP");
        ReflectionTestUtils.setField(factory, "serviceName", "process-context-service");
        Instant now = Instant.parse("2026-08-06T08:03:12Z");
        MaintenanceTask task = new MaintenanceTask(TASK_ID, MaintenanceTargetType.PROCESS,
                "assessment-4711", "assessment-4711", MaintenanceTaskState.EVENT_QUEUED, now, null, null, null);
        MaintenanceJob job = new MaintenanceJob(JOB_ID, MaintenanceJobType.RELATION_REEVALUATION,
                "assessmentProcess", "a".repeat(64), MaintenanceJobState.OPEN, null, now,
                null, null, null, List.of(task));

        var event = factory.processContextOutdatedMaintenanceEvent(job, task);

        assertThat(event.getProcessId()).isEqualTo("assessment-4711");
        assertThat(event.getPayload().getProcessUpdateType()).isEqualTo(ProcessUpdateType.REEVALUATE_JOB);
        assertThat(event.getPayload().getMaintenanceJobTask().getJobId()).isEqualTo(JOB_ID);
        assertThat(event.getPayload().getMaintenanceJobTask().getTaskId()).isEqualTo(TASK_ID);
        assertThat(event.getPayload().getMaintenanceJobTask().getTemplateName()).isEqualTo("assessmentProcess");
        assertThat(event.getIdentity().getIdempotenceId()).contains(JOB_ID + "-" + TASK_ID);
        assertThat(factory.key("assessment-4711").getProcessId()).isEqualTo("assessment-4711");
    }

    @Test
    void maintenanceEvent_backfillUsesBackfillUpdateType() {
        InternalMessageFactory factory = new InternalMessageFactory();
        ReflectionTestUtils.setField(factory, "systemName", "JEAP");
        ReflectionTestUtils.setField(factory, "serviceName", "process-context-service");
        Instant now = Instant.parse("2026-08-06T08:03:12Z");
        MaintenanceTask task = new MaintenanceTask(TASK_ID, MaintenanceTargetType.PROCESS,
                "assessment-4711", "assessment-4711", MaintenanceTaskState.COMMAND_QUEUED,
                now, null, null, null);
        MaintenanceJob job = new MaintenanceJob(JOB_ID, MaintenanceJobType.PROCESS_DATA_BACKFILL,
                "assessmentProcess", "a".repeat(64), MaintenanceJobState.OPEN, null, now,
                null, null, null, List.of(task));

        var event = factory.processContextOutdatedMaintenanceEvent(job, task);

        assertThat(event.getPayload().getProcessUpdateType()).isEqualTo(ProcessUpdateType.BACKFILL_JOB);
    }

    @Test
    void relationRepublicationEvent_containsRelationPayloadWithoutTemplateAndUsesOriginProcessId() {
        InternalMessageFactory factory = factory();
        MaintenanceJob job = relationRepublicationJob();
        MaintenanceTask task = relationTask("assessment-4711");

        var event = factory.processContextOutdatedMaintenanceEvent(job, task);

        assertThat(event.getProcessId()).isEqualTo("assessment-4711");
        assertThat(event.getPayload().getProcessUpdateType()).isEqualTo(ProcessUpdateType.REPUBLISH_RELATION_JOB);
        assertThat(event.getPayload().getMaintenanceJobTask().getJobId()).isEqualTo(JOB_ID);
        assertThat(event.getPayload().getMaintenanceJobTask().getTaskId()).isEqualTo(TASK_ID);
        assertThat(event.getPayload().getMaintenanceJobTask().getOptionalTemplateName()).isEmpty();
        assertThat(event.getPayload().getMaintenanceJobTask().getRelationId()).isEqualTo(RELATION_ID);
        assertThat(event.getIdentity().getIdempotenceId())
                .isEqualTo("assessment-4711-maintenance-" + JOB_ID + "-" + TASK_ID);
    }

    @Test
    void unresolvedRelationRepublicationEvent_usesRelationTargetAsProcessIdWithStableJobTaskIdentity() {
        InternalMessageFactory factory = factory();
        MaintenanceJob job = relationRepublicationJob();
        MaintenanceTask task = relationTask(null);

        var firstEvent = factory.processContextOutdatedMaintenanceEvent(job, task);
        var repeatedEvent = factory.processContextOutdatedMaintenanceEvent(job, task);

        assertThat(firstEvent.getProcessId()).isEqualTo(RELATION_ID.toString());
        assertThat(factory.maintenanceProcessId(task)).isEqualTo(RELATION_ID.toString());
        assertThat(firstEvent.getIdentity().getIdempotenceId())
                .isEqualTo(RELATION_ID + "-maintenance-" + JOB_ID + "-" + TASK_ID)
                .isEqualTo(repeatedEvent.getIdentity().getIdempotenceId());
    }

    private static InternalMessageFactory factory() {
        InternalMessageFactory factory = new InternalMessageFactory();
        ReflectionTestUtils.setField(factory, "systemName", "JEAP");
        ReflectionTestUtils.setField(factory, "serviceName", "process-context-service");
        return factory;
    }

    private static MaintenanceJob relationRepublicationJob() {
        MaintenanceJob job = mock(MaintenanceJob.class);
        when(job.jobId()).thenReturn(JOB_ID);
        when(job.jobType()).thenReturn(MaintenanceJobType.RELATION_REPUBLICATION);
        when(job.processTemplateName()).thenReturn(null);
        return job;
    }

    private static MaintenanceTask relationTask(String originProcessId) {
        MaintenanceTask task = mock(MaintenanceTask.class);
        when(task.taskId()).thenReturn(TASK_ID);
        when(task.targetKey()).thenReturn(RELATION_ID.toString());
        when(task.originProcessId()).thenReturn(originProcessId);
        when(task.relationId()).thenReturn(RELATION_ID);
        return task;
    }
}
