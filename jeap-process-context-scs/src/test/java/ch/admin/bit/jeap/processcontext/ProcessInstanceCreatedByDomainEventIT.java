package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.domain.message.MessageReferenceRepository;
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
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties =
        "jeap.processcontext.template.classpath-location-pattern=classpath:/process/templates/domain_event_triggers_process_instance_instantiation.json")
class ProcessInstanceCreatedByDomainEventIT extends ProcessInstanceMockS3ITBase {

    private static final String PROCESS_TEMPLATE_NAME = "domainEventTriggersProcessInstantiation";
    @Autowired
    private ProcessInstanceRepository processInstanceRepository;
    @Autowired
    private MessageReferenceRepository messageReferenceRepository;
    @Autowired
    private Transactions transactions;

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void testProcessInstantiationWithDomainEventAndCorrelation() {

        assertThat(processInstanceRepository.findByOriginProcessId(originProcessId)).isNotPresent();

        // Send event that triggers the process instantiation
        sendTest1CreatingProcessInstanceEvent();

        assertProcessInstanceCreated(originProcessId, PROCESS_TEMPLATE_NAME);

        // Sent event that is correlated by the process data
        sendTest2Event();

        // When the second event got correlated to the process it will complete the process
        assertProcessInstanceCompleted(originProcessId);

        assertProcessInstanceCompleted(originProcessId);
    }

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void testSendDomainEventAndProcessInstantiationAndCorrelation() {

        // Sent event that is correlated by the process data
        sendTest2Event();

        transactions.withinNewTransaction(() -> assertThat(processInstanceRepository.findByOriginProcessId(originProcessId)).isNotPresent());

        // Send event that triggers the process instantiation
        sendTest1CreatingProcessInstanceEvent();

        assertProcessInstanceCreated(originProcessId, PROCESS_TEMPLATE_NAME);

        // When the second event got correlated to the process it will complete the process
        assertProcessInstanceCompleted(originProcessId);

        assertProcessInstanceCompleted(originProcessId);
    }

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void testProcessInstantiationWithDomainEventsAndCorrelation() {

        assertThat(processInstanceRepository.findByOriginProcessId(originProcessId)).isNotPresent();

        // Send event that triggers the process instantiation
        sendTest1CreatingProcessInstanceEvent();

        assertProcessInstanceCreated(originProcessId, PROCESS_TEMPLATE_NAME);

        // Send event that is correlated by process data and will complete the process by completing its only task
        sendTest2EventOtherProcessId();

        // Send event that tries to create the same process already created before by Test1Event
        sendTest4CreatingProcessInstanceEvent();

        assertProcessInstanceCompleted(originProcessId);

        Awaitility.await()
                .pollInSameThread()
                .timeout(TIMEOUT)
                .until(() -> {
                    int correlatedEventCount = transactions.withinNewTransactionWithResult(() -> {
                        ProcessInstance processInstance = processInstanceRepository.findByOriginProcessId(originProcessId).orElseThrow();
                        return messageReferenceRepository.findByProcessInstanceId(processInstance.getId()).size();
                    });
                    return correlatedEventCount == 3; // test event 1, 2 and 4
                });
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

    @Override
    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }
}
