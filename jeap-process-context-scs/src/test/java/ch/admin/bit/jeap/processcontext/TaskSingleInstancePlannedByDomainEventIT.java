package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.processcontext.domain.processinstance.TaskState;
import ch.admin.bit.jeap.processcontext.event.test1.Test1Event;
import ch.admin.bit.jeap.processcontext.event.test2.Test2Event;
import ch.admin.bit.jeap.processcontext.event.test3.Test3Event;
import ch.admin.bit.jeap.processcontext.event.test4.Test4Event;
import ch.admin.bit.jeap.processcontext.testevent.Test1EventBuilder;
import ch.admin.bit.jeap.processcontext.testevent.Test2EventBuilder;
import ch.admin.bit.jeap.processcontext.testevent.Test3EventBuilder;
import ch.admin.bit.jeap.processcontext.testevent.Test4EventBuilder;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static ch.admin.bit.jeap.processcontext.TaskInstanceAssertionDto.task;
import static ch.admin.bit.jeap.processcontext.TaskInstanceAssertionDto.taskWithoutOriginTaskId;

@SuppressWarnings("SameParameterValue")
@TestPropertySource(properties =
        "jeap.processcontext.template.classpath-location-pattern=classpath:/process/templates/domain_event_triggers_single_instance_task_planned*.json")
class TaskSingleInstancePlannedByDomainEventIT extends ProcessInstanceMockS3ITBase {

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void testTaskPlannedByDomainEvent_singleInstance() {
        // Start a new process
        String processTemplateName = "domainEventTriggersSingleInstanceTaskPlanned";
        createProcessInstance(processTemplateName);
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
        createProcessInstance(processTemplateName);
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
        createProcessInstance(processTemplateName);
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
        createProcessInstanceForConditionalTemplate(processTemplateName);
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
        createProcessInstanceForConditionalTemplate(processTemplateName);
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

    private void createProcessInstance(String templateName) {
        Test3Event event3 = Test3EventBuilder.createForProcessId(originProcessId).build();
        sendSync("topic.test3", event3);
        assertProcessInstanceCreated(originProcessId, templateName);
    }

    private void createProcessInstanceForConditionalTemplate(String templateName) {
        Test4Event event4 = Test4EventBuilder.createForProcessId(originProcessId).build();
        sendSync("topic.test4", event4);
        assertProcessInstanceCreated(originProcessId, templateName);
    }

    @Override
    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }
}
