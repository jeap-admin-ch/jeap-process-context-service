package ch.admin.bit.jeap.processcontext.repository.template.json;

import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.messaging.model.MessagePayload;
import ch.admin.bit.jeap.messaging.model.MessageReferences;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.MessageReference;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.plugin.api.event.MessageCorrelationProvider;
import ch.admin.bit.jeap.processcontext.plugin.api.event.PayloadExtractor;
import ch.admin.bit.jeap.processcontext.plugin.api.event.ProcessInstantiationCondition;
import ch.admin.bit.jeap.processcontext.plugin.api.event.ReferenceExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class JsonProcessTemplateConsumerContractValidatorTest {

    private ContractsValidator contractsValidator;
    private JsonProcessTemplateConsumerContractValidator validator;

    @BeforeEach
    void setUp() {
        contractsValidator = mock(ContractsValidator.class);
        validator = new JsonProcessTemplateConsumerContractValidator(contractsValidator);
    }

    @Test
    void validateContract_shouldCallEnsureConsumerContractForEachMessageReference() {
        ProcessTemplate processTemplate = mock(ProcessTemplate.class);
        MessageCorrelationProvider<Message> correlationProvider = mock(MessageCorrelationProvider.class);
        PayloadExtractor<MessagePayload> payloadExtractor = mock(PayloadExtractor.class);
        ReferenceExtractor<MessageReferences> referenceExtractor = mock(ReferenceExtractor.class);
        ProcessInstantiationCondition<Message> processInstantiationCondition = mock(ProcessInstantiationCondition.class);

        MessageReference messageReference1 = MessageReference.builder()
                .messageName("TestEvent1")
                .topicName("test.topic1")
                .correlationProvider(correlationProvider)
                .payloadExtractor(payloadExtractor)
                .referenceExtractor(referenceExtractor)
                .processInstantiationCondition(processInstantiationCondition)
                .build();

        MessageReference messageReference2 = MessageReference.builder()
                .messageName("TestEvent2")
                .topicName("test.topic2")
                .correlationProvider(correlationProvider)
                .payloadExtractor(payloadExtractor)
                .referenceExtractor(referenceExtractor)
                .processInstantiationCondition(processInstantiationCondition)
                .build();

        when(processTemplate.getMessageReferences()).thenReturn(List.of(messageReference1, messageReference2));

        validator.validateContract(processTemplate);

        verify(contractsValidator, times(1)).ensureConsumerContract("TestEvent1", "test.topic1");
        verify(contractsValidator, times(1)).ensureConsumerContract("TestEvent2", "test.topic2");
    }
}
