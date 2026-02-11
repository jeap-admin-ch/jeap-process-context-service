package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.domain.housekeeping.HouseKeepingService;
import ch.admin.bit.jeap.processcontext.domain.message.*;
import ch.admin.bit.jeap.processcontext.domain.processinstance.*;
import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextFactory;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskCardinality;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskLifecycle;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskType;
import com.fasterxml.uuid.Generators;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties =
        "jeap.processcontext.template.classpath-location-pattern=classpath:/process/templates/domain_event_triggers_process_instance_instantiation.json")
class HouseKeepingServiceIT extends ProcessInstanceMockS3ITBase {

    @Autowired
    private HouseKeepingService houseKeepingService;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MessageReferenceRepository messageReferenceRepository;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private PendingMessageRepository pendingMessageRepository;

    @Autowired
    private ProcessContextFactory processContextFactory;

    @Test
    void testDeleteProcessInstances_completedProcessInstances_processInstancesDeleted() {
        inTransaction(() -> {
            createAndSaveProcessInstance(ProcessState.COMPLETED, Duration.of(210, ChronoUnit.DAYS));
            createAndSaveProcessInstance(ProcessState.COMPLETED, Duration.of(210, ChronoUnit.DAYS));
            createAndSaveProcessInstance(ProcessState.COMPLETED, Duration.of(179, ChronoUnit.DAYS));
            createAndSaveProcessInstance(ProcessState.STARTED, Duration.of(181, ChronoUnit.DAYS));
        });

        assertCountProcessInstances(4);

        houseKeepingService.cleanup();

        assertCountProcessInstances(2);
    }

    private void inTransaction(Runnable runnable) {
        new TransactionTemplate(platformTransactionManager)
                .executeWithoutResult(status -> runnable.run());
    }

    @Test
    void testDeleteProcessInstances_veryOldStartedProcessInstances_processInstancesDeleted() {
        inTransaction(() -> {
            createAndSaveProcessInstance(ProcessState.STARTED, Duration.of(366, ChronoUnit.DAYS));
            createAndSaveProcessInstance(ProcessState.STARTED, Duration.of(366, ChronoUnit.DAYS));
            createAndSaveProcessInstance(ProcessState.STARTED, Duration.of(364, ChronoUnit.DAYS));
            createAndSaveProcessInstance(ProcessState.STARTED, Duration.of(364, ChronoUnit.DAYS));
        });

        assertCountProcessInstances(4);

        houseKeepingService.cleanup();

        assertCountProcessInstances(2);
    }

    @Test
    void testDeleteProcessInstances_completedProcessInstancesWithPaging_processInstancesDeleted() {
        inTransaction(() -> {
            createAndSaveProcessInstance(ProcessState.COMPLETED, Duration.of(210, ChronoUnit.DAYS));
            createAndSaveProcessInstance(ProcessState.COMPLETED, Duration.of(210, ChronoUnit.DAYS));
            createAndSaveProcessInstance(ProcessState.COMPLETED, Duration.of(210, ChronoUnit.DAYS));
            createAndSaveProcessInstance(ProcessState.COMPLETED, Duration.of(210, ChronoUnit.DAYS));
        });

        assertCountProcessInstances(4);

        houseKeepingService.cleanup();

        assertCountProcessInstances(0);
    }

    @Test
    void testDeleteEventsWithoutProcess_eventWithoutProcessNotFound_eventNotDeleted() {
        inTransaction(() -> {
            Message message = createAndSaveEvent(Duration.of(91, ChronoUnit.DAYS));
            createAndSaveProcessInstance(message);
        });

        assertCountEvents(1);

        houseKeepingService.cleanup();

        assertCountEvents(1);
    }

    @Test
    void testDeleteEventsWithoutProcess_eventWithoutProcessFound_eventDeleted() {

        inTransaction(() -> createAndSaveEvent(Duration.of(91, ChronoUnit.DAYS)));

        assertCountEvents(1);
        houseKeepingService.cleanup();

        assertCountEvents(0);
    }

    @Test
    void testDeleteEventsWithoutProcess_eventNotEnoughOld_eventNotDeleted() {
        inTransaction(() -> createAndSaveEvent(Duration.of(89, ChronoUnit.DAYS)));

        assertCountEvents(1);
        houseKeepingService.cleanup();

        assertCountEvents(1);
    }

    @Test
    void testDeleteEventsWithoutProcess_eventWithoutProcessFoundWithPaging_eventDeleted() {

        inTransaction(() -> {
            createAndSaveEvent(Duration.of(210, ChronoUnit.DAYS));
            createAndSaveEvent(Duration.of(210, ChronoUnit.DAYS));
            createAndSaveEvent(Duration.of(210, ChronoUnit.DAYS));
        });

        assertCountEvents(3);

        houseKeepingService.cleanup();

        assertCountEvents(0);
    }

    @Test
    void testDeletePendingMessages_oldPendingMessage_deleted() {
        inTransaction(() -> {
            createAndSavePendingMessage(Duration.of(91, ChronoUnit.DAYS));
            createAndSavePendingMessage(Duration.of(91, ChronoUnit.DAYS));
            createAndSavePendingMessage(Duration.of(89, ChronoUnit.DAYS));
        });

        assertCountPendingMessages(3);

        houseKeepingService.cleanup();

        assertCountPendingMessages(1);
    }

    @Test
    void testDeletePendingMessages_recentPendingMessage_notDeleted() {
        inTransaction(() -> createAndSavePendingMessage(Duration.of(89, ChronoUnit.DAYS)));

        assertCountPendingMessages(1);

        houseKeepingService.cleanup();

        assertCountPendingMessages(1);
    }

    private ProcessInstance createAndSaveProcessInstance(ProcessState processState, Duration age) {
        TaskType taskType = TaskType.builder()
                .name("mandatory")
                .lifecycle(TaskLifecycle.STATIC)
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name("domainEventTriggersProcessInstantiation")
                .templateHash("hash")
                .taskTypes(List.of(
                        taskType))
                .build();
        String originProcessId = Generators.timeBasedEpochGenerator().generate().toString();
        ProcessInstance processInstance = ProcessInstance.createProcessInstance(originProcessId, processTemplate, processContextFactory);
        processInstanceRepository.save(processInstance);

        CriteriaUpdate<ProcessInstance> criteriaUpdate = entityManager.getCriteriaBuilder().createCriteriaUpdate(ProcessInstance.class);
        Root<ProcessInstance> root = criteriaUpdate.from(ProcessInstance.class);
        criteriaUpdate.set("state", processState);
        criteriaUpdate.set("createdAt", ZonedDateTime.now().minus(age));
        criteriaUpdate.where(entityManager.getCriteriaBuilder().equal(root.get("id"), processInstance.getId()));
        entityManager.createQuery(criteriaUpdate).executeUpdate();
        return processInstance;
    }

    private void createAndSaveProcessInstance(Message message) {
        TaskType taskType = TaskType.builder()
                .name("mandatory")
                .lifecycle(TaskLifecycle.STATIC)
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name("domainEventTriggersProcessInstantiation")
                .templateHash("hash")
                .taskTypes(List.of(
                        taskType))
                .build();
        String originProcessId = Generators.timeBasedEpochGenerator().generate().toString();
        ProcessInstance processInstance = ProcessInstance.createProcessInstance(originProcessId, processTemplate, processContextFactory);
        processInstanceRepository.save(processInstance);
        entityManager.flush();
        messageReferenceRepository.save(MessageReference.from(message, processInstance));
        entityManager.flush();
    }

    private void assertCountProcessInstances(int count) {
        assertThat(entityManager.createQuery("select p from ProcessInstance p").getResultList())
                .hasSize(count);
    }

    private void assertCountEvents(int count) {
        assertThat(entityManager.createQuery("select e from events e").getResultList())
                .hasSize(count);
    }

    private void assertCountPendingMessages(int count) {
        assertThat(entityManager.createNativeQuery("SELECT id FROM pending_message").getResultList())
                .hasSize(count);
    }

    private void createAndSavePendingMessage(Duration age) {
        Message message = createAndSaveEvent(age);
        String originProcessId = Generators.timeBasedEpochGenerator().generate().toString();
        pendingMessageRepository.saveIfNew(PendingMessage.from(message, originProcessId));
        entityManager.flush();
        entityManager.createNativeQuery("UPDATE pending_message SET created_at = :createdAt WHERE message_id = :messageId")
                .setParameter("createdAt", ZonedDateTime.now().minus(age))
                .setParameter("messageId", message.getId())
                .executeUpdate();
    }

    private Message createAndSaveEvent(Duration age) {
        final Message message = Message.messageBuilder()
                .messageName("eventName")
                .messageId(Generators.timeBasedEpochGenerator().generate().toString())
                .idempotenceId(Generators.timeBasedEpochGenerator().generate().toString())
                .createdAt(ZonedDateTime.now().minus(age))
                .messageCreatedAt(ZonedDateTime.now().minus(age))
                .originTaskIds(OriginTaskId.from("templateName", Set.of("taskId1", "taskId2")))
                .messageData(Set.of(MessageData.builder()
                        .templateName("templateName")
                        .key("eventDataKey")
                        .value("eventDataValue")
                        .role("eventDataRole")
                        .build()))
                .build();
        messageRepository.save(message);
        return message;
    }

    @AfterEach
    void cleanup() {
        inTransaction(() -> {
            entityManager.createNativeQuery("DELETE FROM pending_message").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM events_origin_task_ids").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM events_event_data").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM events").executeUpdate();
            processInstanceRepository.deleteAllById(
                    processInstanceRepository.findAll(Pageable.ofSize(100)).stream()
                            .map(ProcessInstance::getId).collect(toSet()));
        });
    }
}
