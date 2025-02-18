package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.command.CreateProcessInstanceCommandBuilder;
import ch.admin.bit.jeap.processcontext.command.process.instance.create.CreateProcessInstanceCommand;
import ch.admin.bit.jeap.processcontext.command.process.instance.create.ProcessData;
import ch.admin.bit.jeap.processcontext.domain.message.MessageQueryRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessState;
import ch.admin.bit.jeap.processcontext.domain.tx.Transactions;
import ch.admin.bit.jeap.processcontext.event.test2.Test2Event;
import ch.admin.bit.jeap.processcontext.testevent.Test2EventBuilder;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import com.fasterxml.uuid.Generators;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessInstanceCreatedByCreateProcessInstanceCommandIT extends ProcessInstanceMockS3ITBase {

    private static final String COMMAND_TOPIC_NAME = "create-process-instance";
    private static final String PROCESS_TEMPLATE_NAME = "domainEventCorrelatedByProcessDataEarlyCorrelation";
    private static final String CORRELATION_EVENT_DATA_KEY = "correlationEventDataKey";
    private static final String CORRELATION_EVENT_DATA_VALUE = "correlationEventDataValue";
    private static final String CORRELATION_EVENT_DATA_ROLE = "correlationEventDataRole";

    @Autowired
    protected ProcessInstanceRepository processInstanceRepository;
    @Autowired
    protected MessageQueryRepository messageQueryRepository;
    @Autowired
    private Transactions transactions;

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void testCreateProcessInstanceCommandAndSendDomainEvent() {

        originProcessId = UUID.randomUUID().toString();

        assertThat(processInstanceRepository.findByOriginProcessIdLoadingMessages(originProcessId)).isNotPresent();

        sendCreateProcessInstanceCommand();

        // Assert that the process instance is created.
        assertProcessInstanceCreated();

        transactions.withinNewTransaction(() ->
                {
                    final ProcessInstance processInstance = processInstanceRepository.findByOriginProcessIdLoadingMessages(originProcessId).get();
                    assertThat(processInstance.getState()).isEqualTo(ProcessState.STARTED);
                    assertThat(processInstance.getProcessData()).hasSize(1);
                    final ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessData processData = processInstance.getProcessData().iterator().next();
                    assertThat(processData.getKey()).isEqualTo(CORRELATION_EVENT_DATA_KEY);
                    assertThat(processData.getValue()).isEqualTo(CORRELATION_EVENT_DATA_VALUE);
                    assertThat(processData.getRole()).isEqualTo(CORRELATION_EVENT_DATA_ROLE);
                }
        );

        sendTest2Event();

        assertProcessInstanceCompleted(originProcessId);
    }

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void testSendDomainEventAndCreateProcessInstanceCommand() throws Exception {

        Test2Event test2Event = sendTest2Event();

        Awaitility.await()
                .atMost(TIMEOUT)
                .until(() ->
                        messageQueryRepository.findByMessageNameAndIdempotenceId(
                                        test2Event.getType().getName(), test2Event.getIdentity().getIdempotenceId())
                                .isPresent());
        Thread.sleep(2000);

        transactions.withinNewTransaction(() ->
                assertThat(processInstanceRepository.findByOriginProcessIdLoadingMessages(originProcessId)).isNotPresent());

        sendCreateProcessInstanceCommand();

        // Assert that the process instance is created.
        assertProcessInstanceCreated();

        assertProcessInstanceCompleted(originProcessId);
    }

    private void sendCreateProcessInstanceCommand() {
        CreateProcessInstanceCommand createProcessInstanceCommand = CreateProcessInstanceCommandBuilder.create()
                .systemName("test")
                .serviceName("test")
                .processId(originProcessId)
                .processTemplateName(PROCESS_TEMPLATE_NAME)
                .processData(List.of(new ProcessData(CORRELATION_EVENT_DATA_KEY, CORRELATION_EVENT_DATA_VALUE, CORRELATION_EVENT_DATA_ROLE)))
                .idempotenceId(Generators.timeBasedEpochGenerator().generate().toString())
                .build();
       sendSync(COMMAND_TOPIC_NAME, createProcessInstanceCommand);
    }

    private Test2Event sendTest2Event() {
        Test2Event event2 = Test2EventBuilder.createForProcessId(originProcessId).build();
        sendSync("topic.test2", event2);
        return event2;
    }

    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }

    private void assertProcessInstanceCreated() {
        Awaitility.await()
                .pollInSameThread()
                .atMost(TIMEOUT)
                .until(() -> processInstanceRepository.findByOriginProcessIdLoadingMessages(originProcessId).isPresent());
    }
}
