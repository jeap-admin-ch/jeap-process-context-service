package ch.admin.bit.jeap.processcontext.adapter.kafka.command.consumer;

import ch.admin.bit.jeap.processcontext.adapter.kafka.TopicConfiguration;
import ch.admin.bit.jeap.processcontext.command.CreateProcessInstanceCommandBuilder;
import ch.admin.bit.jeap.processcontext.command.process.instance.create.CreateProcessInstanceCommand;
import ch.admin.bit.jeap.processcontext.command.process.instance.create.ProcessData;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import ch.admin.bit.jeap.processcontext.adapter.kafka.KafkaAdapterIntegrationTestBase;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;

class CreateProcessInstanceCommandConsumerIT extends KafkaAdapterIntegrationTestBase {

    @Autowired
    private TopicConfiguration topicConfiguration;

    @Test
    void whenCreateProcessInstanceCommandProduced_expectForwardedToListener() {
        // given
        CreateProcessInstanceCommand command = CreateProcessInstanceCommandBuilder.create()
                .systemName("TEST")
                .serviceName("service")
                .processId("originProcessId")
                .processTemplateName("processTemplateName")
                .processData(List.of(new ProcessData("key", "value", "role")))
                .idempotenceId("idempotenceId1")
                .build();

        // when
        kafkaTemplate.send(topicConfiguration.getCreateProcessInstance(), command);

        // then
        Mockito.verify(processInstanceService, Mockito.timeout(TEST_TIMEOUT)).createProcessInstance(
                command.getProcessId(),
                command.getPayload().getProcessTemplateName(),
                command.getPayload().getProcessData().stream().map(pd -> new ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessData(pd.getKey(), pd.getValue(), pd.getRole())).collect(Collectors.toSet()));
        Mockito.verifyNoMoreInteractions(processInstanceService);
    }

    @Test
    void whenCreateProcessInstanceCommandProduced_withoutProcessData_expectForwardedToListener() {
        // given
        CreateProcessInstanceCommand command = CreateProcessInstanceCommandBuilder.create()
                .systemName("TEST")
                .serviceName("service")
                .processId("originProcessId")
                .processTemplateName("processTemplateName")
                .idempotenceId("idempotenceId1")
                .build();

        // when
        kafkaTemplate.send(topicConfiguration.getCreateProcessInstance(), command);

        // then
        Mockito.verify(processInstanceService, Mockito.timeout(TEST_TIMEOUT)).createProcessInstance(
                command.getProcessId(),
                command.getPayload().getProcessTemplateName(),
                emptySet());
        Mockito.verifyNoMoreInteractions(processInstanceService);
    }

}
