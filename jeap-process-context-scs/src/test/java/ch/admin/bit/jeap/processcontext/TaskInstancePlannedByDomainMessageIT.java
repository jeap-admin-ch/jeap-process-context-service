package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.domain.processinstance.TaskState;
import ch.admin.bit.jeap.processcontext.event.test1.Test1Event;
import ch.admin.bit.jeap.processcontext.event.test3.Test3Event;
import ch.admin.bit.jeap.processcontext.testevent.Test1EventBuilder;
import ch.admin.bit.jeap.processcontext.testevent.Test3EventBuilder;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static ch.admin.bit.jeap.processcontext.TaskInstanceAssertionDto.task;

@TestPropertySource(properties =
        "jeap.processcontext.template.classpath-location-pattern=classpath:/process/templates/domain_event_triggers_task_planned_conditional.json")
class TaskInstancePlannedByDomainMessageIT extends ProcessInstanceMockS3ITBase {

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void testTaskPlannedByDomainEventWithConditionResolvingToTrue() {
        // Start a new process
        String processTemplateName = "domainEventTriggersTaskPlannedWithCondition";
        createProcessInstance(processTemplateName);

        // Plan task by domain event
        sendTest1EventWithSomeField("foo", "domainEventId");
        assertTasks(
                taskState(null, "domainEventTriggersTaskPlannedWithCondition.task.StaticTask", TaskState.PLANNED, "STATIC", "SINGLE_INSTANCE"),
                taskState("domainEventId", "domainEventTriggersTaskPlannedWithCondition.task.PlannedByDomainEvent", TaskState.PLANNED)
        );
    }

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void testTaskPlannedByDomainEventWithConditionResolvingToFalse() {
        // Start a new process
        String processTemplateName = "domainEventTriggersTaskPlannedWithCondition";
        createProcessInstance(processTemplateName);

        // Plan task by domain event
        sendTest1EventWithSomeField("bar", "domainEventId");
        assertTasks(
                taskState(null, "domainEventTriggersTaskPlannedWithCondition.task.StaticTask", TaskState.PLANNED, "STATIC", "SINGLE_INSTANCE")
        );
    }

    private TaskInstanceAssertionDto taskState(String originTaskId, String name, TaskState state) {
        return task(originTaskId, name, "DYNAMIC", "MULTI_INSTANCE", state.toString());
    }

    private TaskInstanceAssertionDto taskState(String originTaskId, String name, TaskState state, String lifecycle, String cardinality) {
        return task(originTaskId, name, lifecycle, cardinality, state.toString());
    }

    private void sendTest1EventWithSomeField(String someField, String... taskIds) {
        Test1Event event1 = Test1EventBuilder.createForProcessId(originProcessId).someField(someField).taskIds(taskIds).build();
        sendSync("topic.test1", event1);
    }

    private void createProcessInstance(String processTemplateName) {
        Test3Event event3 = Test3EventBuilder.createForProcessId(originProcessId)
                .taskIds()
                .build();
        sendSync("topic.test3", event3);
        assertProcessInstanceCreated(originProcessId, processTemplateName);
    }

    @Override
    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }
}
