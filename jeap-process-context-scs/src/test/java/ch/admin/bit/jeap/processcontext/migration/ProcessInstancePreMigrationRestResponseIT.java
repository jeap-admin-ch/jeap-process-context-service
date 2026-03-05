package ch.admin.bit.jeap.processcontext.migration;

import ch.admin.bit.jeap.processcontext.ProcessInstanceMockS3ITBase;
import ch.admin.bit.jeap.processcontext.adapter.restapi.model.ProcessInstanceDTO;
import ch.admin.bit.jeap.processcontext.adapter.restapi.model.TaskInstanceDTO;
import ch.admin.bit.jeap.processcontext.domain.tx.Transactions;
import ch.admin.bit.jeap.processcontext.event.test1.Test1Event;
import ch.admin.bit.jeap.processcontext.testevent.Test1EventBuilder;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties =
        "jeap.processcontext.template.classpath-location-pattern=classpath:/process/templates/migration_task_test*.json")
class ProcessInstancePreMigrationRestResponseIT extends ProcessInstanceMockS3ITBase {

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private Transactions transactions;

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void getProcessInstance_whenTemplateChangedButNotMigrated_thenReturnsUsableResponse() {
        // Create process instance with migrationTaskTest template (task1, task2)
        Test1Event event = Test1EventBuilder.createForProcessId(originProcessId).build();
        sendSync("topic.test1", event);
        assertProcessInstanceCreated(originProcessId, "migrationTaskTest");

        // Switch template name without running migration
        transactions.withinNewTransaction(() -> entityManager
                .createQuery("UPDATE ProcessInstance p SET p.processTemplateName='migrationTaskTestNew' WHERE p.originProcessId='" + originProcessId + "'")
                .executeUpdate());

        // Query process instance via REST controller (no migration triggered)
        ProcessInstanceDTO dto = processInstanceController.getProcessInstanceByOriginProcessId(originProcessId);

        // Process state and basic fields are populated
        assertThat(dto.getState()).isEqualTo("STARTED");
        assertThat(dto.getTemplateName()).isEqualTo("migrationTaskTestNew");
        assertThat(dto.getName()).isNotEmpty();

        // Only the 2 original tasks are present (task3 from new template not yet created)
        assertThat(dto.getTasks()).hasSize(2);

        // task1 exists in both templates — lifecycle/cardinality resolved from new template
        TaskInstanceDTO task1 = dto.getTasks().stream()
                .filter(t -> t.getName().get("de").equals("migrationTaskTestNew.task.task1"))
                .findFirst().orElseThrow();
        assertThat(task1.getState()).isEqualTo("PLANNED");
        assertThat(task1.getLifecycle()).isEqualTo("STATIC");
        assertThat(task1.getCardinality()).isEqualTo("SINGLE_INSTANCE");

        // task2 no longer exists in new template — lifecycle/cardinality are UNKNOWN
        TaskInstanceDTO task2 = dto.getTasks().stream()
                .filter(t -> t.getName().get("de").equals("migrationTaskTestNew.task.task2"))
                .findFirst().orElseThrow();
        assertThat(task2.getState()).isEqualTo("PLANNED");
        assertThat(task2.getLifecycle()).isEqualTo("UNKNOWN");
        assertThat(task2.getCardinality()).isEqualTo("UNKNOWN");
    }

    @Override
    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }
}
