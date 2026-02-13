package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaTestConstants;
import ch.admin.bit.jeap.processcontext.testevent.Test1EventBuilder;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@TestPropertySource(properties =
        "jeap.processcontext.template.classpath-location-pattern=classpath:/process/templates/completions.json")
class TracingInformationInMessageIT extends ProcessInstanceMockS3ITBase {

    @Autowired
    private KafkaTemplate<AvroMessageKey, AvroMessage> kafkaTemplate;

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void messageTracing_whenMessageIsReceived_thenTracingInformationIsStored() {
        // Send message using the kafka template with tracing configuration active
        sendMessageUsingKafkaTemplateWithTracing();
        assertProcessInstanceCreated(originProcessId, "completions");

        // Wait until a message has been correlated to the process instance
        await().pollInSameThread()
                .until(() -> !getMessages(originProcessId).isEmpty());

        assertEquals(1, getMessages(originProcessId).size());
        assertTrue(hasText(getMessages(originProcessId).getFirst().getTraceId()));
    }

    private void sendMessageUsingKafkaTemplateWithTracing() {
        ProducerRecord<AvroMessageKey, AvroMessage> producerRecord = new ProducerRecord<>("topic.test1", Test1EventBuilder.createForProcessId(originProcessId)
                .taskIds("gotcha")
                .build());
        producerRecord.headers().add(KafkaTestConstants.TEST_PRODUCER_DISABLE_CONTRACT_CHECK_HEADER);
        kafkaTemplate.send(producerRecord);
    }

    @Override
    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }
}
