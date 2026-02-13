package ch.admin.bit.jeap.processcontext.adapter.restapi.model;

import ch.admin.bit.jeap.processcontext.domain.message.MessageReferenceRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.MessageReferenceMessageDTO;
import ch.admin.bit.jeap.processcontext.domain.processinstance.MessageReferenceMessageDataDTO;
import com.fasterxml.uuid.Generators;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessageDTOFactoryTest {

    private MessageReferenceRepository messageReferenceRepository;
    private MessageDTOFactory factory;

    @BeforeEach
    void setUp() {
        messageReferenceRepository = mock(MessageReferenceRepository.class);
        factory = new MessageDTOFactory(messageReferenceRepository);
    }

    @Test
    void createMessageDTOPage_returnsPageOfDTOs() {
        UUID processInstanceId = UUID.randomUUID();
        ZonedDateTime now = ZonedDateTime.now();

        MessageReferenceMessageDTO ref1 = MessageReferenceMessageDTO.builder()
                .messageReferenceId(Generators.timeBasedEpochGenerator().generate())
                .messageId(Generators.timeBasedEpochGenerator().generate())
                .messageName("TestEvent1")
                .messageCreatedAt(now)
                .messageReceivedAt(now)
                .messageData(List.of(MessageReferenceMessageDataDTO.builder()
                        .messageDataKey("key1")
                        .messageDataValue("value1")
                        .build()))
                .relatedOriginTaskIds(Set.of("task1"))
                .build();

        MessageReferenceMessageDTO ref2 = MessageReferenceMessageDTO.builder()
                .messageReferenceId(Generators.timeBasedEpochGenerator().generate())
                .messageId(Generators.timeBasedEpochGenerator().generate())
                .messageName("TestEvent2")
                .messageCreatedAt(now.minusHours(1))
                .messageReceivedAt(now.minusHours(1))
                .messageData(List.of())
                .relatedOriginTaskIds(Set.of())
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<MessageReferenceMessageDTO> refPage = new PageImpl<>(List.of(ref1, ref2), pageable, 2);
        when(messageReferenceRepository.findByProcessInstanceId(eq(processInstanceId), any(Pageable.class)))
                .thenReturn(refPage);

        Page<MessageDTO> result = factory.createMessageDTOPage(processInstanceId, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("TestEvent1");
        assertThat(result.getContent().get(0).getRelatedOriginTaskIds()).containsExactly("task1");
        assertThat(result.getContent().get(1).getName()).isEqualTo("TestEvent2");
    }

    @Test
    void createMessageDTOPage_emptyPage_returnsEmptyPage() {
        UUID processInstanceId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        when(messageReferenceRepository.findByProcessInstanceId(eq(processInstanceId), any(Pageable.class)))
                .thenReturn(Page.empty(pageable));

        Page<MessageDTO> result = factory.createMessageDTOPage(processInstanceId, pageable);

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }
}
