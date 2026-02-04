package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.adapter.restapi.model.ProcessInstanceDTO;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessState;
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
import org.springframework.test.context.TestPropertySource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("SameParameterValue")
@TestPropertySource(properties =
        "jeap.processcontext.template.classpath-location-pattern=classpath:/process/templates/dynamic_task_completed_by_event*.json")
@Slf4j
class ProcessInstanceTaskCompletedByEventIT extends ProcessInstanceMockS3ITBase {

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void whenDynamicTaskPlannedByEvent_whenCompletionEventIsReceivedAfterPlannedEvent_thenExpectTaskAndProcessToComplete() {
        sendTest3Event();
        assertProcessInstanceCreated(originProcessId, "dynamic_task_completed_by_event");
        assertThat(processInstanceController.getProcessInstanceByOriginProcessId(originProcessId).getState())
                .isEqualTo(ProcessState.STARTED.name());

        sendTest1Event();
        awaitEvent("Test1Event");
        sendTest2Event();

        assertProcessInstanceCompleted(originProcessId);
    }

    private void awaitEvent(String eventName) {
        Awaitility.await().pollInSameThread()
                .untilAsserted(() -> {
                    ProcessInstanceDTO dto = processInstanceController.getProcessInstanceByOriginProcessId(originProcessId);
                    assertThat(dto.getMessages())
                            .extracting("name")
                            .contains(eventName);
                });
    }

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void whenDynamicTaskPlannedByEvent_whenCompletionEventIsReceivedBeforePlannedEvent_thenExpectTaskAndProcessToComplete() {
        sendTest3Event();
        assertProcessInstanceCreated(originProcessId, "dynamic_task_completed_by_event");
        assertThat(processInstanceController.getProcessInstanceByOriginProcessId(originProcessId).getState())
                .isEqualTo(ProcessState.STARTED.name());

        sendTest2Event();
        awaitEvent("Test2Event");
        sendTest1Event();

        assertProcessInstanceCompleted(originProcessId);
    }

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void whenDynamicTaskPlannedByEvent_multiInstanceTask_whenCompletionEventIsReceivedAfterPlannedEvent_thenExpectTaskAndProcessToComplete() {
        sendTest4Event();
        assertProcessInstanceCreated(originProcessId, "dynamic_task_completed_by_event_multi_instance");
        assertThat(processInstanceController.getProcessInstanceByOriginProcessId(originProcessId).getState())
                .isEqualTo(ProcessState.STARTED.name());

        var originTaskIds = Set.of("task-id-1", "task-id-2");
        sendTest1Event(originTaskIds);
        awaitEvent("Test1Event");
        sendTest2Event(originTaskIds);

        assertProcessInstanceCompleted(originProcessId);
        ProcessInstanceDTO dto = processInstanceController.getProcessInstanceByOriginProcessId(originProcessId);
        assertThat(dto.getTasks())
                .hasSize(2) // 2x raceCarRefuel
                .extracting("originTaskId")
                .containsExactlyInAnyOrderElementsOf(originTaskIds);
    }

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void whenDynamicTaskPlannedByEvent_multiInstanceTask_whenCompletionEventIsReceivedBeforePlannedEvent_thenExpectTaskAndProcessToComplete() {
        sendTest4Event();
        assertProcessInstanceCreated(originProcessId, "dynamic_task_completed_by_event_multi_instance");
        assertThat(processInstanceController.getProcessInstanceByOriginProcessId(originProcessId).getState())
                .isEqualTo(ProcessState.STARTED.name());

        var originTaskIds = Set.of("task-id-1", "task-id-2");
        sendTest2Event(originTaskIds);
        awaitEvent("Test2Event");
        sendTest1Event(originTaskIds);

        assertProcessInstanceCompleted(originProcessId);
        ProcessInstanceDTO dto = processInstanceController.getProcessInstanceByOriginProcessId(originProcessId);
        assertThat(dto.getTasks())
                .hasSize(2) // 2x raceCarRefuel
                .extracting("originTaskId")
                .containsExactlyInAnyOrderElementsOf(originTaskIds);
    }

    private void sendTest1Event() {
        sendTest1Event(Set.of());
    }

    private void sendTest1Event(Set<String> taskIds) {
        Test1Event event1 = Test1EventBuilder.createForProcessId(originProcessId)
                .taskIds(taskIds)
                .build();
        sendSync("topic.test1", event1);
    }

    private void sendTest2Event() {
        sendTest2Event(Set.of());
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
