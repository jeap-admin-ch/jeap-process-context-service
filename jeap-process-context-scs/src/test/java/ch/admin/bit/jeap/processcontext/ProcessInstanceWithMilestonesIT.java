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

class ProcessInstanceWithMilestonesIT extends ProcessInstanceMockS3ITBase {

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void processWithMilestoneConditions_whenMilestonesAreReached_thenExpectMilestoneReachedEventsToBeProduced() {
        // Start a new process
        String processTemplateName = "twoMilestones";
        createProcessInstanceFromTemplate(processTemplateName);
        assertProcessInstanceCreatedEvent(originProcessId, processTemplateName);

        // Plan all tasks with test1Event
        sendTest1Event("test");


        // Check process state, now tasks shall be planned
        assertTasks(
                task(null, "twoMilestones.task.task1", "STATIC", "SINGLE_INSTANCE", "PLANNED"),
                task("test", "twoMilestones.task.task2", "DYNAMIC", "SINGLE_INSTANCE", "PLANNED"),
                task("test", "twoMilestones.task.task3", "DYNAMIC", "SINGLE_INSTANCE", "PLANNED")
        );

        // Complete task1
        sendTest2Event("test");

        // Check (single) milestone reached event published after task 1 is complete
        assertMilestoneReachedEvents("Task1Completed");

        // Complete task2
        sendTest3Event("test");

        // Check milestone reached event published after task 1 is complete
        assertMilestoneReachedEvents("Task1And2Completed");

        // Complete task3, expect process completion
        sendTest4Event("test");

        // Check that process completes after all task completed
        assertProcessInstanceCompleted(originProcessId);
        assertProcessInstanceCompletedEvent(originProcessId);
    }

    @Override
    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }

    private void sendTest1Event(String... taskIds) {
        Test1Event event = Test1EventBuilder.createForProcessId(originProcessId).taskIds(taskIds).build();
        sendSync("topic.test1", event);
    }

    private void sendTest2Event(String... taskIds) {
        Test2Event event = Test2EventBuilder.createForProcessId(originProcessId).taskIds(taskIds).build();
        sendSync("topic.test2", event);
    }

    private void sendTest3Event(String... taskIds) {
        Test3Event event = Test3EventBuilder.createForProcessId(originProcessId).taskIds(taskIds).build();
        sendSync("topic.test3", event);
    }

    private void sendTest4Event(String... taskIds) {
        Test4Event event = Test4EventBuilder.createForProcessId(originProcessId).taskIds(taskIds).build();
        sendSync("topic.test4", event);
    }
}
