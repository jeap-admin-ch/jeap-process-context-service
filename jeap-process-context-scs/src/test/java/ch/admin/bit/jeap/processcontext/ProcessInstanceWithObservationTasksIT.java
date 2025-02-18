package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.domain.processinstance.TaskState;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskLifecycle;
import ch.admin.bit.jeap.processcontext.event.test1.Test1Event;
import ch.admin.bit.jeap.processcontext.testevent.Test1EventBuilder;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import org.junit.jupiter.api.Test;

import static ch.admin.bit.jeap.processcontext.TaskInstanceAssertionDto.task;

class ProcessInstanceWithObservationTasksIT extends ProcessInstanceMockS3ITBase {

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void completingObservationTasks() {
        // Start a new process
        String processTemplateName = "observationTasksWithDomainEvent";
        createProcessInstanceFromTemplate(processTemplateName);
        assertProcessInstanceCreatedEvent(originProcessId, processTemplateName);

        // OldWay Task exists, but no ObservedTask
        assertTasks(
                taskState(null, "observationTasksWithDomainEvent.task.PlannedTheOldWay", TaskLifecycle.STATIC, TaskState.PLANNED, "SINGLE_INSTANCE")
        );

        // Produce event 1, which is expected to complete the 'completed_by_domain_event_observation_task' task immediately
        String eventId1 = sendTest1Event("someEventId");
        assertTasks(
                taskState(eventId1, "observationTasksWithDomainEvent.task.first_observation_task", TaskLifecycle.OBSERVED, TaskState.COMPLETED, "MULTI_INSTANCE"),
                taskState(null, "observationTasksWithDomainEvent.task.PlannedTheOldWay", TaskLifecycle.STATIC, TaskState.PLANNED, "SINGLE_INSTANCE")
        );

    }

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void completingObservationTasksWithConditionResolvingToTrue() {
        // Start a new process
        String processTemplateName = "observationTasksWithDomainEventConditional";
        createProcessInstanceFromTemplate(processTemplateName);
        assertProcessInstanceCreatedEvent(originProcessId, processTemplateName);

        // OldWay Task exists, but no ObservedTask
        assertTasks(
                taskState(null, "observationTasksWithDomainEventConditional.task.PlannedTheOldWay", TaskLifecycle.STATIC, TaskState.PLANNED, "SINGLE_INSTANCE")
        );

        // Produce event 1, which is expected to complete the 'completed_by_domain_event_observation_task' task immediately
        String eventId1 = sendTest1EventWithSomeField("foo", "someEventId");
        assertTasks(
                taskState(eventId1, "observationTasksWithDomainEventConditional.task.first_observation_task", TaskLifecycle.OBSERVED, TaskState.COMPLETED, "MULTI_INSTANCE"),
                taskState(null, "observationTasksWithDomainEventConditional.task.PlannedTheOldWay", TaskLifecycle.STATIC, TaskState.PLANNED, "SINGLE_INSTANCE")
        );
    }

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void completingObservationTasksWithConditionResolvingToFalse() {
        // Start a new process
        String processTemplateName = "observationTasksWithDomainEventConditional";
        createProcessInstanceFromTemplate(processTemplateName);
        assertProcessInstanceCreatedEvent(originProcessId, processTemplateName);

        // OldWay Task exists, but no ObservedTask
        assertTasks(
                taskState(null, "observationTasksWithDomainEventConditional.task.PlannedTheOldWay", TaskLifecycle.STATIC, TaskState.PLANNED, "SINGLE_INSTANCE")
        );

        // Produce event 1, which won't instantiate first_observation_task due to non matching condition
        sendTest1Event("someEventId");
        assertTasks(
                taskState(null, "observationTasksWithDomainEventConditional.task.PlannedTheOldWay", TaskLifecycle.STATIC, TaskState.PLANNED, "SINGLE_INSTANCE")
        );

    }

    private TaskInstanceAssertionDto taskState(String originTaskId, String name, TaskLifecycle lifecycle, TaskState state, String cardinality) {
        return task(originTaskId, name, lifecycle.name(), cardinality, state.name());
    }

    private String sendTest1Event(String... taskIds) {
        Test1Event event1 = Test1EventBuilder.createForProcessId(originProcessId).taskIds(taskIds).build();
        sendSync("topic.test1", event1);
        return event1.getIdentity().getEventId();
    }

    private String sendTest1EventWithSomeField(String someField, String... taskIds) {
        Test1Event event1 = Test1EventBuilder.createForProcessId(originProcessId).someField(someField).taskIds(taskIds).build();
        sendSync("topic.test1", event1);
        return event1.getIdentity().getEventId();
    }
    @Override
    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }
}
