package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.message.MessageData;
import ch.admin.bit.jeap.processcontext.domain.message.MessageUserData;
import ch.admin.bit.jeap.processcontext.domain.message.OriginTaskId;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import com.fasterxml.uuid.Generators;
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
class MessageJpaRepositoryTest {

    private static final String EVENT_ID = "eventId";
    private static final String EVENT_NAME = "eventName";
    private static final String IDEMPOTENCE_ID = "idempotenceId";
    private static final String TEMPLATE_NAME = "templateName";
    private static final String TASK_ID_1 = "taskid1";
    private static final String TASK_ID_2 = "taskid2";
    private static final MessageData TEMPLATE_EVENT_DATA_NO_ROLE = MessageData.builder()
            .templateName(TEMPLATE_NAME)
            .key("key")
            .value("value")
            .build();
    private static final MessageData TEMPLATE_EVENT_DATA_WITH_ROLE = MessageData.builder()
            .templateName(TEMPLATE_NAME)
            .key("keyother")
            .value("valueother")
            .role("role")
            .build();
    private static final OriginTaskId TEMPLATE_ORIGIN_TASK_ID_1 = OriginTaskId.from(TEMPLATE_NAME, TASK_ID_1);
    private static final OriginTaskId TEMPLATE_ORIGIN_TASK_ID_2 = OriginTaskId.from(TEMPLATE_NAME, TASK_ID_2);
    private static final String TRACE_ID = "66016cec9b6734e17b88d21c0466c6e7";

    @Autowired
    private MessageJpaRepository messageJpaRepository;

    @PersistenceContext
    EntityManager entityManager;

    @MockitoBean
    private ProcessTemplateRepository processTemplateRepository;

    @Test
    void testSaveAndGetDomainEvent() {
        Message message = Message.messageBuilder()
                .messageId(EVENT_ID)
                .messageName(EVENT_NAME)
                .idempotenceId(IDEMPOTENCE_ID)
                .messageData(Set.of(TEMPLATE_EVENT_DATA_NO_ROLE, TEMPLATE_EVENT_DATA_WITH_ROLE))
                .originTaskIds(Set.of(TEMPLATE_ORIGIN_TASK_ID_1, TEMPLATE_ORIGIN_TASK_ID_2))
                .traceId(TRACE_ID)
                .build();

        Message messageSaved = messageJpaRepository.saveAndFlush(message);

        entityManager.detach(messageSaved);
        Message messageRead = messageJpaRepository.getReferenceById(messageSaved.getId());
        assertThat(messageRead.getMessageId()).isEqualTo(message.getMessageId());
        assertThat(messageRead.getMessageName()).isEqualTo(message.getMessageName());
        assertThat(messageRead.getIdempotenceId()).isEqualTo(message.getIdempotenceId());
        assertThat(messageRead.getMessageData()).containsOnly(TEMPLATE_EVENT_DATA_NO_ROLE, TEMPLATE_EVENT_DATA_WITH_ROLE);
        assertThat(messageRead.getOriginTaskIds()).containsOnly(TEMPLATE_ORIGIN_TASK_ID_1, TEMPLATE_ORIGIN_TASK_ID_2);
        assertThat(messageRead.getReceivedAt()).isNotNull();
        assertThat(messageRead.getId()).isEqualTo(messageSaved.getId());
        assertThat(messageRead.getTraceId()).isEqualTo(TRACE_ID);
    }

    @Test
    void testFindEventsToCorrelate() {

        final String eventNameOne = "eventNameOne";
        final String eventNameTwo = "eventNameTwo";

        UUID eventNameOneInstance1 = saveEvent(eventNameOne, createEventData("eventKeyOne", "eventDataValue", "eventDataRole"));
        UUID eventNameOneInstance2 = saveEvent(eventNameOne, createEventData("eventKeyOne", "eventDataValue", "eventDataRole"));
        UUID eventNameOneInstance3 = saveEvent(eventNameOne, createEventData("eventKeyOne", "eventDataValue", "eventDataRole"));
        UUID eventNameOneInstance4 = saveEvent(eventNameOne, createEventData("eventKeyTwo", "eventDataValue", "eventDataRole"));

        UUID eventNameTwoInstance1 = saveEvent(eventNameTwo, createEventData("eventKeyOne", "eventDataValue", "eventDataRole"));

        List<UUID> alreadyCorrelatedEventIds = List.of(eventNameOneInstance1, eventNameOneInstance3);
        List<Message> eventsToCorrelate = messageJpaRepository.findMessagesToCorrelate(eventNameOne, "templateName", "eventKeyOne", "eventDataValue", "eventDataRole", alreadyCorrelatedEventIds);
        assertThat(eventsToCorrelate).hasSize(1);
        assertThat(eventsToCorrelate.getFirst().getId()).isEqualTo(eventNameOneInstance2);

        eventsToCorrelate = messageJpaRepository.findMessagesToCorrelate(eventNameOne, "templateName", "eventKeyTwo", "eventDataValue", "eventDataRole", alreadyCorrelatedEventIds);
        assertThat(eventsToCorrelate).hasSize(1);
        assertThat(eventsToCorrelate.getFirst().getId()).isEqualTo(eventNameOneInstance4);

        eventsToCorrelate = messageJpaRepository.findMessagesToCorrelate(eventNameTwo, "templateName", "eventKeyOne", "eventDataValue", "eventDataRole", alreadyCorrelatedEventIds);
        assertThat(eventsToCorrelate).hasSize(1);
        assertThat(eventsToCorrelate.getFirst().getId()).isEqualTo(eventNameTwoInstance1);

        eventsToCorrelate = messageJpaRepository.findMessagesToCorrelate(eventNameTwo, "templateName", "eventKeyTwo", "eventDataValue", "eventDataRole", alreadyCorrelatedEventIds);
        assertThat(eventsToCorrelate).isEmpty();

    }

    @Test
    void testFindEventsToCorrelate_wrongRole_eventNotFound() {

        final String eventNameOne = "eventNameOne";
        saveEvent(eventNameOne, createEventData("eventKeyOne", "eventDataValue", "eventDataRole"));

        List<Message> eventsToCorrelate = messageJpaRepository.findMessagesToCorrelate(eventNameOne, "templateName", "eventKeyOne", "eventDataValue", "wrong");
        assertThat(eventsToCorrelate).isEmpty();

        eventsToCorrelate = messageJpaRepository.findMessagesToCorrelate(eventNameOne, "templateName", "eventKeyOne", "eventDataValue", "eventDataRole");
        assertThat(eventsToCorrelate).isNotEmpty();

    }

    @Test
    void testFindEventsToCorrelate_wrongTemplate_eventNotFound() {

        final String eventNameWithRole = "eventNameWithRole";
        saveEvent(eventNameWithRole, createEventData("eventKeyOne", "eventDataValue", "eventDataRole"));

        List<Message> eventsToCorrelateWithRoleWrongTemplate = messageJpaRepository.findMessagesToCorrelate(eventNameWithRole, "templateNameOther", "eventKeyOne", "eventDataValue", "eventDataRole");
        assertThat(eventsToCorrelateWithRoleWrongTemplate).isEmpty();
        List<Message> eventsToCorrelateWithRoleCorrectTemplate = messageJpaRepository.findMessagesToCorrelate(eventNameWithRole, "templateName", "eventKeyOne", "eventDataValue", "eventDataRole");
        assertThat(eventsToCorrelateWithRoleCorrectTemplate).isNotEmpty(); // double check

        final String eventNameNoRole = "eventNameNoRole";
        saveEvent(eventNameNoRole, createEventData("eventKeyOne", "eventDataValue", null));

        List<Message> eventsToCorrelateNoRoleWrongTemplate = messageJpaRepository.findMessagesToCorrelate(eventNameNoRole, "templateNameOther", "eventKeyOne", "eventDataValue");
        assertThat(eventsToCorrelateNoRoleWrongTemplate).isEmpty();
        List<Message> eventsToCorrelateNoRoleCorrectTemplate = messageJpaRepository.findMessagesToCorrelate(eventNameNoRole, "templateName", "eventKeyOne", "eventDataValue");
        assertThat(eventsToCorrelateNoRoleCorrectTemplate).isNotEmpty(); // double check
    }

    @Test
    void testFindEventsToCorrelate_wrongValue_eventNotFound() {

        final String eventNameOne = "eventNameOne";
        saveEvent(eventNameOne, createEventData("eventKeyOne", "eventDataValue", "eventDataRole"));

        List<Message> eventsToCorrelate = messageJpaRepository.findMessagesToCorrelate(eventNameOne, "templateName", "eventKeyOne", "wrong", "eventDataRole");
        assertThat(eventsToCorrelate).isEmpty();

        eventsToCorrelate = messageJpaRepository.findMessagesToCorrelate(eventNameOne, "templateName", "eventKeyOne", "eventDataValue", "eventDataRole");
        assertThat(eventsToCorrelate).isNotEmpty();

    }

    @Test
    void testFindEventsToCorrelate_multipleEventData_eventFound() {

        final String eventNameOne = "eventNameOne";
        saveEvent(eventNameOne,
                createEventData("eventKeyOne", "eventDataValue", "eventDataRole"),
                createEventData("myKey", "myValue", "myRole"));

        List<Message> eventsToCorrelate = messageJpaRepository.findMessagesToCorrelate(eventNameOne, "templateName", "myKey", "myValue", "myRole");
        assertThat(eventsToCorrelate).isNotEmpty();

    }

    @Test
    void testFindEventsToCorrelate_roleNull_eventFound() {

        final String eventNameOne = "eventNameOne";
        saveEvent(eventNameOne,
                createEventData("myKey", "myValue", null));

        List<Message> eventsToCorrelate = messageJpaRepository.findMessagesToCorrelate(eventNameOne, "templateName", "myKey", "myValue");
        assertThat(eventsToCorrelate).isNotEmpty();

    }

    @Test
    void testFindEventsWithoutProcessCorrelationAndDelete_oneEventFound_eventDeleted() {
        saveEvent("eventNameOne", createEventData("myKey", "myValue", null));
        saveEvent("eventNameOne", createEventData("myKey", "myValue", null));

        assertThat(messageJpaRepository.findAll()).hasSize(2);
        final Slice<UUID> uuids = messageJpaRepository.findMessagesWithoutProcessCorrelation(ZonedDateTime.now().plusDays(1), Pageable.ofSize(1));
        assertThat(uuids.getContent()).hasSize(1);

        messageJpaRepository.deleteAllById(uuids.toSet());
        assertThat(messageJpaRepository.findAll()).hasSize(1);
    }

    @Test
    void findMessageUserDataByMessageId() {
        Message message = Message.messageBuilder()
                .messageId(EVENT_ID)
                .messageName(EVENT_NAME)
                .idempotenceId(IDEMPOTENCE_ID)
                .userData(Set.of(new MessageUserData("key1", "value1"), new MessageUserData("key2", "value2")))
                .traceId(TRACE_ID)
                .build();

        Message messageSaved = messageJpaRepository.saveAndFlush(message);

        entityManager.detach(messageSaved);
        List<String[]> messageRead = messageJpaRepository.findMessageUserDataByMessageId(messageSaved.getId());
        assertThat(messageRead)
                .hasSize(2)
                .containsExactlyInAnyOrder(new String[]{"key1", "value1"}, new String[]{"key2", "value2"});
    }

    @Test
    void findMessageUserDataByMessageId_noUserDataFound() {
        Message message = Message.messageBuilder()
                .messageId(EVENT_ID)
                .messageName(EVENT_NAME)
                .idempotenceId(IDEMPOTENCE_ID)
                .traceId(TRACE_ID)
                .build();

        Message messageSaved = messageJpaRepository.saveAndFlush(message);

        entityManager.detach(messageSaved);
        List<String[]> messageRead = messageJpaRepository.findMessageUserDataByMessageId(messageSaved.getId());
        assertThat(messageRead).isEmpty();
    }

    private MessageData createEventData(String eventDataKey, String eventDataValue, String eventDataRole) {
        return MessageData.builder()
                .templateName("templateName")
                .key(eventDataKey)
                .value(eventDataValue)
                .role(eventDataRole)
                .build();
    }

    private UUID saveEvent(String name, MessageData... messageData) {
        Message message = Message.messageBuilder()
                .messageId(EVENT_ID)
                .idempotenceId(Generators.timeBasedEpochGenerator().generate().toString())
                .messageName(name)
                .messageData(Set.of(messageData))
                .build();


        messageJpaRepository.saveAndFlush(message);
        entityManager.detach(message);
        return message.getId();
    }

}

