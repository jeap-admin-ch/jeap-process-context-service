package ch.admin.bit.jeap.processcontext.domain.housekeeping;

import ch.admin.bit.jeap.processcontext.domain.message.MessageRepository;
import ch.admin.bit.jeap.processcontext.domain.processevent.ProcessEventRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceQueryResult;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessState;
import ch.admin.bit.jeap.processcontext.domain.processupdate.ProcessUpdateRepository;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Component
@Slf4j
public class HouseKeepingService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm:ss Z");

    private final ProcessInstanceRepository processInstanceRepository;
    private final ProcessUpdateRepository processUpdateRepository;
    private final ProcessEventRepository processEventRepository;
    private final MessageRepository messageRepository;
    private final HouseKeepingConfigProperties configProperties;
    private final TransactionTemplate transactionTemplate;
    private final Pageable pageable;

    public HouseKeepingService(ProcessInstanceRepository processInstanceRepository,
                               ProcessUpdateRepository processUpdateRepository,
                               ProcessEventRepository processEventRepository,
                               MessageRepository messageRepository,
                               HouseKeepingConfigProperties configProperties,
                               PlatformTransactionManager transactionManager) {
        this.processInstanceRepository = processInstanceRepository;
        this.processUpdateRepository = processUpdateRepository;
        this.processEventRepository = processEventRepository;
        this.messageRepository = messageRepository;
        this.configProperties = configProperties;
        this.pageable = Pageable.ofSize(configProperties.getPageSize());

        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Timed(value = "jeap_pcs_housekeeping_cleanup", percentiles = {0.5, 0.8, 0.95, 0.99})
    public void cleanup() {
        deleteProcessInstances(ProcessState.COMPLETED, configProperties.getCompletedProcessInstancesMaxAge());
        deleteProcessInstances(ProcessState.STARTED, configProperties.getStartedProcessInstancesMaxAge());
        deleteMessagesWithoutProcessCorrelation(configProperties.getEventsMaxAge());
        deleteUnhandledProcessUpdates(configProperties.getProcessUpdateMaxAge());
    }

    protected void deleteProcessInstances(ProcessState processState, Duration maxAge) {
        ZonedDateTime olderThan = ZonedDateTime.now().minus(maxAge);
        log.info("Housekeeping: find processInstances which are older than {} ({}) with processState '{}'", maxAge, olderThan.format(DATE_TIME_FORMATTER), processState);
        executeInTransactionPerPage(() -> deleteProcessInstancePage(processState, olderThan));
    }

    private boolean deleteProcessInstancePage(ProcessState processState, ZonedDateTime olderThan) {
        final Slice<ProcessInstanceQueryResult> resultPage = processInstanceRepository.findProcessInstances(processState, olderThan, pageable);
        log.info("Housekeeping: found {} processInstances", resultPage.getNumberOfElements());

        final Set<ProcessInstanceQueryResult> processInstances = resultPage.toSet();
        deleteProcessInstances(processInstances.stream().map(ProcessInstanceQueryResult::getId).collect(Collectors.toSet()));

        final Set<String> originProcessIds = processInstances.stream().map(ProcessInstanceQueryResult::getOriginProcessId).collect(Collectors.toSet());
        deleteProcessUpdates(originProcessIds);
        deleteProcessEvents(originProcessIds);
        return resultPage.hasNext();
    }

    // Process Instances (process_instance, process_instance_process_data, process_instance_relations, event_reference, milestone, task_instance)
    private void deleteProcessInstances(Set<UUID> ids) {
        log.info("Housekeeping: delete processInstances...");
        if (!ids.isEmpty()) {
            processInstanceRepository.deleteAllById(ids);
            log.info("Housekeeping: deleted {} processInstances", ids.size());
        }
    }

    // Process Updates (process_update)
    private void deleteProcessUpdates(Set<String> originProcessIds) {
        log.info("Housekeeping: delete processUpdates...");
        final long count = processUpdateRepository.countAllByOriginProcessIdIn(originProcessIds);
        processUpdateRepository.deleteAllByOriginProcessIdIn(originProcessIds);
        log.info("Housekeeping: deleted {} processUpdates", count);
    }

    // Process Events (process_event)
    private void deleteProcessEvents(Set<String> originProcessIds) {
        log.info("Housekeeping: delete processEvents...");
        final long count = processEventRepository.deleteAllByOriginProcessIdIn(originProcessIds);
        log.info("Housekeeping: deleted {} processEvents", count);
    }

    // Messages (events, events_event_data, events_origin_task_ids)
    protected void deleteMessagesWithoutProcessCorrelation(Duration maxAge) {
        ZonedDateTime olderThan = ZonedDateTime.now().minus(maxAge);
        log.info("Housekeeping: find messages which are older than {} ({}) without process correlation", maxAge, olderThan.format(DATE_TIME_FORMATTER));
        executeInTransactionPerPage(() -> deleteMessagesWithoutProcessCorrelationPage(olderThan));
    }

    private boolean deleteMessagesWithoutProcessCorrelationPage(ZonedDateTime olderThan) {
        final Slice<UUID> resultPage = messageRepository.findMessagesWithoutProcessCorrelation(olderThan, pageable);
        log.info("Housekeeping: found {} messages to delete", resultPage.getNumberOfElements());
        final Set<UUID> messageIds = resultPage.toSet();
        messageRepository.deleteMessageDataByMessageIds(messageIds);
        messageRepository.deleteMessageUserDataByMessageIds(messageIds);
        messageRepository.deleteOriginTaskIdByMessageIds(messageIds);
        messageRepository.deleteMessageByIds(messageIds);
        log.info("Housekeeping: deleted {} messages", messageIds.size());
        return resultPage.hasNext();
    }

    // Process Updates (process_update)
    protected void deleteUnhandledProcessUpdates(Duration maxAge) {
        ZonedDateTime olderThan = ZonedDateTime.now().minus(maxAge);
        log.info("Housekeeping: find process updates which are older than {} ({}) where handled = false", maxAge, olderThan.format(DATE_TIME_FORMATTER));
        executeInTransactionPerPage(() -> deleteUnhandledProcessUpdatesPage(olderThan));
    }

    private boolean deleteUnhandledProcessUpdatesPage(ZonedDateTime olderThan) {
        final Slice<UUID> resultPage = processUpdateRepository.findProcessUpdateIdWithHandledFalse(olderThan, pageable);
        final Set<UUID> processUpdateIds = resultPage.toSet();
        processUpdateRepository.deleteAllById(processUpdateIds);
        log.info("Housekeeping: deleted {} process updates", processUpdateIds.size());
        return resultPage.hasNext();
    }

    /**
     * The mix of JPQL and native queries in housekeeping requires care when querying for objects deleted by native
     * queries. A hibernate session flush is forced after every page by using a new transaction. This also reduces
     * transaction size and minimizes the duration of locks during housekeeping.
     */
    private void executeInTransactionPerPage(Supplier<Boolean> callback) {
        int pages = 0;
        while (pages < configProperties.getMaxPages()) {
            boolean hasMorePages = transactionTemplate.execute(status -> callback.get());
            if (!hasMorePages) {
                break;
            }
            pages++;
        }
    }
}
