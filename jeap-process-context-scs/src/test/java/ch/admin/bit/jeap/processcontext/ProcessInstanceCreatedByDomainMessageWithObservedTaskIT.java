package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.TaskState;
import ch.admin.bit.jeap.processcontext.event.test1.TestCreatingProcessInstanceAndTaskEvent;
import ch.admin.bit.jeap.processcontext.testevent.TestCreatingProcessInstanceAndTaskEventBuilder;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessInstanceCreatedByDomainMessageWithObservedTaskIT extends ProcessInstanceMockS3ITBase {

    private static final String PROCESS_TEMPLATE_NAME = "domainEventTriggersProcessAndTaskInstantiation";
    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void testProcessInstantiationWithDomainEventAndCorrelation() {

        assertThat(processInstanceRepository.findByOriginProcessIdLoadingMessages(originProcessId)).isNotPresent();

        // Send event that triggers the process instantiation and creates an observed task
        sendTestCreatingProcessInstanceAndTaskEvent();

        assertProcessInstanceCreatedEvent(originProcessId, PROCESS_TEMPLATE_NAME);

        // When the second event got correlated to the process it will complete the process
        assertProcessInstanceCompleted(originProcessId);

        assertProcessEventCount(originProcessId, 2);

        Optional<ProcessInstance> processInstance = processInstanceRepository.findByOriginProcessIdLoadingMessages(originProcessId);

        assertThat(processInstance).isPresent();
        assertThat(processInstance.get().getTasks()).hasSize(1);
        assertThat(processInstance.get().getTasks().get(0).getState()).isEqualTo(TaskState.COMPLETED);
    }

    private void sendTestCreatingProcessInstanceAndTaskEvent() {
        TestCreatingProcessInstanceAndTaskEvent event1 = TestCreatingProcessInstanceAndTaskEventBuilder.createForProcessId(originProcessId).build();
        sendSync("topic.testcreatingprocessinstanceandtask", event1);
    }

    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }

    protected void assertProcessEventCount(String originProcessId, int count) {
        // JEAP-6536 TODO
    }

}
