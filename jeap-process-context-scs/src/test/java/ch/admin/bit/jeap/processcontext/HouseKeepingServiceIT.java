package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.domain.housekeeping.HouseKeepingService;
import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.message.MessageData;
import ch.admin.bit.jeap.processcontext.domain.message.MessageRepository;
import ch.admin.bit.jeap.processcontext.domain.message.OriginTaskId;
import ch.admin.bit.jeap.processcontext.domain.processevent.ProcessEvent;
import ch.admin.bit.jeap.processcontext.domain.processevent.ProcessEventRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessState;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskCardinality;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskLifecycle;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskType;
import ch.admin.bit.jeap.processcontext.domain.processupdate.ProcessUpdate;
import ch.admin.bit.jeap.processcontext.domain.processupdate.ProcessUpdateRepository;
import com.fasterxml.uuid.Generators;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

class HouseKeepingServiceIT extends ProcessInstanceMockS3ITBase {

    @Autowired
    private HouseKeepingService houseKeepingService;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private ProcessUpdateRepository processUpdateRepository;

    @Autowired
    private ProcessEventRepository processEventRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Test
    void testDeleteProcessInstances_completedProcessInstances_processInstancesDeleted() {
        List<String> expiredProcessIds = new ArrayList<>();
        List<String> keptProcessIds = new ArrayList<>();
        inTransaction(() -> {
            expiredProcessIds.add(createAndSaveProcessInstance(ProcessState.COMPLETED, Duration.of(210, ChronoUnit.DAYS)).getOriginProcessId());
            expiredProcessIds.add(createAndSaveProcessInstance(ProcessState.COMPLETED, Duration.of(210, ChronoUnit.DAYS)).getOriginProcessId());
            keptProcessIds.add(createAndSaveProcessInstance(ProcessState.COMPLETED, Duration.of(179, ChronoUnit.DAYS)).getOriginProcessId());
            keptProcessIds.add(createAndSaveProcessInstance(ProcessState.STARTED, Duration.of(181, ChronoUnit.DAYS)).getOriginProcessId());
        });

        assertCountProcessInstances(4);
        assertPresent(expiredProcessIds);

        houseKeepingService.cleanup();

        assertCountProcessInstances(2);
        assertNotPresent(expiredProcessIds);
        assertPresent(keptProcessIds);
    }

    private void inTransaction(Runnable runnable) {
        new TransactionTemplate(platformTransactionManager)
                .executeWithoutResult(status -> runnable.run());
    }

    @Test
    void testDeleteProcessInstances_veryOldStartedProcessInstances_processInstancesDeleted() {
        List<String> expiredProcessIds = new ArrayList<>();
        List<String> keptProcessIds = new ArrayList<>();
        inTransaction(() -> {
            expiredProcessIds.add(createAndSaveProcessInstance(ProcessState.STARTED, Duration.of(366, ChronoUnit.DAYS)).getOriginProcessId());
            expiredProcessIds.add(createAndSaveProcessInstance(ProcessState.STARTED, Duration.of(366, ChronoUnit.DAYS)).getOriginProcessId());
            keptProcessIds.add(createAndSaveProcessInstance(ProcessState.STARTED, Duration.of(364, ChronoUnit.DAYS)).getOriginProcessId());
            keptProcessIds.add(createAndSaveProcessInstance(ProcessState.STARTED, Duration.of(364, ChronoUnit.DAYS)).getOriginProcessId());
        });

        assertCountProcessInstances(4);
        assertPresent(expiredProcessIds);

        houseKeepingService.cleanup();

        assertCountProcessInstances(2);
        assertNotPresent(expiredProcessIds);
        assertPresent(keptProcessIds);
    }

    @Test
    void testDeleteProcessInstances_completedProcessInstancesWithPaging_processInstancesDeleted() {
        List<String> expiredProcessIds = new ArrayList<>();
        inTransaction(() -> {
            expiredProcessIds.add(createAndSaveProcessInstance(ProcessState.COMPLETED, Duration.of(210, ChronoUnit.DAYS)).getOriginProcessId());
            expiredProcessIds.add(createAndSaveProcessInstance(ProcessState.COMPLETED, Duration.of(210, ChronoUnit.DAYS)).getOriginProcessId());
            expiredProcessIds.add(createAndSaveProcessInstance(ProcessState.COMPLETED, Duration.of(210, ChronoUnit.DAYS)).getOriginProcessId());
            expiredProcessIds.add(createAndSaveProcessInstance(ProcessState.COMPLETED, Duration.of(210, ChronoUnit.DAYS)).getOriginProcessId());
        });

        assertCountProcessInstances(4);
        assertPresent(expiredProcessIds);

        houseKeepingService.cleanup();

        assertCountProcessInstances(0);
        assertNotPresent(expiredProcessIds);
    }

    @Test
    void testDeleteProcessUpdates_handledFalseAndFailedFalse_processUpdatesDeleted() {
        List<String> expiredProcessIds = new ArrayList<>();
        List<String> keptProcessIds = new ArrayList<>();
        inTransaction(() -> {
            keptProcessIds.add(createAndSaveProcessUpdate("jrn1", Duration.of(89, ChronoUnit.DAYS), false).getOriginProcessId());
            expiredProcessIds.add(createAndSaveProcessUpdate("jrn2", Duration.of(91, ChronoUnit.DAYS), false).getOriginProcessId());
            keptProcessIds.add(createAndSaveProcessUpdate("jrn3", Duration.of(89, ChronoUnit.DAYS), true).getOriginProcessId());
            keptProcessIds.add(createAndSaveProcessUpdate("jrn4", Duration.of(91, ChronoUnit.DAYS), true).getOriginProcessId());
        });


        assertCountProcessUpdates(4);
        assertProcessUpdatePresent(expiredProcessIds);
        assertProcessUpdatePresent(keptProcessIds);

        houseKeepingService.cleanup();

        assertCountProcessUpdates(3);
        assertNotPresent(expiredProcessIds);
        assertProcessUpdatePresent(keptProcessIds);
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

    private void assertPresent(List<String> originProcessIds) {
        originProcessIds.forEach(originProcessId -> {
            assertThat(processUpdateRepository.findByOriginProcessIdAndMessageNameAndIdempotenceId(originProcessId, "test", "test")).isPresent();
            assertThat(processEventRepository.findByOriginProcessId(originProcessId)).hasSize(1);
        });
    }

    private void assertProcessUpdatePresent(List<String> originProcessIds) {
        originProcessIds.forEach(originProcessId -> assertThat(processUpdateRepository.findByOriginProcessIdAndMessageNameAndIdempotenceId(originProcessId, "test", "test")).isPresent()
        );
    }

    private void assertNotPresent(List<String> originProcessIds) {
        originProcessIds.forEach(originProcessId -> {
            assertThat(processUpdateRepository.findByOriginProcessIdAndMessageNameAndIdempotenceId(originProcessId, "test", "test")).isNotPresent();
            assertThat(processEventRepository.findByOriginProcessId(originProcessId)).isEmpty();
        });
    }

    private ProcessInstance createAndSaveProcessInstance(ProcessState processState, Duration age) {
        TaskType taskType = TaskType.builder()
                .name("mandatory")
                .lifecycle(TaskLifecycle.STATIC)
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name("domainEvents")
                .templateHash("hash")
                .taskTypes(List.of(
                        taskType))
                .build();
        String originProcessId = Generators.timeBasedEpochGenerator().generate().toString();
        ProcessInstance processInstance = ProcessInstance.startProcess(originProcessId, processTemplate, Collections.emptySet());
        processInstanceRepository.save(processInstance);

        CriteriaUpdate<ProcessInstance> criteriaUpdate = entityManager.getCriteriaBuilder().createCriteriaUpdate(ProcessInstance.class);
        Root<ProcessInstance> root = criteriaUpdate.from(ProcessInstance.class);
        criteriaUpdate.set("state", processState);
        criteriaUpdate.set("modifiedAt", ZonedDateTime.now().minus(age));
        criteriaUpdate.where(entityManager.getCriteriaBuilder().equal(root.get("id"), processInstance.getId()));
        entityManager.createQuery(criteriaUpdate).executeUpdate();
        processUpdateRepository.save(ProcessUpdate.messageReceived().originProcessId(originProcessId).messageName("test").messageReference(Generators.timeBasedEpochGenerator().generate()).idempotenceId("test").build());
        processEventRepository.saveAll(Set.of(ProcessEvent.createRelationAdded(originProcessId, Generators.timeBasedEpochGenerator().generate())));
        return processInstance;
    }

    private void createAndSaveProcessInstance(Message message) {
        TaskType taskType = TaskType.builder()
                .name("mandatory")
                .lifecycle(TaskLifecycle.STATIC)
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .build();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name("domainEvents")
                .templateHash("hash")
                .taskTypes(List.of(
                        taskType))
                .build();
        String originProcessId = Generators.timeBasedEpochGenerator().generate().toString();
        ProcessInstance processInstance = ProcessInstance.startProcess(originProcessId, processTemplate, Collections.emptySet());
        processInstance.addMessage(message);
        processInstanceRepository.save(processInstance);
    }

    private void assertCountProcessInstances(int count) {
        assertThat(entityManager.createQuery("select p from ProcessInstance p").getResultList()).hasSize(count);
    }

    private void assertCountEvents(int count) {
        assertThat(entityManager.createQuery("select e from events e").getResultList()).hasSize(count);
    }

    private void assertCountProcessUpdates(int count) {
        assertThat(entityManager.createQuery("select p from ProcessUpdate p").getResultList()).hasSize(count);
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

    private ProcessUpdate createAndSaveProcessUpdate(String originProcessId, Duration age, boolean handled) {
        final ProcessUpdate processUpdate = ProcessUpdate.messageReceived()
                .originProcessId(originProcessId)
                .messageName("test")
                .messageReference(Generators.timeBasedEpochGenerator().generate())
                .idempotenceId("test")
                .build();
        processUpdateRepository.save(processUpdate);

        CriteriaUpdate<ProcessUpdate> criteriaUpdateCreatedAt = entityManager.getCriteriaBuilder().createCriteriaUpdate(ProcessUpdate.class);
        Root<ProcessUpdate> criteriaUpdateCreatedAtRoot = criteriaUpdateCreatedAt.from(ProcessUpdate.class);
        criteriaUpdateCreatedAt.set("createdAt", ZonedDateTime.now().minus(age));
        criteriaUpdateCreatedAt.where(entityManager.getCriteriaBuilder().equal(criteriaUpdateCreatedAtRoot.get("originProcessId"), processUpdate.getOriginProcessId()));
        entityManager.createQuery(criteriaUpdateCreatedAt).executeUpdate();

        if (handled) {
            CriteriaUpdate<ProcessUpdate> criteriaUpdateHandled = entityManager.getCriteriaBuilder().createCriteriaUpdate(ProcessUpdate.class);
            Root<ProcessUpdate> criteriaUpdateHandledRoot = criteriaUpdateHandled.from(ProcessUpdate.class);
            criteriaUpdateHandled.set("handled", Boolean.TRUE);
            criteriaUpdateHandled.where(entityManager.getCriteriaBuilder().equal(criteriaUpdateHandledRoot.get("originProcessId"), processUpdate.getOriginProcessId()));
            entityManager.createQuery(criteriaUpdateHandled).executeUpdate();
        }
        return processUpdate;
    }

    @AfterEach
    void cleanup() {
        inTransaction(() -> {
            entityManager.createNativeQuery("DELETE FROM events_origin_task_ids").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM events_event_data").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM events").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM process_event").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM process_update").executeUpdate();
            processInstanceRepository.deleteAllById(
                    processInstanceRepository.findAll(Pageable.ofSize(100)).stream()
                            .map(ProcessInstance::getId).collect(toSet()));
        });
    }
}
