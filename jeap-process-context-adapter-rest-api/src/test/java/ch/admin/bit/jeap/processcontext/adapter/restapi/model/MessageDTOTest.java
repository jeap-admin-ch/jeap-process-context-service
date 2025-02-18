package ch.admin.bit.jeap.processcontext.adapter.restapi.model;

import ch.admin.bit.jeap.processcontext.domain.message.MessageData;
import ch.admin.bit.jeap.processcontext.domain.processinstance.MessageReferenceMessageDTO;
import ch.admin.bit.jeap.processcontext.domain.processinstance.MessageReferenceMessageDataDTO;
import com.fasterxml.uuid.Generators;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MessageDTOTest {

    @Test
    void create() {
        ZonedDateTime receivedAt = ZonedDateTime.now();
        MessageReferenceMessageDTO messageReferenceMessageDTO = createMessageReferenceMessageDTO(receivedAt, "name");
        MessageDTO messageDTO = MessageDTO.create(messageReferenceMessageDTO);
        assertEquals("name", messageDTO.getName());
        assertEquals(receivedAt, messageDTO.getReceivedAt());
        assertEquals("traceId", messageDTO.getTraceId());
        assertFalse(messageDTO.getMessageData().isEmpty());
        ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData messageData = messageDTO.getMessageData().iterator().next();
        assertEquals("key", messageData.getKey());
        assertEquals("value", messageData.getValue());
        assertEquals("role", messageData.getRole());
        assertEquals(Set.of("t1", "t2"), messageDTO.getRelatedOriginTaskIds());
    }

    @SuppressWarnings("SameParameterValue")
    private MessageReferenceMessageDTO createMessageReferenceMessageDTO(ZonedDateTime receivedAt, String messageName) {
        MessageData messageData = MessageData.builder()
                .templateName("templateName")
                .key("key")
                .value("value")
                .role("role")
                .build();
        return MessageReferenceMessageDTO.builder()
                .messageReferenceId(Generators.timeBasedEpochGenerator().generate())
                .messageId(Generators.timeBasedEpochGenerator().generate())
                .messageReceivedAt(receivedAt)
                .messageName(messageName)
                .messageData(Set.of(MessageReferenceMessageDataDTO.from(messageData)))
                .relatedOriginTaskIds(Set.of("t1", "t2"))
                .traceId("traceId")
                .build();
    }
}