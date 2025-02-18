package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.TaskInstanceAssertionDto.TaskDataAssertionDTO;
import ch.admin.bit.jeap.processcontext.domain.processinstance.TaskState;
import ch.admin.bit.jeap.processcontext.event.test1.Test1Event;
import ch.admin.bit.jeap.processcontext.testevent.Test1EventBuilder;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static ch.admin.bit.jeap.processcontext.TaskInstanceAssertionDto.task;

@SuppressWarnings("SameParameterValue")
class TaskInstancePlannedByDomainWithTaskDataIT extends ProcessInstanceMockS3ITBase {

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void testTaskPlannedByDomainEventWithConditionResolvingToTrue() {
        // Start a new process
        String processTemplateName = "domainEventTriggersTaskPlannedWithTaskData";
        createProcessInstanceFromTemplate(processTemplateName);

        // Plan task by domain event with event data named "someField" and value "foo"
        sendTest1EventWithSomeField("foo", "domainEventId");

        // Expect task data with key "someField" with value from "Test1Event" and key label "someField"
        TaskDataAssertionDTO taskDataAssertionDTO =
                new TaskDataAssertionDTO("someField", "foo", "domainEventTriggersTaskPlannedWithTaskData.task.PlannedByDomainEvent.data.someField");
                new TaskDataAssertionDTO("someField", "Test1Event", "domainEventTriggersTaskPlannedWithTaskData.task.PlannedByDomainEvent.data.someField");
        assertTasks(
                taskState(null, "domainEventTriggersTaskPlannedWithTaskData.task.StaticTask", TaskState.PLANNED, "STATIC", "SINGLE_INSTANCE"),
                taskState("domainEventId", "domainEventTriggersTaskPlannedWithTaskData.task.PlannedByDomainEvent", TaskState.PLANNED, Set.of(taskDataAssertionDTO))
        );
    }

    private TaskInstanceAssertionDto taskState(String originTaskId, String name, TaskState state, Set<TaskDataAssertionDTO> taskDataAssertionDTOs) {
        return task(originTaskId, name, "DYNAMIC", "MULTI_INSTANCE", state.toString(), taskDataAssertionDTOs);
    }

    private TaskInstanceAssertionDto taskState(String originTaskId, String name, TaskState state, String lifecycle, String cardinality) {
        return task(originTaskId, name, lifecycle, cardinality, state.toString());
    }

    private void sendTest1EventWithSomeField(String someField, String... taskIds) {
        Test1Event event1 = Test1EventBuilder.createForProcessId(originProcessId).someField(someField).taskIds(taskIds).build();
        sendSync("topic.test1", event1);
    }

    @Override
    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }
}
