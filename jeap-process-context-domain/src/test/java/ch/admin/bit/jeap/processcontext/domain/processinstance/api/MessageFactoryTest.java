package ch.admin.bit.jeap.processcontext.domain.processinstance.api;

import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.message.MessageData;
import ch.admin.bit.jeap.processcontext.domain.message.OriginTaskId;
import ch.admin.bit.jeap.processcontext.domain.processinstance.AddedMessage;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceStubs;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageFactoryTest {

    @Test
    void createMessage() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleDynamicTaskInstance();
        String templateName = processInstance.getProcessTemplateName();
        Set<String> taskNames = Set.of("taskId1", "taskId2");
        Message domainMessage = Message.messageBuilder()
                .messageName("event")
                .messageId("eventId")
                .idempotenceId("idempotenceId")
                .originTaskIds(OriginTaskId.from(templateName, taskNames))
                .messageData(Set.of(new MessageData(templateName, "myKey", "myValue", "myRole")))
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .build();
        AddedMessage addedMessage = processInstance.addMessage(domainMessage);

        ch.admin.bit.jeap.processcontext.plugin.api.context.Message apiMessage = MessageFactory.createMessage(addedMessage.messageReference());

        assertEquals(domainMessage.getMessageName(), apiMessage.getName());
        assertEquals(taskNames, apiMessage.getRelatedOriginTaskIds());
        MessageData messageData = domainMessage.getMessageData(templateName).iterator().next();
        assertEquals(1, apiMessage.getMessageData().size());
        ch.admin.bit.jeap.processcontext.plugin.api.message.MessageData eventDataFromAPI = apiMessage.getMessageData().iterator().next();
        assertEquals(messageData.getKey(), eventDataFromAPI.getKey());
        assertEquals(messageData.getValue(), eventDataFromAPI.getValue());
        assertEquals(messageData.getRole(), eventDataFromAPI.getRole());
    }
}
