package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessState;
import ch.admin.bit.jeap.processcontext.event.test1.Test1Event;
import ch.admin.bit.jeap.processcontext.event.test2.Test2Event;
import ch.admin.bit.jeap.processcontext.event.test3.Test3Event;
import ch.admin.bit.jeap.processcontext.testevent.Test1EventBuilder;
import ch.admin.bit.jeap.processcontext.testevent.Test2EventBuilder;
import ch.admin.bit.jeap.processcontext.testevent.Test3EventBuilder;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("SameParameterValue")
@TestPropertySource(properties =
        "jeap.processcontext.template.classpath-location-pattern=classpath:/process/templates/dynamic_task_completed_by_event.json")
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
        sendTest2Event();

        assertProcessInstanceCompleted(originProcessId);
    }

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void whenDynamicTaskPlannedByEvent_whenCompletionEventIsReceivedBeforePlannedEvent_thenExpectTaskAndProcessToComplete() {
        sendTest3Event();
        assertProcessInstanceCreated(originProcessId, "dynamic_task_completed_by_event");
        assertThat(processInstanceController.getProcessInstanceByOriginProcessId(originProcessId).getState())
                .isEqualTo(ProcessState.STARTED.name());

        sendTest2Event();
        sendTest1Event();

        assertProcessInstanceCompleted(originProcessId);
    }

    private void sendTest1Event() {
        Test1Event event1 = Test1EventBuilder.createForProcessId(originProcessId)
                .taskIds("test")
                .build();
       sendSync("topic.test1", event1);
    }

    private void sendTest2Event() {
        Test2Event event2 = Test2EventBuilder.createForProcessId(originProcessId)
                .build();
        sendSync("topic.test2", event2);
    }

    private void sendTest3Event() {
        Test3Event event3 = Test3EventBuilder.createForProcessId(originProcessId).build();
        sendSync("topic.test3", event3);
    }

    @Override
    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }
}
