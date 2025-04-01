package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.message.MessageData;
import ch.admin.bit.jeap.processcontext.domain.message.MessageRepository;
import ch.admin.bit.jeap.processcontext.domain.message.OriginTaskId;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceRepository;
import ch.admin.bit.jeap.processcontext.event.test1.Test1Event;
import ch.admin.bit.jeap.processcontext.testevent.Test1EventBuilder;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import com.fasterxml.uuid.Generators;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.ZonedDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationByProcessDataLateCorrelationIT extends ProcessInstanceMockS3ITBase {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void testLateCorrelationByProcessData() {

        // Start a new process
        String processTemplateName = "domainEventCorrelatedByProcessDataEarlyCorrelation";
        createProcessInstanceFromTemplate(processTemplateName);
        assertProcessInstanceCreatedEvent(originProcessId, processTemplateName);

        // event which will be correlated
        saveEvent("Test2Event", processTemplateName, "correlationEventDataKey", "correlationEventDataValue", "correlationEventDataRole");

        // events that will not be correlated
        saveEvent("Test2Event", processTemplateName, "correlationEventDataKeyOther", "correlationEventDataValue", "correlationEventDataRole");
        saveEvent("Test2Event", processTemplateName, "correlationEventDataKey", "correlationEventDataValueOther", "correlationEventDataRole");
        saveEvent("Test2Event", processTemplateName, "correlationEventDataKey", "correlationEventDataValue", "correlationEventDataRoleOther");
        saveEvent("Test2Event", "templateNameOther", "correlationEventDataKey", "correlationEventDataValue", "correlationEventDataRole");
        saveEvent("Test1Event", processTemplateName, "correlationEventDataKey", "correlationEventDataValue", "correlationEventDataRole");

        // The ProcessInstance has no EventReference
        assertEventReferencesCount(0);

        // Send event that adds process data to the process
        sendTest1Event();

        // When the old event got correlated to the process it will complete the process
        assertProcessInstanceCompleted(originProcessId);

        // The ProcessInstance has 2 EventReferences (Test1Event and the old Test2Event)
        assertEventReferencesCount(2);
    }

    private void sendTest1Event() {
        Test1Event event1 = Test1EventBuilder.createForProcessId(originProcessId).build();
       sendSync("topic.test1", event1);
    }

    @Override
    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }

    private void assertEventReferencesCount(int count) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> {
            assertThat(processInstanceRepository.findByOriginProcessIdLoadingMessages(originProcessId).orElseThrow().getMessageReferences()).hasSize(count);
        });
    }

    private void saveEvent(String eventName, String templateName, String eventDataKey, String eventDataValue, String eventDataRole) {
        messageRepository.save(Message.messageBuilder()
                .messageName(eventName)
                .messageId(Generators.timeBasedEpochGenerator().generate().toString())
                .idempotenceId(Generators.timeBasedEpochGenerator().generate().toString())
                .messageCreatedAt(ZonedDateTime.now())
                .originTaskIds(OriginTaskId.from(templateName, Set.of("taskId1", "taskId2")))
                .messageData(Set.of(MessageData.builder()
                        .templateName(templateName)
                        .key(eventDataKey)
                        .value(eventDataValue)
                        .role(eventDataRole)
                        .build()))
                .build());
    }
}
