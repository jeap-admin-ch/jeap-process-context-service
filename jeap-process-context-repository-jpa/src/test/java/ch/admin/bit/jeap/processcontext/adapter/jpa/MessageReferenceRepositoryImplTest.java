package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.processinstance.MessageReference;
import ch.admin.bit.jeap.processcontext.domain.processinstance.MessageReferenceMessageDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageReferenceRepositoryImplTest {

    @Mock
    private MessageReferenceJpaRepository messageReferenceJpaRepository;

    @Mock
    private MessageReference messageReference;

    private MessageReferenceRepositoryImpl messageReferenceRepository;

    @BeforeEach
    void setUp() {
        messageReferenceRepository = new MessageReferenceRepositoryImpl(messageReferenceJpaRepository);
    }

    @Test
    void save_delegatesToJpaRepository() {
        when(messageReferenceJpaRepository.save(messageReference)).thenReturn(messageReference);

        MessageReference result = messageReferenceRepository.save(messageReference);

        assertThat(result).isEqualTo(messageReference);
        verify(messageReferenceJpaRepository).save(messageReference);
    }

    @Test
    void findByProcessInstanceIdAndMessageId_found_returnsDTO() {
        UUID processInstanceId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID messageReferenceId = UUID.randomUUID();
        String processTemplateName = "testTemplate";

        MessageReference mockMessageRef = mock(MessageReference.class);
        when(mockMessageRef.getId()).thenReturn(messageReferenceId);

        Message message = Message.messageBuilder()
                .messageId("msg-123")
                .idempotenceId("idemp-123")
                .messageName("TestMessage")
                .messageData(Set.of())
                .originTaskIds(Set.of())
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .build();

        MessageReferenceWithMessage refWithMessage = new MessageReferenceWithMessage(mockMessageRef, message, processTemplateName);
        when(messageReferenceJpaRepository.findMessageReferenceWithMessageByProcessInstanceIdAndMessageId(processInstanceId, messageId))
                .thenReturn(Optional.of(refWithMessage));

        MessageReferenceMessageDTO result = messageReferenceRepository.findByProcessInstanceIdAndMessageId(processInstanceId, messageId);

        assertThat(result).isNotNull();
        assertThat(result.getMessageReferenceId()).isEqualTo(messageReferenceId);
        assertThat(result.getMessageId()).isEqualTo(message.getId());
        assertThat(result.getMessageName()).isEqualTo("TestMessage");
    }

    @Test
    void findByProcessInstanceIdAndMessageId_notFound_returnsNull() {
        UUID processInstanceId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        when(messageReferenceJpaRepository.findMessageReferenceWithMessageByProcessInstanceIdAndMessageId(processInstanceId, messageId))
                .thenReturn(Optional.empty());

        MessageReferenceMessageDTO result = messageReferenceRepository.findByProcessInstanceIdAndMessageId(processInstanceId, messageId);

        assertThat(result).isNull();
    }

    @Test
    void findByProcessInstanceId_noResults_returnsEmptyList() {
        UUID processInstanceId = UUID.randomUUID();
        when(messageReferenceJpaRepository.findMessageReferencesWithMessagesByProcessInstanceId(processInstanceId))
                .thenReturn(List.of());

        List<MessageReferenceMessageDTO> result = messageReferenceRepository.findByProcessInstanceId(processInstanceId);

        assertThat(result).isEmpty();
        verify(messageReferenceJpaRepository).findMessageReferencesWithMessagesByProcessInstanceId(processInstanceId);
    }

    @Test
    void findByProcessInstanceId_withResults_transformsToDTO() {
        UUID processInstanceId = UUID.randomUUID();
        UUID messageReferenceId = UUID.randomUUID();
        String processTemplateName = "testTemplate";

        MessageReference mockMessageReference = mock(MessageReference.class);
        when(mockMessageReference.getId()).thenReturn(messageReferenceId);

        Message message = Message.messageBuilder()
                .messageId("msg-123")
                .idempotenceId("idemp-123")
                .messageName("TestMessage")
                .messageData(Set.of())
                .originTaskIds(Set.of())
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .build();

        MessageReferenceWithMessage result1 = new MessageReferenceWithMessage(mockMessageReference, message, processTemplateName);
        when(messageReferenceJpaRepository.findMessageReferencesWithMessagesByProcessInstanceId(processInstanceId))
                .thenReturn(List.of(result1));

        List<MessageReferenceMessageDTO> result = messageReferenceRepository.findByProcessInstanceId(processInstanceId);

        assertThat(result).hasSize(1);
        MessageReferenceMessageDTO dto = result.get(0);
        assertThat(dto.getMessageReferenceId()).isEqualTo(messageReferenceId);
        assertThat(dto.getMessageId()).isEqualTo(message.getId());
        assertThat(dto.getMessageName()).isEqualTo("TestMessage");
    }

    @Test
    void findByProcessInstanceId_multipleResults_transformsAllToDTOs() {
        UUID processInstanceId = UUID.randomUUID();
        String processTemplateName = "testTemplate";

        MessageReference mockMessageReference1 = mock(MessageReference.class);
        when(mockMessageReference1.getId()).thenReturn(UUID.randomUUID());
        Message message1 = Message.messageBuilder()
                .messageId("msg-1")
                .idempotenceId("idemp-1")
                .messageName("Message1")
                .messageData(Set.of())
                .originTaskIds(Set.of())
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .build();

        MessageReference mockMessageReference2 = mock(MessageReference.class);
        when(mockMessageReference2.getId()).thenReturn(UUID.randomUUID());
        Message message2 = Message.messageBuilder()
                .messageId("msg-2")
                .idempotenceId("idemp-2")
                .messageName("Message2")
                .messageData(Set.of())
                .originTaskIds(Set.of())
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .build();

        MessageReferenceWithMessage result1 = new MessageReferenceWithMessage(mockMessageReference1, message1, processTemplateName);
        MessageReferenceWithMessage result2 = new MessageReferenceWithMessage(mockMessageReference2, message2, processTemplateName);
        when(messageReferenceJpaRepository.findMessageReferencesWithMessagesByProcessInstanceId(processInstanceId))
                .thenReturn(List.of(result1, result2));

        List<MessageReferenceMessageDTO> result = messageReferenceRepository.findByProcessInstanceId(processInstanceId);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(MessageReferenceMessageDTO::getMessageName)
                .containsExactly("Message1", "Message2");
    }

    @Test
    void findLastMessageCreatedAtByProcessInstanceIds_emptyCollection_returnsEmptyMap() {
        Map<UUID, ZonedDateTime> result = messageReferenceRepository.findLastMessageCreatedAtByProcessInstanceIds(List.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(messageReferenceJpaRepository);
    }

    @Test
    void findLastMessageCreatedAtByProcessInstanceIds_withResults_returnsMap() {
        UUID processInstanceId1 = UUID.randomUUID();
        UUID processInstanceId2 = UUID.randomUUID();
        ZonedDateTime date1 = ZonedDateTime.now().minusDays(1);
        ZonedDateTime date2 = ZonedDateTime.now();

        ProcessInstanceLastMessageProjection projection1 = mock(ProcessInstanceLastMessageProjection.class);
        when(projection1.getProcessInstanceId()).thenReturn(processInstanceId1);
        when(projection1.getLastMessageCreatedAt()).thenReturn(date1);

        ProcessInstanceLastMessageProjection projection2 = mock(ProcessInstanceLastMessageProjection.class);
        when(projection2.getProcessInstanceId()).thenReturn(processInstanceId2);
        when(projection2.getLastMessageCreatedAt()).thenReturn(date2);

        List<UUID> ids = List.of(processInstanceId1, processInstanceId2);
        when(messageReferenceJpaRepository.findLastMessageCreatedAtByProcessInstanceIds(ids))
                .thenReturn(List.of(projection1, projection2));

        Map<UUID, ZonedDateTime> result = messageReferenceRepository.findLastMessageCreatedAtByProcessInstanceIds(ids);

        assertThat(result)
                .hasSize(2)
                .containsEntry(processInstanceId1, date1)
                .containsEntry(processInstanceId2, date2);
    }

    @Test
    void findLastMessageCreatedAtByProcessInstanceIds_noMatchingMessages_returnsEmptyMap() {
        UUID processInstanceId = UUID.randomUUID();
        List<UUID> ids = List.of(processInstanceId);
        when(messageReferenceJpaRepository.findLastMessageCreatedAtByProcessInstanceIds(ids))
                .thenReturn(List.of());

        Map<UUID, ZonedDateTime> result = messageReferenceRepository.findLastMessageCreatedAtByProcessInstanceIds(ids);

        assertThat(result).isEmpty();
    }
}
