package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaTestConstants;
import ch.admin.bit.jeap.processcontext.adapter.restapi.ProcessInstanceController;
import ch.admin.bit.jeap.processcontext.adapter.restapi.model.ProcessInstanceDTO;
import ch.admin.bit.jeap.processcontext.testevent.Test1EventBuilder;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
class TracingInformationInMessageIT extends ProcessInstanceMockS3ITBase {

    @Autowired
    private ProcessInstanceController processInstanceController;

    @Autowired
    private KafkaTemplate kafkaTemplate;

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void messageTracing_whenMessageIsReceived_thenTracingInformationIsStored() {
        // Start a new process
        createProcessInstanceFromTemplate("completions");

        // Over the kafka template will the tracing configuration active
        sendMessageAsyncOverKafkaTemplate();

        // Wait until a message is found sent to the process instance
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInSameThread()
                .until(() -> !processInstanceController.getProcessInstanceByOriginProcessId(originProcessId).getMessages().isEmpty());
        ProcessInstanceDTO processInstance = processInstanceController.getProcessInstanceByOriginProcessId(originProcessId);
        assertEquals(1, processInstance.getMessages().size());
        assertTrue(hasText(processInstance.getMessages().get(0).getTraceId()));

    }

    private void sendMessageAsyncOverKafkaTemplate() {
        ProducerRecord<AvroMessageKey, AvroMessage> producerRecord = new ProducerRecord("topic.test1", Test1EventBuilder.createForProcessId(originProcessId)
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
