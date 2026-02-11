package ch.admin.bit.jeap.processcontext.domain.housekeeping;

import ch.admin.bit.jeap.processcontext.domain.message.MessageRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.PendingMessageRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceQueryResult;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessState;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

@Component
@Slf4j
public class HouseKeepingService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm:ss Z");

    private final ProcessInstanceRepository processInstanceRepository;
    private final MessageRepository messageRepository;
    private final PendingMessageRepository pendingMessageRepository;
    private final HouseKeepingConfigProperties configProperties;
    private final TransactionTemplate transactionTemplate;
    private final Pageable pageable;

    public HouseKeepingService(ProcessInstanceRepository processInstanceRepository,
                               MessageRepository messageRepository,
                               PendingMessageRepository pendingMessageRepository,
                               HouseKeepingConfigProperties configProperties,
                               PlatformTransactionManager transactionManager) {
        this.processInstanceRepository = processInstanceRepository;
        this.messageRepository = messageRepository;
        this.pendingMessageRepository = pendingMessageRepository;
        this.configProperties = configProperties;
        this.pageable = Pageable.ofSize(configProperties.getPageSize());

        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Timed(value = "jeap_pcs_housekeeping_cleanup", percentiles = {0.5, 0.8, 0.99})
    public void cleanup() {
        deleteProcessInstances(ProcessState.COMPLETED, configProperties.getCompletedProcessInstancesMaxAge());
        deleteProcessInstances(ProcessState.STARTED, configProperties.getStartedProcessInstancesMaxAge());
        deletePendingMessages(configProperties.getEventsMaxAge());
        deleteMessagesWithoutProcessCorrelation(configProperties.getEventsMaxAge());
    }

    protected void deleteProcessInstances(ProcessState processState, Duration maxAge) {
        ZonedDateTime olderThan = ZonedDateTime.now().minus(maxAge);
        log.info("Housekeeping: find processInstances which are older than {} ({}) with processState '{}'", maxAge, olderThan.format(DATE_TIME_FORMATTER), processState);
        executeInTransactionPerPage(() -> deleteProcessInstancePage(processState, olderThan));
    }

    private boolean deleteProcessInstancePage(ProcessState processState, ZonedDateTime olderThan) {
        Slice<ProcessInstanceQueryResult> resultPage = processInstanceRepository.findProcessInstances(processState, olderThan, pageable);
        log.info("Housekeeping: found {} processInstances", resultPage.getNumberOfElements());

        Set<ProcessInstanceQueryResult> processInstances = resultPage.toSet();
        deleteProcessInstances(processInstances.stream().map(ProcessInstanceQueryResult::getId).collect(Collectors.toSet()));

        return resultPage.hasNext();
    }

    // Process Instances (process_instance, process_instance_process_data, process_instance_relations, event_reference, task_instance)
    private void deleteProcessInstances(Set<UUID> ids) {
        log.info("Housekeeping: delete processInstances...");
        if (!ids.isEmpty()) {
            processInstanceRepository.deleteAllById(ids);
            log.info("Housekeeping: deleted {} processInstances", ids.size());
        }
    }

    // PendingMessages (message_id)
    protected void deletePendingMessages(Duration maxAge) {
        ZonedDateTime olderThan = ZonedDateTime.now().minus(maxAge);
        log.info("Housekeeping: find pending mesages which are older than {} ({})", maxAge, olderThan.format(DATE_TIME_FORMATTER));
        executeInTransactionPerPage(() -> deletePendingMessages(olderThan));
    }

    private boolean deletePendingMessages(ZonedDateTime olderThan) {
        Slice<UUID> resultPage = pendingMessageRepository.findPendingMessagesCreatedBefore(olderThan, pageable);
        log.info("Housekeeping: found {} pending messages to delete", resultPage.getNumberOfElements());
        Set<UUID> pendingMessageIds = resultPage.toSet();
        pendingMessageRepository.deleteAll(pendingMessageIds);
        log.info("Housekeeping: deleted {} pending messages", pendingMessageIds.size());
        return resultPage.hasNext();
    }

    // Messages (events, events_event_data, events_origin_task_ids)
    protected void deleteMessagesWithoutProcessCorrelation(Duration maxAge) {
        ZonedDateTime olderThan = ZonedDateTime.now().minus(maxAge);
        log.info("Housekeeping: find messages which are older than {} ({}) without process correlation", maxAge, olderThan.format(DATE_TIME_FORMATTER));
        executeInTransactionPerPage(() -> deleteMessagesWithoutProcessCorrelationPage(olderThan));
    }

    private boolean deleteMessagesWithoutProcessCorrelationPage(ZonedDateTime olderThan) {
        Slice<UUID> resultPage = messageRepository.findMessagesWithoutProcessCorrelation(olderThan, pageable);
        log.info("Housekeeping: found {} messages to delete", resultPage.getNumberOfElements());
        Set<UUID> messageIds = resultPage.toSet();
        messageRepository.deleteMessageDataByMessageIds(messageIds);
        messageRepository.deleteMessageUserDataByMessageIds(messageIds);
        messageRepository.deleteOriginTaskIdByMessageIds(messageIds);
        messageRepository.deleteMessageByIds(messageIds);
        log.info("Housekeeping: deleted {} messages", messageIds.size());
        return resultPage.hasNext();
    }

    /**
     * The mix of JPQL and native queries in housekeeping requires care when querying for objects deleted by native
     * queries. A hibernate session flush is forced after every page by using a new transaction. This also reduces
     * transaction size and minimizes the duration of locks during housekeeping.
     */
    private void executeInTransactionPerPage(BooleanSupplier callback) {
        int pages = 0;
        while (pages < configProperties.getMaxPages()) {
            Boolean hasMorePages = transactionTemplate.<Boolean>execute(status -> callback.getAsBoolean());
            if (hasMorePages == null || !hasMorePages) {
                break;
            }
            pages++;
        }
    }
}
