package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.message.MessageData;
import ch.admin.bit.jeap.processcontext.domain.message.MessageRepository;
import ch.admin.bit.jeap.processcontext.domain.message.OriginTaskId;
import ch.admin.bit.jeap.processcontext.domain.processinstance.MessageReference;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceStubs;
import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextFactory;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.byLessThan;

@DataJpaTest
@ContextConfiguration(classes = JpaAdapterConfig.class)
@Import(ProcessContextRepositoryFacadeImpl.class)
class MessageReferenceJpaRepositoryTest {

    @Autowired
    private MessageReferenceJpaRepository messageReferenceJpaRepository;

    @Autowired
    private ProcessInstanceJpaRepository processInstanceJpaRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MessageJpaRepository messageJpaRepository;

    @MockitoBean
    private ProcessTemplateRepository processTemplateRepository;

    @MockitoBean
    private ProcessContextFactory processContextFactory;

    private JpaRepositoryTestSupport jpaRepositoryTestSupport;

    @BeforeEach
    void setUp() {
        jpaRepositoryTestSupport = new JpaRepositoryTestSupport(messageReferenceJpaRepository);
    }

    @Test
    void findMessageReferencesWithMessagesByProcessInstanceId_noMessages_returnsEmptyList() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);

        List<MessageReferenceWithMessage> result = messageReferenceJpaRepository.findMessageReferencesWithMessagesByProcessInstanceId(
                savedProcessInstance.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void findMessageReferencesWithMessagesByProcessInstanceId_withMessages_returnsMessageReferencesAndMessages() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);

        String templateName = savedProcessInstance.getProcessTemplateName();
        Message message = createAndSaveMessage("TestMessage", templateName);
        MessageReference savedReference = jpaRepositoryTestSupport.createAndPersist(message, savedProcessInstance);
        messageReferenceJpaRepository.flush();

        List<MessageReferenceWithMessage> result = messageReferenceJpaRepository.findMessageReferencesWithMessagesByProcessInstanceId(
                savedProcessInstance.getId());

        assertThat(result).hasSize(1);
        MessageReferenceWithMessage first = result.getFirst();

        assertThat(first.messageReference().getId()).isEqualTo(savedReference.getId());
        assertThat(first.message().getId()).isEqualTo(message.getId());
        assertThat(first.message().getMessageName()).isEqualTo("TestMessage");
        assertThat(first.processTemplateName()).isEqualTo(templateName);
    }

    @Test
    void findMessageReferencesWithMessagesByProcessInstanceId_multipleMessages_returnsAll() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);

        String templateName = savedProcessInstance.getProcessTemplateName();
        Message message1 = createAndSaveMessage("Message1", templateName);
        Message message2 = createAndSaveMessage("Message2", templateName);
        jpaRepositoryTestSupport.createAndPersist(message1, savedProcessInstance);
        jpaRepositoryTestSupport.createAndPersist(message2, savedProcessInstance);
        messageReferenceJpaRepository.flush();

        List<MessageReferenceWithMessage> result = messageReferenceJpaRepository.findMessageReferencesWithMessagesByProcessInstanceId(
                savedProcessInstance.getId());

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(r -> r.message().getMessageName())
                .containsExactlyInAnyOrder("Message1", "Message2");
    }

    @Test
    void findMessageReferencesWithMessagesByProcessInstanceId_differentProcessInstances_returnsOnlyMatchingMessages() {
        ProcessInstance processInstance1 = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        ProcessInstance savedProcessInstance1 = processInstanceJpaRepository.saveAndFlush(processInstance1);

        ProcessInstance processInstance2 = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        ProcessInstance savedProcessInstance2 = processInstanceJpaRepository.saveAndFlush(processInstance2);

        String templateName1 = savedProcessInstance1.getProcessTemplateName();
        String templateName2 = savedProcessInstance2.getProcessTemplateName();

        Message message1 = createAndSaveMessage("Message1", templateName1);
        Message message2 = createAndSaveMessage("Message2", templateName2);
        jpaRepositoryTestSupport.createAndPersist(message1, savedProcessInstance1);
        jpaRepositoryTestSupport.createAndPersist(message2, savedProcessInstance2);
        messageReferenceJpaRepository.flush();

        List<MessageReferenceWithMessage> result = messageReferenceJpaRepository.findMessageReferencesWithMessagesByProcessInstanceId(
                savedProcessInstance1.getId());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().message().getMessageName()).isEqualTo("Message1");
    }

    @Test
    void findMessageReferencesWithMessagesByProcessInstanceId_paged_returnsPagedResults() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);

        String templateName = savedProcessInstance.getProcessTemplateName();
        Message message1 = createAndSaveMessage("Message1", templateName);
        Message message2 = createAndSaveMessage("Message2", templateName);
        jpaRepositoryTestSupport.createAndPersist(message1, savedProcessInstance);
        jpaRepositoryTestSupport.createAndPersist(message2, savedProcessInstance);
        messageReferenceJpaRepository.flush();

        Page<MessageReferenceWithMessage> firstPage = messageReferenceJpaRepository
                .findMessageReferencesWithMessagesByProcessInstanceId(savedProcessInstance.getId(), PageRequest.of(0, 1));

        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent()).hasSize(1);

        Page<MessageReferenceWithMessage> secondPage = messageReferenceJpaRepository
                .findMessageReferencesWithMessagesByProcessInstanceId(savedProcessInstance.getId(), PageRequest.of(1, 1));

        assertThat(secondPage.getContent()).hasSize(1);
        assertThat(secondPage.getContent().getFirst().message().getMessageName())
                .isNotEqualTo(firstPage.getContent().getFirst().message().getMessageName());
    }

    @Test
    void findMessageReferenceWithMessageByProcessInstanceIdAndMessageId_found_returnsResult() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);

        String templateName = savedProcessInstance.getProcessTemplateName();
        Message message = createAndSaveMessage("TestMessage", templateName);
        MessageReference savedReference = jpaRepositoryTestSupport.createAndPersist(message, savedProcessInstance);
        messageReferenceJpaRepository.flush();

        Optional<MessageReferenceWithMessage> result = messageReferenceJpaRepository
                .findMessageReferenceWithMessageByProcessInstanceIdAndMessageId(savedProcessInstance.getId(), message.getId());

        assertThat(result).isPresent();
        assertThat(result.get().messageReference().getId()).isEqualTo(savedReference.getId());
        assertThat(result.get().message().getId()).isEqualTo(message.getId());
        assertThat(result.get().message().getMessageName()).isEqualTo("TestMessage");
        assertThat(result.get().processTemplateName()).isEqualTo(templateName);
    }

    @Test
    void findMessageReferenceWithMessageByProcessInstanceIdAndMessageId_notFound_returnsEmpty() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);

        Optional<MessageReferenceWithMessage> result = messageReferenceJpaRepository
                .findMessageReferenceWithMessageByProcessInstanceIdAndMessageId(savedProcessInstance.getId(), UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    @Test
    void findMessageReferenceWithMessageByProcessInstanceIdAndMessageId_wrongProcessInstance_returnsEmpty() {
        ProcessInstance processInstance1 = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        ProcessInstance savedProcessInstance1 = processInstanceJpaRepository.saveAndFlush(processInstance1);

        ProcessInstance processInstance2 = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        ProcessInstance savedProcessInstance2 = processInstanceJpaRepository.saveAndFlush(processInstance2);

        String templateName1 = savedProcessInstance1.getProcessTemplateName();
        Message message = createAndSaveMessage("TestMessage", templateName1);
        jpaRepositoryTestSupport.createAndPersist(message, savedProcessInstance1);
        messageReferenceJpaRepository.flush();

        Optional<MessageReferenceWithMessage> result = messageReferenceJpaRepository
                .findMessageReferenceWithMessageByProcessInstanceIdAndMessageId(savedProcessInstance2.getId(), message.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void findMessageReferencesWithMessagesByProcessInstanceId_unknownProcessInstanceId_returnsEmptyList() {
        List<MessageReferenceWithMessage> result = messageReferenceJpaRepository.findMessageReferencesWithMessagesByProcessInstanceId(
                UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    @Test
    void findMessageReferencesWithMessagesByProcessInstanceId_withMessageDataAndOriginTaskIds_returnsFullMessage() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);

        String templateName = savedProcessInstance.getProcessTemplateName();
        MessageData messageData = new MessageData(templateName, "testKey", "testValue", "testRole");
        OriginTaskId originTaskId = OriginTaskId.from(templateName, "originTask1");

        Message message = messageRepository.save(Message.messageBuilder()
                .messageName("TestMessageWithData")
                .messageId(UUID.randomUUID().toString())
                .idempotenceId(UUID.randomUUID().toString())
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .messageData(Set.of(messageData))
                .originTaskIds(Set.of(originTaskId))
                .traceId("trace-123")
                .build());
        jpaRepositoryTestSupport.createAndPersist(message, savedProcessInstance);
        messageReferenceJpaRepository.flush();

        List<MessageReferenceWithMessage> result = messageReferenceJpaRepository.findMessageReferencesWithMessagesByProcessInstanceId(
                savedProcessInstance.getId());

        assertThat(result).hasSize(1);
        Message returnedMessage = result.getFirst().message();
        assertThat(returnedMessage.getMessageData(templateName)).hasSize(1);
        assertThat(returnedMessage.getMessageData(templateName).iterator().next().getKey()).isEqualTo("testKey");
        assertThat(returnedMessage.getOriginTaskIds(templateName)).hasSize(1);
        assertThat(returnedMessage.getOriginTaskIds(templateName).iterator().next().getOriginTaskId()).isEqualTo("originTask1");
    }

    private Message createAndSaveMessage(String messageName, String templateName) {
        return messageRepository.save(Message.messageBuilder()
                .messageName(messageName)
                .messageId(UUID.randomUUID().toString())
                .idempotenceId(UUID.randomUUID().toString())
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .messageData(Set.of(new MessageData(templateName, "key", "value")))
                .build());
    }

    private Message createAndSaveMessageWithCreatedAt(String messageName, String templateName, ZonedDateTime createdAt) {
        return messageRepository.save(Message.messageBuilder()
                .messageName(messageName)
                .messageId(UUID.randomUUID().toString())
                .idempotenceId(UUID.randomUUID().toString())
                .createdAt(createdAt)
                .messageCreatedAt(createdAt)
                .messageData(Set.of(new MessageData(templateName, "key", "value")))
                .build());
    }

    @Test
    void findLastMessageCreatedAtByProcessInstanceIds_noMessages_returnsEmptyList() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);

        List<ProcessInstanceLastMessageProjection> result = messageReferenceJpaRepository
                .findLastMessageCreatedAtByProcessInstanceIds(List.of(savedProcessInstance.getId()));

        assertThat(result).isEmpty();
    }

    @Test
    void findLastMessageCreatedAtByProcessInstanceIds_withMessages_returnsLatestDate() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);

        String templateName = savedProcessInstance.getProcessTemplateName();
        ZonedDateTime olderDate = ZonedDateTime.now().minusDays(2);
        ZonedDateTime newerDate = ZonedDateTime.now().minusDays(1);

        Message olderMessage = createAndSaveMessageWithCreatedAt("OlderMessage", templateName, olderDate);
        Message newerMessage = createAndSaveMessageWithCreatedAt("NewerMessage", templateName, newerDate);
        jpaRepositoryTestSupport.createAndPersist(olderMessage, savedProcessInstance);
        jpaRepositoryTestSupport.createAndPersist(newerMessage, savedProcessInstance);
        messageReferenceJpaRepository.flush();

        List<ProcessInstanceLastMessageProjection> result = messageReferenceJpaRepository
                .findLastMessageCreatedAtByProcessInstanceIds(List.of(savedProcessInstance.getId()));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getProcessInstanceId()).isEqualTo(savedProcessInstance.getId());
        assertThat(result.getFirst().getLastMessageCreatedAt()).isCloseTo(newerDate, byLessThan(Duration.ofSeconds(1)));
    }

    @Test
    void findLastMessageCreatedAtByProcessInstanceIds_multipleProcessInstances_returnsAllDates() {
        ProcessInstance processInstance1 = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        ProcessInstance savedProcessInstance1 = processInstanceJpaRepository.saveAndFlush(processInstance1);

        ProcessInstance processInstance2 = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        ProcessInstance savedProcessInstance2 = processInstanceJpaRepository.saveAndFlush(processInstance2);

        String templateName1 = savedProcessInstance1.getProcessTemplateName();
        String templateName2 = savedProcessInstance2.getProcessTemplateName();
        ZonedDateTime date1 = ZonedDateTime.now().minusDays(2);
        ZonedDateTime date2 = ZonedDateTime.now().minusDays(1);

        Message message1 = createAndSaveMessageWithCreatedAt("Message1", templateName1, date1);
        Message message2 = createAndSaveMessageWithCreatedAt("Message2", templateName2, date2);
        jpaRepositoryTestSupport.createAndPersist(message1, savedProcessInstance1);
        jpaRepositoryTestSupport.createAndPersist(message2, savedProcessInstance2);
        messageReferenceJpaRepository.flush();

        List<ProcessInstanceLastMessageProjection> result = messageReferenceJpaRepository
                .findLastMessageCreatedAtByProcessInstanceIds(
                        List.of(savedProcessInstance1.getId(), savedProcessInstance2.getId()));

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(ProcessInstanceLastMessageProjection::getProcessInstanceId)
                .containsExactlyInAnyOrder(savedProcessInstance1.getId(), savedProcessInstance2.getId());
    }

    @Test
    void findLastMessageCreatedAtByProcessInstanceIds_emptyCollection_returnsEmptyList() {
        List<ProcessInstanceLastMessageProjection> result = messageReferenceJpaRepository
                .findLastMessageCreatedAtByProcessInstanceIds(List.of());

        assertThat(result).isEmpty();
    }
}
