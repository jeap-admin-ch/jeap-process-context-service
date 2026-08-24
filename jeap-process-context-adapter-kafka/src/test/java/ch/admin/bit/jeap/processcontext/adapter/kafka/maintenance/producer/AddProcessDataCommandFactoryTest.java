package ch.admin.bit.jeap.processcontext.adapter.kafka.maintenance.producer;

import ch.admin.bit.jeap.messaging.avro.AvroSerializationHelper;
import ch.admin.bit.jeap.messaging.avro.security.AvroClassSecurity;
import ch.admin.bit.jeap.processcontext.command.addprocessdata.AddProcessDataCommand;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJob;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobState;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobType;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTargetType;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTask;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTaskState;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessDataValue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AddProcessDataCommandFactoryTest {
    private static final UUID JOB_ID = UUID.fromString("88dbb65f-9634-4685-bc86-17b72d715d3e");
    private static final UUID TASK_ID = UUID.fromString("019c8c72-6fd1-7f25-a9a1-3b3d51fbb321");

    @BeforeAll
    static void installAvroClassWhitelist() {
        AvroClassSecurity.installDefaultIfMissing();
    }

    @Test
    void create_mapsStableIdentityAndFullDurablePayload() throws Exception {
        AddProcessDataCommandFactory factory = new AddProcessDataCommandFactory("JEAP", "process-context-service");
        MaintenanceTask task = task();

        AddProcessDataCommand command = factory.create(job(task), task);

        assertThat(command.getIdentity().getIdempotenceId()).isEqualTo(JOB_ID + "-" + TASK_ID);
        assertThat(command.getPublisher().getSystem()).isEqualTo("JEAP");
        assertThat(command.getPublisher().getService()).isEqualTo("process-context-service");
        assertThat(command.getProcessId()).isEqualTo("assessment-4711");
        assertThat(command.getPayload().getJobId()).isEqualTo(JOB_ID);
        assertThat(command.getPayload().getTaskId()).isEqualTo(TASK_ID);
        assertThat(command.getPayload().getProcessTemplateName()).isEqualTo("assessmentProcess");
        assertThat(command.getPayload().getProcessData())
                .extracting(value -> value.getKey(), value -> value.getValue(), value -> value.getRole())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("artefactId", "art-456", "FinalVersion"),
                        org.assertj.core.groups.Tuple.tuple("assessmentId", "a-123", null));
        assertThat(factory.key(task).getOriginProcessId()).isEqualTo("assessment-4711");

        AddProcessDataCommand deserialized = AvroSerializationHelper.deserialize(
                AvroSerializationHelper.serialize(command), AddProcessDataCommand.class);
        assertThat(deserialized.getPayload()).isEqualTo(command.getPayload());
    }

    private static MaintenanceJob job(MaintenanceTask task) {
        return new MaintenanceJob(JOB_ID, MaintenanceJobType.PROCESS_DATA_BACKFILL, "assessmentProcess",
                "a".repeat(64), MaintenanceJobState.OPEN, null, task.createdAt(), null, null, null, List.of(task));
    }

    private static MaintenanceTask task() {
        Instant now = Instant.parse("2026-08-06T08:03:12Z");
        return new MaintenanceTask(TASK_ID, MaintenanceTargetType.PROCESS, "assessment-4711", "assessment-4711",
                MaintenanceTaskState.COMMAND_QUEUED, now, null, null, null, List.of(
                new ProcessDataValue("assessmentId", "a-123", null),
                new ProcessDataValue("artefactId", "art-456", "FinalVersion")));
    }
}
