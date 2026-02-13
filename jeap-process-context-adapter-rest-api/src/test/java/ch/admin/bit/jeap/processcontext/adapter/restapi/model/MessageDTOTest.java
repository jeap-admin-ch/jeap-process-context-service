package ch.admin.bit.jeap.processcontext.adapter.restapi.model;

import ch.admin.bit.jeap.processcontext.domain.message.MessageData;
import ch.admin.bit.jeap.processcontext.domain.processinstance.MessageReferenceMessageDTO;
import ch.admin.bit.jeap.processcontext.domain.processinstance.MessageReferenceMessageDataDTO;
import com.fasterxml.uuid.Generators;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MessageDTOTest {

    @Test
    void create() {
        ZonedDateTime receivedAt = ZonedDateTime.now();
        ZonedDateTime createdAt = receivedAt.minusMinutes(5);

        MessageReferenceMessageDTO messageReferenceMessageDTO = createMessageReferenceMessageDTO(receivedAt, createdAt, "name", 1);
        MessageDTO messageDTO = MessageDTO.create(messageReferenceMessageDTO);
        assertThat(messageDTO.getName()).isEqualTo("name");
        assertThat(messageDTO.getReceivedAt()).isEqualTo(receivedAt);
        assertThat(messageDTO.getCreatedAt()).isEqualTo(createdAt);
        assertThat(messageDTO.getTraceId()).isEqualTo("traceId");
        assertThat(messageDTO.getMessageData()).hasSize(1);
        assertThat(messageDTO.isMessageDataTruncated()).isFalse();
        ch.admin.bit.jeap.processcontext.plugin.api.message.MessageData messageData = messageDTO.getMessageData().getFirst();
        assertThat(messageData.getKey()).isEqualTo("key0");
        assertThat(messageData.getValue()).isEqualTo("value0");
        assertThat(messageData.getRole()).isEqualTo("role");
        assertThat(messageDTO.getRelatedOriginTaskIds()).isEqualTo(Set.of("t1", "t2"));
    }

    @Test
    void create_messageDataNotTruncated_whenExactlyAtLimit() {
        ZonedDateTime now = ZonedDateTime.now();
        MessageReferenceMessageDTO ref = createMessageReferenceMessageDTO(now, now, "name", MessageDTO.MAX_MESSAGE_DATA_SIZE);
        MessageDTO dto = MessageDTO.create(ref);

        assertThat(dto.getMessageData()).hasSize(MessageDTO.MAX_MESSAGE_DATA_SIZE);
        assertThat(dto.isMessageDataTruncated()).isFalse();
    }

    @Test
    void create_messageDataTruncated_whenOverLimit() {
        ZonedDateTime now = ZonedDateTime.now();
        int overLimit = MessageDTO.MAX_MESSAGE_DATA_SIZE + 5;
        MessageReferenceMessageDTO ref = createMessageReferenceMessageDTO(now, now, "name", overLimit);
        MessageDTO dto = MessageDTO.create(ref);

        assertThat(dto.getMessageData()).hasSize(MessageDTO.MAX_MESSAGE_DATA_SIZE);
        assertThat(dto.isMessageDataTruncated()).isTrue();
        assertThat(dto.getMessageData())
                .extracting(ch.admin.bit.jeap.processcontext.plugin.api.message.MessageData::getKey)
                .isSorted();
    }

    @Test
    void create_messageDataSortedByKey() {
        ZonedDateTime now = ZonedDateTime.now();
        MessageReferenceMessageDTO ref = createMessageReferenceMessageDTO(now, now, "name", 3);
        MessageDTO dto = MessageDTO.create(ref);

        assertThat(dto.getMessageData())
                .extracting(ch.admin.bit.jeap.processcontext.plugin.api.message.MessageData::getKey)
                .isSorted();
    }

    private MessageReferenceMessageDTO createMessageReferenceMessageDTO(ZonedDateTime receivedAt, ZonedDateTime createdAt, String messageName, int messageDataCount) {
        List<MessageReferenceMessageDataDTO> dataDTOs = new ArrayList<>();
        for (int i = 0; i < messageDataCount; i++) {
            MessageData messageData = MessageData.builder()
                    .templateName("templateName")
                    .key("key" + i)
                    .value("value" + i)
                    .role("role")
                    .build();
            dataDTOs.add(MessageReferenceMessageDataDTO.from(messageData));
        }
        return MessageReferenceMessageDTO.builder()
                .messageReferenceId(Generators.timeBasedEpochGenerator().generate())
                .messageId(Generators.timeBasedEpochGenerator().generate())
                .messageReceivedAt(receivedAt)
                .messageCreatedAt(createdAt)
                .messageName(messageName)
                .messageData(dataDTOs)
                .relatedOriginTaskIds(Set.of("t1", "t2"))
                .traceId("traceId")
                .build();
    }
}
