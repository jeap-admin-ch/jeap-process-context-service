package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.domain.processevent.ProcessEventQueryRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceRepository;
import ch.admin.bit.jeap.processcontext.domain.tx.Transactions;
import ch.admin.bit.jeap.processcontext.event.test1.Test1CreatingProcessInstanceEvent;
import ch.admin.bit.jeap.processcontext.event.test2.Test2Event;
import ch.admin.bit.jeap.processcontext.event.test4.Test4CreatingProcessInstanceEvent;
import ch.admin.bit.jeap.processcontext.testevent.Test1CreatingProcessInstanceEventBuilder;
import ch.admin.bit.jeap.processcontext.testevent.Test2EventBuilder;
import ch.admin.bit.jeap.processcontext.testevent.Test4CreatingProcessInstanceEventBuilder;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;

class ProcessInstanceCreatedByDomainMessageIT extends ProcessInstanceMockS3ITBase {

    private static final String PROCESS_TEMPLATE_NAME = "domainEventTriggersProcessInstantiation";
    @Autowired
    private ProcessInstanceRepository processInstanceRepository;
    @Autowired
    private ProcessEventQueryRepository processEventQueryRepository;
    @Autowired
    private Transactions transactions;

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void testProcessInstantiationWithDomainEventAndCorrelation() {

        assertThat(processInstanceRepository.findByOriginProcessIdLoadingMessages(originProcessId)).isNotPresent();

        // Send event that triggers the process instantiation
        sendTest1CreatingProcessInstanceEvent();

        assertProcessInstanceCreatedEvent(originProcessId, PROCESS_TEMPLATE_NAME);

        // Sent event that is correlated by the process data
        sendTest2Event();

        // When the second event got correlated to the process it will complete the process
        assertProcessInstanceCompleted(originProcessId);

        assertProcessEventCount(originProcessId, 2);
    }

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void testSendDomainEventAndProcessInstantiationAndCorrelation() {

        // Sent event that is correlated by the process data
        sendTest2Event();

        transactions.withinNewTransaction(() -> assertThat(processInstanceRepository.findByOriginProcessIdLoadingMessages(originProcessId)).isNotPresent());

        // Send event that triggers the process instantiation
        sendTest1CreatingProcessInstanceEvent();

        assertProcessInstanceCreatedEvent(originProcessId, PROCESS_TEMPLATE_NAME);

        // When the second event got correlated to the process it will complete the process
        assertProcessInstanceCompleted(originProcessId);

        assertProcessEventCount(originProcessId, 2);
    }

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void testProcessInstantiationWithDomainEventsAndCorrelation() {

        assertThat(processInstanceRepository.findByOriginProcessIdLoadingMessages(originProcessId)).isNotPresent();

        // Send event that triggers the process instantiation
        sendTest1CreatingProcessInstanceEvent();

        assertProcessInstanceCreatedEvent(originProcessId, PROCESS_TEMPLATE_NAME);

        // Send event that is correlated by process data and will complete the process by completing its only task
        sendTest2EventOtherProcessId();

        // Send event that tries to create the same process already created before by Test1Event
        sendTest4CreatingProcessInstanceEvent();

        assertProcessEventCount(originProcessId, 2); // process created and process completed

        transactions.withinNewTransaction(() ->
                {
                    final ProcessInstance processInstance = processInstanceRepository.findByOriginProcessIdLoadingMessages(originProcessId).get();
                    assertThat(processInstance.getMessageReferences()).hasSize(3); // test events 1, 2 and 4
                }
        );
    }

    private void sendTest1CreatingProcessInstanceEvent() {
        Test1CreatingProcessInstanceEvent event1 = Test1CreatingProcessInstanceEventBuilder.createForProcessId(originProcessId).build();
       sendSync("topic.test1creatingprocessinstance", event1);
    }

    private void sendTest4CreatingProcessInstanceEvent() {
        Test4CreatingProcessInstanceEvent event4 = Test4CreatingProcessInstanceEventBuilder.createForProcessId(originProcessId).build();
        sendSync("topic.test4creatingprocessinstance", event4);
    }

    private void sendTest2Event() {
        Test2Event event2 = Test2EventBuilder.createForProcessId(originProcessId).build();
        sendSync("topic.test2", event2);
    }

    private void sendTest2EventOtherProcessId() {
        Test2Event event2 = Test2EventBuilder.createForProcessId("other").build();
        sendSync("topic.test2", event2);
    }

    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }

    protected void assertProcessEventCount(String originProcessId, int count) {
        Awaitility.await()
                .pollInSameThread()
                .atMost(TIMEOUT)
                .until(() -> processEventQueryRepository.findByOriginProcessId(originProcessId),
                        hasSize(count));
    }

}
