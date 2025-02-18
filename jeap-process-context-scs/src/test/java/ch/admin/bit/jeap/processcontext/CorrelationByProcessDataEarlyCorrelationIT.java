package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.event.test1.Test1Event;
import ch.admin.bit.jeap.processcontext.event.test2.Test2Event;
import ch.admin.bit.jeap.processcontext.testevent.Test1EventBuilder;
import ch.admin.bit.jeap.processcontext.testevent.Test2EventBuilder;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import org.junit.jupiter.api.Test;

class CorrelationByProcessDataEarlyCorrelationIT extends ProcessInstanceMockS3ITBase {

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void testEarlyCorrelationByProcessData() {

        // Start a new process
        String processTemplateName = "domainEventCorrelatedByProcessDataEarlyCorrelation";
        createProcessInstanceFromTemplate(processTemplateName);
        assertProcessInstanceCreatedEvent(originProcessId, processTemplateName);

        // Send event that adds process data to the process
        sendTest1Event();

        // Sent event that is correlated by the process data added to the process by the first event
        sendTest2Event();

        // When the second event got correlated to the process it will complete the process
        assertProcessInstanceCompleted(originProcessId);
    }

    private void sendTest1Event() {
        Test1Event event1 = Test1EventBuilder.createForProcessId(originProcessId).build();
       sendSync("topic.test1", event1);
    }

    private void sendTest2Event() {
        Test2Event event2 = Test2EventBuilder.createForProcessId(originProcessId).build();
        sendSync("topic.test2", event2);
    }

    @Override
    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }
}
