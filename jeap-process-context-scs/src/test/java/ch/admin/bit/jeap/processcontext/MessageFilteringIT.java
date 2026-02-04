package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceRepository;
import ch.admin.bit.jeap.processcontext.event.test1.Test1CreatingProcessInstanceEvent;
import ch.admin.bit.jeap.processcontext.testevent.Test1CreatingProcessInstanceEventBuilder;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties =
        "jeap.processcontext.template.classpath-location-pattern=classpath:/process/templates/domain_event_triggers_process_instance_instantiation.json")
@ActiveProfiles("message-filter")
class MessageFilteringIT extends ProcessInstanceMockS3ITBase {

    private static final String PROCESS_TEMPLATE_NAME = "domainEventTriggersProcessInstantiation";
    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void testProcessInstantiationWithDomainEventFiltered_processInstanceNotCreated() {

        assertThat(processInstanceRepository.findByOriginProcessId(originProcessId)).isNotPresent();

        // Send event that triggers the process instantiation
        sendTest1CreatingProcessInstanceEvent("taskIdToFilter");

        assertThat(processInstanceRepository.findByOriginProcessId(originProcessId)).isNotPresent();

    }

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void testProcessInstantiationWithDomainEventNotFiltered_processInstanceCreated() {

        assertThat(processInstanceRepository.findByOriginProcessId(originProcessId)).isNotPresent();

        // Send event that triggers the process instantiation
        sendTest1CreatingProcessInstanceEvent("dontFilter");

        assertProcessInstanceCreated(originProcessId, PROCESS_TEMPLATE_NAME);

    }

    private void sendTest1CreatingProcessInstanceEvent(String taskId) {
        Test1CreatingProcessInstanceEvent event1 = Test1CreatingProcessInstanceEventBuilder
                .createForProcessId(originProcessId)
                .taskIds(taskId)
                .build();
        sendSync("topic.test1creatingprocessinstance", event1);
    }

    @Override
    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }

}
