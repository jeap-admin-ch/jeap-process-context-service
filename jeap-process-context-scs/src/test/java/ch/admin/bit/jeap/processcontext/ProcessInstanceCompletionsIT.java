package ch.admin.bit.jeap.processcontext;

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

@TestPropertySource(properties =
        "jeap.processcontext.template.classpath-location-pattern=classpath:/process/templates/completions.json")
@Slf4j
class ProcessInstanceCompletionsIT extends ProcessInstanceMockS3ITBase {

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void processCompletion_whenTest1EventFulfillingCompletionConditionReceived_thenCompleted() {
        // send Test1Event that creates the process and fulfills the completion condition
        sendTest1Event("gotcha");
        assertProcessInstanceCreated(originProcessId, "completions");

        // Check that process completes after receiving Test1Event fulfilling the completion condition
        assertProcessInstanceCompleted(originProcessId);
    }

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void processCompletion_whenTest2EventReceived_thenCompleted() {
        // send Test2Event that creates the process and has been registered for process completion
        sendTest2Event("complete");
        assertProcessInstanceCreated(originProcessId, "completions");

        // Check that process completes after receiving Test2Event
        assertProcessInstanceCompleted(originProcessId);
    }

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void processCompletion_whenAllTasksCompleted_thenCompleted() {
        // send Test3Event that completes the single mandatory task
        sendTest3Event("complete");
        assertProcessInstanceCreated(originProcessId, "completions");

        // Check that process completes after receiving Test2Event
        sendTest2Event("complete");
        assertProcessInstanceCompleted(originProcessId);
    }


    private void sendTest1Event(String s) {
        Test1Event event1 = Test1EventBuilder.createForProcessId(originProcessId)
                .taskIds(s)
                .build();
       sendSync("topic.test1", event1);
    }

    private void sendTest2Event(String s) {
        Test2Event event2 = Test2EventBuilder.createForProcessId(originProcessId)
                .objectId(s)
                .build();
        sendSync("topic.test2", event2);
    }

    private void sendTest3Event(String s) {
        Test3Event event3 = Test3EventBuilder.createForProcessId(originProcessId)
                .taskIds()
                .build();
        sendSync("topic.test3", event3);
    }

    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }
}
