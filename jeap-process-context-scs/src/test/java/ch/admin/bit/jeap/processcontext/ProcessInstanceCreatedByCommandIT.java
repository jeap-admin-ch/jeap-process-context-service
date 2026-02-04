package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.command.test.createprocessinstance.TestCreateProcessInstanceCommand;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceRepository;
import ch.admin.bit.jeap.processcontext.domain.tx.Transactions;
import ch.admin.bit.jeap.processcontext.testevent.TestCreateProcessInstanceCommandBuilder;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties =
        "jeap.processcontext.template.classpath-location-pattern=classpath:/process/templates/command_triggers_process_instance_instantiation.json")
class ProcessInstanceCreatedByCommandIT extends ProcessInstanceMockS3ITBase {

    private static final String PROCESS_TEMPLATE_NAME = "commandTriggersProcessInstantiation";
    @Autowired
    private ProcessInstanceRepository processInstanceRepository;
    @Autowired
    private Transactions transactions;

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void testProcessInstantiationWithDomainEventAndCorrelation() {

        assertThat(processInstanceRepository.findByOriginProcessId(originProcessId)).isNotPresent();

        // Send command that triggers the process instantiation
        sendTestCreateProcessInstanceCommand();

        assertProcessInstanceCreated(originProcessId, PROCESS_TEMPLATE_NAME);
        assertProcessInstanceCompleted(originProcessId);
    }

    private void sendTestCreateProcessInstanceCommand() {
        TestCreateProcessInstanceCommand command = TestCreateProcessInstanceCommandBuilder.createForProcessId(originProcessId).build();
        sendSync("topic.testcreateprocessinstance", command);
    }

    @Override
    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }
}
