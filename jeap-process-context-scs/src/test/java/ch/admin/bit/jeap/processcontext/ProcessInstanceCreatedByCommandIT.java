package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.command.test.createprocessinstance.TestCreateProcessInstanceCommand;
import ch.admin.bit.jeap.processcontext.domain.processevent.ProcessEventQueryRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceRepository;
import ch.admin.bit.jeap.processcontext.domain.tx.Transactions;
import ch.admin.bit.jeap.processcontext.testevent.TestCreateProcessInstanceCommandBuilder;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;

class ProcessInstanceCreatedByCommandIT extends ProcessInstanceMockS3ITBase {

    private static final String PROCESS_TEMPLATE_NAME = "commandTriggersProcessInstantiation";
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

        // Send command that triggers the process instantiation
        sendTestCreateProcessInstanceCommand();

        assertProcessInstanceCreatedEvent(originProcessId, PROCESS_TEMPLATE_NAME);
        assertProcessEventCount(originProcessId, 2); // 2 = created + completed events
        assertProcessInstanceCompleted(originProcessId);
    }

    private void sendTestCreateProcessInstanceCommand() {
        TestCreateProcessInstanceCommand command = TestCreateProcessInstanceCommandBuilder.createForProcessId(originProcessId).build();
        sendSync("topic.testcreateprocessinstance", command);
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