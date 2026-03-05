package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.message.MessageData;
import ch.admin.bit.jeap.processcontext.domain.processinstance.PendingMessage;
import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextFactory;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = JpaAdapterConfig.class)
class PendingMessageJpaRepositoryTest {

    @PersistenceContext
    EntityManager entityManager;

    @MockitoBean
    private ProcessTemplateRepository processTemplateRepository;

    @MockitoBean
    private ProcessContextFactory processContextFactory;

    @Autowired
    private PendingMessageJpaRepository pendingMessageJpaRepository;

    @Autowired
    private MessageJpaRepository messageJpaRepository;

    @Test
    void findByOriginProcessId_returnsMatchingMessages() {
        Message message1 = createAndSaveMessage("event1");
        Message message2 = createAndSaveMessage("event2");
        Message message3 = createAndSaveMessage("event3");

        pendingMessageJpaRepository.saveIfNew("process1", message1.getId());
        pendingMessageJpaRepository.saveIfNew("process1", message2.getId());
        pendingMessageJpaRepository.saveIfNew("process2", message3.getId());
        entityManager.flush();
        entityManager.clear();

        List<PendingMessage> result = pendingMessageJpaRepository.findByOriginProcessId("process1");

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(PendingMessage::getMessageId)
                .containsExactlyInAnyOrder(message1.getId(), message2.getId());
    }

    @Test
    void findByOriginProcessId_noMatches_returnsEmptyList() {
        Message message = createAndSaveMessage("event1");
        pendingMessageJpaRepository.saveIfNew("process1", message.getId());
        entityManager.flush();
        entityManager.clear();

        List<PendingMessage> result = pendingMessageJpaRepository.findByOriginProcessId("nonExistent");

        assertThat(result).isEmpty();
    }

    @Test
    void deleteAll_removesEntities() {
        Message message1 = createAndSaveMessage("event1");
        Message message2 = createAndSaveMessage("event2");

        pendingMessageJpaRepository.saveIfNew("process1", message1.getId());
        pendingMessageJpaRepository.saveIfNew("process1", message2.getId());
        entityManager.flush();
        entityManager.clear();

        List<PendingMessage> toDelete = pendingMessageJpaRepository.findByOriginProcessId("process1");
        pendingMessageJpaRepository.deleteAll(toDelete);
        entityManager.flush();
        entityManager.clear();

        assertThat(pendingMessageJpaRepository.findByOriginProcessId("process1")).isEmpty();
    }

    @Test
    void saveIfNew_insertsNewEntry() {
        Message message = createAndSaveMessage("event1");
        entityManager.clear();

        pendingMessageJpaRepository.saveIfNew("process1", message.getId());
        entityManager.flush();
        entityManager.clear();

        List<PendingMessage> result = pendingMessageJpaRepository.findByOriginProcessId("process1");
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getMessageId()).isEqualTo(message.getId());
    }

    @Test
    void saveIfNew_duplicateEntry_doesNothing() {
        Message message = createAndSaveMessage("event1");
        pendingMessageJpaRepository.saveIfNew("process1", message.getId());
        entityManager.flush();
        entityManager.clear();

        pendingMessageJpaRepository.saveIfNew("process1", message.getId());
        entityManager.flush();
        entityManager.clear();

        List<PendingMessage> result = pendingMessageJpaRepository.findByOriginProcessId("process1");
        assertThat(result).hasSize(1);
    }

    @Test
    void deleteAllByIds_removesMatchingEntries() {
        Message message1 = createAndSaveMessage("event1");
        Message message2 = createAndSaveMessage("event2");
        pendingMessageJpaRepository.saveIfNew("process1", message1.getId());
        pendingMessageJpaRepository.saveIfNew("process2", message2.getId());
        entityManager.flush();
        entityManager.clear();

        Slice<UUID> ids = pendingMessageJpaRepository.findPendingMessagesCreatedBefore(
                ZonedDateTime.now().plusMinutes(1), Pageable.ofSize(1));
        assertThat(ids.getContent()).hasSize(1);

        pendingMessageJpaRepository.deleteAllByIds(Set.copyOf(ids.getContent()));
        entityManager.flush();
        entityManager.clear();

        // One of the two entries should remain
        Slice<UUID> remaining = pendingMessageJpaRepository.findPendingMessagesCreatedBefore(
                ZonedDateTime.now().plusMinutes(1), Pageable.ofSize(10));
        assertThat(remaining.getContent()).hasSize(1);
    }

    @Test
    void findPendingMessagesCreatedBefore_returnsMatchingIds() {
        Message message1 = createAndSaveMessage("event1");
        Message message2 = createAndSaveMessage("event2");
        pendingMessageJpaRepository.saveIfNew("process1", message1.getId());
        pendingMessageJpaRepository.saveIfNew("process1", message2.getId());
        entityManager.flush();
        entityManager.clear();

        Slice<UUID> result = pendingMessageJpaRepository.findPendingMessagesCreatedBefore(
                ZonedDateTime.now().plusMinutes(1), Pageable.ofSize(10));

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void findPendingMessagesCreatedBefore_noneOldEnough_returnsEmpty() {
        Message message = createAndSaveMessage("event1");
        pendingMessageJpaRepository.saveIfNew("process1", message.getId());
        entityManager.flush();
        entityManager.clear();

        Slice<UUID> result = pendingMessageJpaRepository.findPendingMessagesCreatedBefore(
                ZonedDateTime.now().minusMinutes(1), Pageable.ofSize(10));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void deleteByOriginProcessId_removesMatchingEntries() {
        Message message1 = createAndSaveMessage("event1");
        Message message2 = createAndSaveMessage("event2");
        Message message3 = createAndSaveMessage("event3");
        pendingMessageJpaRepository.saveIfNew("process1", message1.getId());
        pendingMessageJpaRepository.saveIfNew("process1", message2.getId());
        pendingMessageJpaRepository.saveIfNew("process2", message3.getId());
        entityManager.flush();
        entityManager.clear();

        pendingMessageJpaRepository.deleteByOriginProcessId("process1");
        entityManager.flush();
        entityManager.clear();

        assertThat(pendingMessageJpaRepository.findByOriginProcessId("process1")).isEmpty();
        assertThat(pendingMessageJpaRepository.findByOriginProcessId("process2")).hasSize(1);
    }

    private Message createAndSaveMessage(String eventId) {
        Message message = Message.messageBuilder()
                .messageId(eventId)
                .messageName("testEvent")
                .idempotenceId(eventId + "-idempotence")
                .messageData(Set.of(MessageData.builder()
                        .templateName("template")
                        .key("key")
                        .value("value")
                        .build()))
                .build();
        return messageJpaRepository.saveAndFlush(message);
    }
}
