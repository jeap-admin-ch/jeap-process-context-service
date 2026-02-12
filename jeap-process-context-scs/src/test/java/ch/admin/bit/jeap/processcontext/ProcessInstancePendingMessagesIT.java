package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.domain.processinstance.PendingMessageRepository;
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
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("SameParameterValue")
@TestPropertySource(properties =
        "jeap.processcontext.template.classpath-location-pattern=classpath:/process/templates/pending_messages.json")
@Slf4j
class ProcessInstancePendingMessagesIT extends ProcessInstanceMockS3ITBase {

    @Autowired
    private PendingMessageRepository pendingMessageRepository;

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void whenDynamicTaskPlannedByEvent_whenTaskMessagesBufferedAsPendingMessages_thenAllMessagesAreProcessedWhenInstanceIsCreated() {
        // Buffer some pending messages before process creation
        int taskCount = 20;
        var allOriginTaskIds = new HashSet<>();
        for (int i = 0; i < taskCount / 2; i++) {
            var originTaskIds = Set.of("task-id-1-" + i, "task-id-2-" + i);
            sendTest1Event(originTaskIds);
            sendTest2Event(originTaskIds);
            allOriginTaskIds.addAll(originTaskIds);
        }

        assertPendingMessageCount(20);

        // create process
        sendTest4Event();
        assertProcessInstanceCreated(originProcessId, "pending_messages");
        assertPendingMessageCount(0); // all pending messages should be deleted after being processed
        Awaitility.await().pollInSameThread()
                .untilAsserted(() -> assertThat(processInstanceController.getProcessInstanceByOriginProcessId(originProcessId).getTasks())
                        .hasSize(taskCount) // taskCount times raceCarRefuel
                        .extracting("originTaskId")
                        .containsExactlyInAnyOrderElementsOf(allOriginTaskIds));

        // complete process
        sendTest3Event();

        assertProcessInstanceCompleted(originProcessId);
    }

    private void assertPendingMessageCount(int expected) {
        Awaitility.await().untilAsserted(() ->
                assertThat(pendingMessageRepository.findByOriginProcessId(originProcessId))
                        .hasSize(expected));
    }

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void whenDynamicTaskPlannedByEvent_whenTaskMessagesBufferedAsPendingMessagesInAnyOrder_thenAllMessagesAreProcessedWhenInstanceIsCreated() {
        // Buffer some pending messages before process creation
        int taskCount = 20;
        var allOriginTaskIds = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            var originTaskIds = Set.of("task-id-1-" + i, "task-id-2-" + i);
            // send task completion event before task creation event to simulate out-of-order message arrival
            sendTest2Event(originTaskIds);
            sendTest1Event(originTaskIds);
            allOriginTaskIds.addAll(originTaskIds);
        }

        assertPendingMessageCount(10);

        // create process
        sendTest4Event();
        assertProcessInstanceCreated(originProcessId, "pending_messages");
        assertPendingMessageCount(0); // all pending messages should be deleted after being processed

        // Create some tasks by messages sent after process creation
        for (int i = 5; i < taskCount / 2; i++) {
            var originTaskIds = Set.of("task-id-1-" + i, "task-id-2-" + i);
            sendTest2Event(originTaskIds);
            sendTest1Event(originTaskIds);
            allOriginTaskIds.addAll(originTaskIds);
        }
        Awaitility.await().pollInSameThread()
                .untilAsserted(() -> assertThat(processInstanceController.getProcessInstanceByOriginProcessId(originProcessId).getTasks())
                        .hasSize(taskCount) // taskCount times raceCarRefuel
                        .extracting("originTaskId")
                        .containsExactlyInAnyOrderElementsOf(allOriginTaskIds));

        // complete process
        sendTest3Event();
        assertProcessInstanceCompleted(originProcessId);
    }

    private void sendTest1Event(Set<String> taskIds) {
        Test1Event event1 = Test1EventBuilder.createForProcessId(originProcessId)
                .taskIds(taskIds)
                .build();
        sendSync("topic.test1", event1);
    }

    private void sendTest2Event(Set<String> taskIds) {
        Test2Event event2 = Test2EventBuilder.createForProcessId(originProcessId)
                .taskIds(taskIds)
                .build();
        sendSync("topic.test2", event2);
    }

    private void sendTest3Event() {
        Test3Event event3 = Test3EventBuilder.createForProcessId(originProcessId).build();
        sendSync("topic.test3", event3);
    }

    private void sendTest4Event() {
        Test4Event event4 = Test4EventBuilder.createForProcessId(originProcessId).build();
        sendSync("topic.test4", event4);
    }

    @Override
    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }
}
