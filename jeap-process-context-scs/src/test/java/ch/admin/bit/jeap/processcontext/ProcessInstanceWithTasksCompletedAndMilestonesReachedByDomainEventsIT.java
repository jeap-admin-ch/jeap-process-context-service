package ch.admin.bit.jeap.processcontext;

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

import static ch.admin.bit.jeap.processcontext.TaskInstanceAssertionDto.task;
import static ch.admin.bit.jeap.processcontext.TaskInstanceAssertionDto.taskWithoutOriginTaskId;

class ProcessInstanceWithTasksCompletedAndMilestonesReachedByDomainEventsIT extends ProcessInstanceMockS3ITBase {

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    @SuppressWarnings("java:S2925")
    void processWithTaskCompletionConditions_whenConditionsAreMet_thenExpectTaskToBeCompleted() throws Exception {
        String processTemplateName = "domainEvents";
        createProcessInstanceFromTemplate(processTemplateName);
        assertProcessInstanceCreatedEvent(originProcessId, processTemplateName);
        assertTasks(taskWithoutOriginTaskId("domainEvents.task.mandatory", "STATIC", "SINGLE_INSTANCE", "PLANNED"));

        // Plan multi-instance tasks
        Test4Event event4 = Test4EventBuilder.createForProcessId(originProcessId)
                .taskIds("id-multiple-1", "id-multiple-2", "id-multiple-3")
                .build();
        sendSync("topic.test4", event4);

        assertTasks(
                taskWithoutOriginTaskId("domainEvents.task.mandatory", "STATIC", "SINGLE_INSTANCE", "PLANNED"),
                task("id-multiple-1", "domainEvents.task.multiple", "DYNAMIC", "MULTI_INSTANCE", "PLANNED"),
                task("id-multiple-2", "domainEvents.task.multiple", "DYNAMIC", "MULTI_INSTANCE", "PLANNED"),
                task("id-multiple-3", "domainEvents.task.multiple", "DYNAMIC", "MULTI_INSTANCE", "PLANNED"));

        // Produce event 1, which is expected to complete the 'mandatory' task, which should lead to 2 milestones being reached
        Test1Event event1 = Test1EventBuilder.createForProcessId(originProcessId)
                .build();
        sendSync("topic.test1", event1);

        assertTasks(
                taskWithoutOriginTaskId("domainEvents.task.mandatory", "STATIC", "SINGLE_INSTANCE", "COMPLETED"),
                task("id-multiple-1", "domainEvents.task.multiple", "DYNAMIC", "MULTI_INSTANCE", "PLANNED"),
                task("id-multiple-2", "domainEvents.task.multiple", "DYNAMIC", "MULTI_INSTANCE", "PLANNED"),
                task("id-multiple-3", "domainEvents.task.multiple", "DYNAMIC", "MULTI_INSTANCE", "PLANNED"));
        assertMilestoneReachedEvents("Test1EventReceived", "MandatoryTaskCompleted");

        // Produce event 2, which is expected to complete the 'optional' task, leading to another milestone being reached
        Test2Event event2 = Test2EventBuilder.createForProcessId(originProcessId)
                .taskIds("id-optional")
                .build();
        sendSync("topic.test2", event2);
        // Simulate domain event received before task planning to test if the completion condition is evaluated
        // correctly regardless of event ordering
        Thread.sleep(500);

        assertTasks(
                taskWithoutOriginTaskId("domainEvents.task.mandatory", "STATIC", "SINGLE_INSTANCE", "COMPLETED"),
                task(event2.getIdentity().getEventId(), "domainEvents.task.optional", "OBSERVED", "MULTI_INSTANCE", "COMPLETED"),
                task("id-multiple-1", "domainEvents.task.multiple", "DYNAMIC", "MULTI_INSTANCE", "PLANNED"),
                task("id-multiple-2", "domainEvents.task.multiple", "DYNAMIC", "MULTI_INSTANCE", "PLANNED"),
                task("id-multiple-3", "domainEvents.task.multiple", "DYNAMIC", "MULTI_INSTANCE", "PLANNED"));
        assertMilestoneReachedEvents("OptionalTaskCompleted");

        // Produce event 3, which is expected to complete the first two 'multiple' tasks
        Test3Event event3WithTwoTaskIds = Test3EventBuilder.createForProcessId(originProcessId)
                .taskIds("id-multiple-1", "id-multiple-2")
                .build();
        sendSync("topic.test3", event3WithTwoTaskIds);

        assertTasks(
                taskWithoutOriginTaskId("domainEvents.task.mandatory", "STATIC", "SINGLE_INSTANCE", "COMPLETED"),
                task(event2.getIdentity().getEventId(), "domainEvents.task.optional", "OBSERVED", "MULTI_INSTANCE", "COMPLETED"),
                task("id-multiple-1", "domainEvents.task.multiple", "DYNAMIC", "MULTI_INSTANCE", "COMPLETED"),
                task("id-multiple-2", "domainEvents.task.multiple", "DYNAMIC", "MULTI_INSTANCE", "COMPLETED"),
                task("id-multiple-3", "domainEvents.task.multiple", "DYNAMIC", "MULTI_INSTANCE", "PLANNED"));

        // Produce event 3 completing the remaining 'multiple' task, leading to process completion
        Test3Event event3WithThirdTaskId = Test3EventBuilder.createForProcessId(originProcessId)
                .taskIds("id-multiple-3")
                .build();
        sendSync("topic.test3", event3WithThirdTaskId);

        assertTasks(
                taskWithoutOriginTaskId("domainEvents.task.mandatory", "STATIC", "SINGLE_INSTANCE", "COMPLETED"),
                task(event2.getIdentity().getEventId(), "domainEvents.task.optional", "OBSERVED", "MULTI_INSTANCE", "COMPLETED"),
                task("id-multiple-1", "domainEvents.task.multiple", "DYNAMIC", "MULTI_INSTANCE", "COMPLETED"),
                task("id-multiple-2", "domainEvents.task.multiple", "DYNAMIC", "MULTI_INSTANCE", "COMPLETED"),
                task("id-multiple-3", "domainEvents.task.multiple", "DYNAMIC", "MULTI_INSTANCE", "COMPLETED"));

        assertMilestoneReachedEvents("AllMultipleTaskInstancesCompleted");
        assertProcessInstanceCompleted(originProcessId);
        assertProcessInstanceCompletedEvent(originProcessId);
    }

    @Override
    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }
}
