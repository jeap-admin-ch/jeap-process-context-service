package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.processcontext.domain.processinstance.TaskState;
import ch.admin.bit.jeap.processcontext.event.test1.Test1Event;
import ch.admin.bit.jeap.processcontext.event.test2.Test2Event;
import ch.admin.bit.jeap.processcontext.testevent.Test1EventBuilder;
import ch.admin.bit.jeap.processcontext.testevent.Test2EventBuilder;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import org.junit.jupiter.api.Test;

import static ch.admin.bit.jeap.processcontext.TaskInstanceAssertionDto.task;
import static ch.admin.bit.jeap.processcontext.TaskInstanceAssertionDto.taskWithoutOriginTaskId;

class TaskSingleInstancePlannedByDomainEventIT extends ProcessInstanceMockS3ITBase {

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void testTaskPlannedByDomainEvent_singleInstance() {
        // Start a new process
        String processTemplateName = "domainEventTriggersSingleInstanceTaskPlanned";
        createProcessInstanceFromTemplate(processTemplateName);
        assertTasks(taskStateStatic(processTemplateName, TaskState.PLANNED));

        // Plan dynamic single instance task by domain event
        sendTest1Event("domainEventId");
        assertTasks(
                taskStateDynamic(processTemplateName, TaskState.PLANNED),
                taskStateStatic(processTemplateName, TaskState.COMPLETED)
        );

        // Complete single instance task with domain event
        sendTest2Event();
        assertTasks(
                taskStateDynamic(processTemplateName, TaskState.COMPLETED),
                taskStateStatic(processTemplateName, TaskState.COMPLETED)
        );
        assertProcessInstanceCompleted(originProcessId);
    }

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void testTaskPlannedByDomainEvent_singleInstance_omitOnDuplicateCreation() {
        // Start a new process
        String processTemplateName = "domainEventTriggersSingleInstanceTaskPlanned";
        createProcessInstanceFromTemplate(processTemplateName);
        assertTasks(taskStateStatic(processTemplateName, TaskState.PLANNED));

        // Plan dynamic single instance task by domain event
        sendTest1Event("domainEventId");
        assertTasks(
                taskStateDynamic(processTemplateName, TaskState.PLANNED),
                taskStateStatic(processTemplateName, TaskState.COMPLETED)
        );

        // No second instance of 'PlannedByDomainEventSingleInstance' expected.
        Message message = sendTest1Event("domainEventId");
        awaitProcessUpdateHandled(originProcessId, message);
        assertTasks(
                taskStateDynamic(processTemplateName, TaskState.PLANNED),
                taskStateStatic(processTemplateName, TaskState.COMPLETED)
        );
    }

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void testTaskPlannedByDomainEvent_singleInstance_completeByName() {
        // Start a new process
        String processTemplateName = "domainEventTriggersSingleInstanceTaskPlanned";
        createProcessInstanceFromTemplate(processTemplateName);
        assertTasks(taskStateStatic(processTemplateName, TaskState.PLANNED));

        // Plan dynamic single instance task by domain event
        sendTest1Event("domainEventId");
        assertTasks(
                taskStateDynamic(processTemplateName, TaskState.PLANNED),
                taskStateStatic(processTemplateName, TaskState.COMPLETED)
        );

        // Send completing domain event without any task ids.
        sendTest2Event();

        assertProcessInstanceCompleted(originProcessId);
    }

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void testTaskPlannedByDomainEvent_singleInstanceConditionResolvesToTrue() {
        // Start a new process
        String processTemplateName = "domainEventTriggersSingleInstanceTaskPlannedConditional";
        createProcessInstanceFromTemplate(processTemplateName);
        assertTasks(taskStateStatic(processTemplateName, TaskState.PLANNED));

        // Plan dynamic single instance task by domain event
        sendTest1EventWithSomeField("foo", "domainEventId");
        assertTasks(
                taskStateDynamic(processTemplateName, TaskState.PLANNED),
                taskStateStatic(processTemplateName, TaskState.COMPLETED)
        );

        // Complete single instance task with domain event
        sendTest2Event();
        assertTasks(
                taskStateDynamic(processTemplateName, TaskState.COMPLETED),
                taskStateStatic(processTemplateName, TaskState.COMPLETED)
        );
        assertProcessInstanceCompleted(originProcessId);
    }

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void testTaskPlannedByDomainEvent_singleInstanceConditionResolvesToFalse() {
        // Start a new process
        String processTemplateName = "domainEventTriggersSingleInstanceTaskPlannedConditional";
        createProcessInstanceFromTemplate(processTemplateName);
        assertTasks(taskStateStatic(processTemplateName, TaskState.PLANNED));

        // Plan dynamic single instance task by domain event
        sendTest1Event("domainEventId");
        assertTasks(
                taskStateStatic(processTemplateName, TaskState.COMPLETED)
        );

        // Complete single instance task with domain event
        sendTest2Event();
        assertTasks(
                taskStateStatic(processTemplateName, TaskState.COMPLETED)
        );
        assertProcessInstanceCompleted(originProcessId);
    }

    public static TaskInstanceAssertionDto taskState(String originTaskId, String name, TaskState state, String lifecycle, String cardinality) {
        return task(originTaskId, name, lifecycle, cardinality, state.toString());
    }

    private TaskInstanceAssertionDto taskStateStatic(String processTemplateName, TaskState state) {
        return taskWithoutOriginTaskId(processTemplateName + ".task.staticTask", "STATIC", "SINGLE_INSTANCE", state.toString());
    }

    private TaskInstanceAssertionDto taskStateDynamic(String processTemplateName, TaskState state) {
        return task("domainEventId", processTemplateName + ".task.PlannedByDomainEventSingleInstance", "DYNAMIC", "SINGLE_INSTANCE", state.toString());
    }

    private Message sendTest1Event(String... taskIds) {
        Test1Event event1 = Test1EventBuilder.createForProcessId(originProcessId).taskIds(taskIds).build();
       sendSync("topic.test1", event1);
        return event1;
    }

    private void sendTest1EventWithSomeField(String someField, String... taskIds) {
        Test1Event event1 = Test1EventBuilder.createForProcessId(originProcessId).someField(someField).taskIds(taskIds).build();
        sendSync("topic.test1", event1);
    }

    private void sendTest2Event(String... taskIds) {
        Test2Event event2 = Test2EventBuilder.createForProcessId(originProcessId).taskIds(taskIds).build();
        sendSync("topic.test2", event2);
    }

    @Override
    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }
}
