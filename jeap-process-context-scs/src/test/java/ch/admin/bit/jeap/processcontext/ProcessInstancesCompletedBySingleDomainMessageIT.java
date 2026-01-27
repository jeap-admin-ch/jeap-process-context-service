package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.adapter.restapi.model.MessageDTO;
import ch.admin.bit.jeap.processcontext.event.processids.ProcessIdsEvent;
import ch.admin.bit.jeap.processcontext.event.test1.Test1Event;
import ch.admin.bit.jeap.processcontext.testevent.ProcessIdsEventBuilder;
import ch.admin.bit.jeap.processcontext.testevent.Test1EventBuilder;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static ch.admin.bit.jeap.processcontext.TaskInstanceAssertionDto.taskWithoutOriginTaskId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestPropertySource(properties =
        "jeap.processcontext.template.classpath-location-pattern=classpath:/process/templates/domain_event_to_multiple_process_instances.json")
class ProcessInstancesCompletedBySingleDomainMessageIT extends ProcessInstanceMockS3ITBase {

    private static final String PROCESS_TEMPLATE_NAME = "domainEventToMultipleProcessInstances";
    private static final String MANDATORY_TASK_LABEL = "domainEventToMultipleProcessInstances.task.mandatory";
    private static final String PROCESS_IDS_EVENT_TOPIC_NAME = "topic.processids";
    private static final String ORIGIN_PROCESS_ID_1 = "process1";
    private static final String ORIGIN_PROCESS_ID_2 = "process2";
    private static final String ORIGIN_PROCESS_ID_3 = "process3";

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void testCorrelationOfMoreThanOneProcessInstancesByOneDomainEvent() {

        // Start 3 new processes
        createAndAssertProcessInstance(ORIGIN_PROCESS_ID_1);
        createAndAssertProcessInstance(ORIGIN_PROCESS_ID_2);
        createAndAssertProcessInstance(ORIGIN_PROCESS_ID_3);

        // Complete the mandatory tasks of the process instances 1 and 3
        // by sending one domain event that correlates to both process instances.
        completeMandatoryTasksOfProcessInstances(ORIGIN_PROCESS_ID_1, ORIGIN_PROCESS_ID_3);

        // Assert that the mandatory tasks of process instances 1 and 3 have completed,
        // but not the mandatory task of process instance 2.
        assertTasks(ORIGIN_PROCESS_ID_1, taskWithoutOriginTaskId(MANDATORY_TASK_LABEL, "STATIC", "SINGLE_INSTANCE", "COMPLETED"));
        assertTasks(ORIGIN_PROCESS_ID_2, taskWithoutOriginTaskId(MANDATORY_TASK_LABEL, "STATIC", "SINGLE_INSTANCE", "PLANNED"));
        assertTasks(ORIGIN_PROCESS_ID_3, taskWithoutOriginTaskId(MANDATORY_TASK_LABEL, "STATIC", "SINGLE_INSTANCE", "COMPLETED"));

        // Assert that the domain event has been associated with both correlated process instances 1 and 3, but not with process instance 2.
        assertProcessIdsEvent(ORIGIN_PROCESS_ID_1);
        assertNoProcessIdsEvent(ORIGIN_PROCESS_ID_2);
        assertProcessIdsEvent(ORIGIN_PROCESS_ID_3);

        // Complete the mandatory task of the process instance 2
        // by sending a domain event that correlates to process instance 2.
        completeMandatoryTasksOfProcessInstances(ORIGIN_PROCESS_ID_2);

        // Assert that the mandatory task of process instance 2 has completed.
        assertTasks(ORIGIN_PROCESS_ID_2, taskWithoutOriginTaskId(MANDATORY_TASK_LABEL, "STATIC", "SINGLE_INSTANCE", "COMPLETED"));

        // Assert that the domain event has been associated with process instance 2.
        assertProcessIdsEvent(ORIGIN_PROCESS_ID_2);

        // Assert that all process instances have completed.
        assertProcessInstanceCompleted(ORIGIN_PROCESS_ID_1);
        assertProcessInstanceCompleted(ORIGIN_PROCESS_ID_2);
        assertProcessInstanceCompleted(ORIGIN_PROCESS_ID_3);
    }

    private void createAndAssertProcessInstance(String originProcessId) {
        Test1Event event1 = Test1EventBuilder.createForProcessId(originProcessId).build();
        sendSync("topic.test1", event1);
        assertProcessInstanceCreated(originProcessId, PROCESS_TEMPLATE_NAME);
        assertTasks(originProcessId, taskWithoutOriginTaskId(MANDATORY_TASK_LABEL, "STATIC", "SINGLE_INSTANCE", "PLANNED"));
    }

    private void completeMandatoryTasksOfProcessInstances(String... processIds) {
        ProcessIdsEvent processIdsEvent = ProcessIdsEventBuilder.create()
                .idempotenceId(String.join("-", processIds))
                .processIds(processIds)
                .build();
        sendSync(PROCESS_IDS_EVENT_TOPIC_NAME, processIdsEvent);
    }

    private void assertProcessIdsEvent(String originProcessId) {
        List<MessageDTO> processInstanceEvents = processInstanceController.getProcessInstanceByOriginProcessId(originProcessId).getMessages();
        assertEquals(2, processInstanceEvents.size());
        assertThat(processInstanceEvents.stream().map(MessageDTO::getName).toList())
                .containsExactlyInAnyOrder("Test1Event", "ProcessIdsEvent");
    }

    private void assertNoProcessIdsEvent(String originProcessId) {
        List<MessageDTO> processInstance1Events = processInstanceController.getProcessInstanceByOriginProcessId(originProcessId).getMessages();
        assertThat(processInstance1Events.stream().map(MessageDTO::getName).toList())
                .containsOnly("Test1Event");
    }

    @Override
    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }
}
