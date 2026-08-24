package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.adapter.restapi.ReevaluationJobController;
import ch.admin.bit.jeap.processcontext.domain.maintenance.BackfillJobEntry;
import ch.admin.bit.jeap.processcontext.domain.maintenance.BackfillJobService;
import ch.admin.bit.jeap.processcontext.domain.maintenance.BackfillJobSubmission;
import ch.admin.bit.jeap.processcontext.domain.maintenance.AddProcessDataCommandService;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobResult;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobRepository;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobSubmitter;
import ch.admin.bit.jeap.processcontext.domain.maintenance.ReevaluationJobSubmission;
import ch.admin.bit.jeap.processcontext.domain.maintenance.ReevaluationJobService;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessDataValue;
import ch.admin.bit.jeap.processcontext.event.test1.SubjectReference;
import ch.admin.bit.jeap.processcontext.event.test1.Test1Event;
import ch.admin.bit.jeap.processcontext.event.test1.Test1EventReferences;
import ch.admin.bit.jeap.processcontext.event.test2.Test2Event;
import ch.admin.bit.jeap.processcontext.testevent.Test1EventBuilder;
import ch.admin.bit.jeap.processcontext.testevent.Test2EventBuilder;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        "jeap.processcontext.maintenance.enabled=true",
        "spring.jpa.database=h2",
        "jeap.processcontext.template.classpath-location-pattern=" +
                "classpath:/process/templates/relations_join.json"
})
class MaintenanceEnabledContextIT extends ProcessInstanceMockS3ITBase {

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private ReevaluationJobService reevaluationJobService;
    @Autowired
    private BackfillJobService backfillJobService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private RelationListenerStub relationListenerStub;

    @Test
    void maintenanceEnabled_registersCompleteRuntimeSlice() {
        assertThat(applicationContext.getBeansOfType(MaintenanceJobRepository.class)).hasSize(1);
        assertThat(applicationContext.getBean(ReevaluationJobService.class)).isNotNull();
        assertThat(applicationContext.getBean(BackfillJobService.class)).isNotNull();
        assertThat(applicationContext.getBean(ReevaluationJobController.class)).isNotNull();
        assertThat(applicationContext.getBean(AddProcessDataCommandService.class)).isNotNull();
        assertThat(applicationContext.containsBean("maintenanceJobJpaRepository")).isTrue();
        assertThat(applicationContext.containsBean("maintenanceTaskJpaRepository")).isTrue();
        assertThat(applicationContext.containsBean("outboxMaintenanceEventPublisher")).isTrue();
        assertThat(applicationContext.containsBean("outboxMaintenanceCommandPublisher")).isTrue();
        assertThat(applicationContext.containsBean("addProcessDataCommandConsumer")).isTrue();
        assertThat(applicationContext.containsBean("maintenanceProcessContextOutdatedEventHandler")).isTrue();
    }

    @Test
    void backfillJob_addsProcessDataRecreatesRelationAndCompletesJob() {
        Test1Event subjectEvent = Test1EventBuilder.createForProcessId(originProcessId).taskIds().build();
        subjectEvent.setReferences(Test1EventReferences.newBuilder()
                .setSubjectReference(SubjectReference.newBuilder().setSubjectId("subject-1").build())
                .build());
        sendSync("topic.test1", subjectEvent);
        Awaitility.await().untilAsserted(() -> assertThat(processDataCount("subjectKey", "subject-1")).isOne());

        UUID jobId = UUID.randomUUID();
        boolean created = backfillJobService.submit(new BackfillJobSubmission(
                jobId, "relations", List.of(new BackfillJobEntry(originProcessId,
                        List.of(new ProcessDataValue("objectKey", "object-1", null)))),
                new MaintenanceJobSubmitter("test", "test")));

        assertThat(created).isTrue();
        Awaitility.await().untilAsserted(() -> {
            assertThat(processDataCount("objectKey", "object-1")).isOne();
            assertThat(relationCount()).isOne();
            assertThat(relationListenerStub.getRelations(originProcessId)).hasSize(1);
            assertThat(backfillJobService.get(jobId))
                    .get()
                    .extracting(job -> job.jobResult())
                    .isEqualTo(MaintenanceJobResult.SUCCEEDED);
        });
    }

    @Test
    void reevaluationJob_recreatesRelationAndCompletesJob() {
        Test1Event subjectEvent = Test1EventBuilder.createForProcessId(originProcessId).taskIds().build();
        subjectEvent.setReferences(Test1EventReferences.newBuilder()
                .setSubjectReference(SubjectReference.newBuilder().setSubjectId("subject-1").build())
                .build());
        Test2Event objectEvent = Test2EventBuilder.createForProcessId(originProcessId).objectId("object-1").build();

        sendSync("topic.test1", subjectEvent);
        sendSync("topic.test2", objectEvent);
        Awaitility.await().untilAsserted(() -> assertThat(relationCount()).isOne());

        jdbcTemplate.update("DELETE FROM process_instance_relations");
        UUID jobId = UUID.randomUUID();
        boolean created = reevaluationJobService.submit(new ReevaluationJobSubmission(
                jobId, "relations", List.of(originProcessId), new MaintenanceJobSubmitter("test", "test")));

        assertThat(created).isTrue();
        Awaitility.await().untilAsserted(() -> {
            assertThat(relationCount()).isOne();
            assertThat(reevaluationJobService.get(jobId))
                    .get()
                    .extracting(job -> job.jobResult())
                    .isEqualTo(MaintenanceJobResult.SUCCEEDED);
        });
    }

    private int relationCount() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM process_instance_relations relation
                  JOIN process_instance process ON process.id = relation.process_instance_id
                 WHERE process.origin_process_id = ?
                """, Integer.class, originProcessId);
    }

    private int processDataCount(String key, String value) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM process_instance_process_data data
                  JOIN process_instance process ON process.id = data.process_instance_id
                 WHERE process.origin_process_id = ?
                   AND data.key_ = ?
                   AND data.value_ = ?
                """, Integer.class, originProcessId, key, value);
    }
}
