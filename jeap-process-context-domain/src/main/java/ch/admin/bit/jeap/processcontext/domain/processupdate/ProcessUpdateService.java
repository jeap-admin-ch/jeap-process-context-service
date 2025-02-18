package ch.admin.bit.jeap.processcontext.domain.processupdate;

import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.port.InternalMessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;


/**
 * This service listens to change request coming in e.g. with TaskPlanned and TaskCompleted Events
 * It stores the needed change as an ProcessUpdate and then triggers an internal event to actually perform these updates
 * <p>
 * Reason for this is that we need to ensure that updates to the same process are not worked on in parallel.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessUpdateService {
    private final InternalMessageProducer internalMessageProducer;
    private final ProcessUpdateRepository processUpdateRepository;
    private final PlatformTransactionManager transactionManager;

    public void createProcessReceived(String originProcessId, String template, Message message) {
        withinTransaction(() -> createAndSaveCreateProcessIfNeeded(originProcessId, message, template));

        internalMessageProducer.produceProcessContextOutdatedEventSynchronously(originProcessId);
        log.info("CreateProcess message {} for {} received",
                StructuredArguments.keyValue("messageName", message.getMessageName()),
                StructuredArguments.keyValue("originProcessId", originProcessId));
    }


    public void messageReceived(String originProcessId, Message message) {
        withinTransaction(() -> createAndSaveMessageIfNeeded(originProcessId, message));

        internalMessageProducer.produceProcessContextOutdatedEventSynchronously(originProcessId);
        log.info("Message {} for {} received",
                StructuredArguments.keyValue("messageName", message.getMessageName()),
                StructuredArguments.keyValue("originProcessId", originProcessId));
    }


    private void createAndSaveMessageIfNeeded(String originProcessId, Message message) {
        String messageName = message.getMessageName();
        String idempotenceId = message.getIdempotenceId();
        if (processUpdateRepository.findByOriginProcessIdAndMessageNameAndIdempotenceId(originProcessId, messageName, idempotenceId).isPresent()) {
            log.info("Message {} {} has already been received for process {}.",
                    StructuredArguments.keyValue("messageName", messageName),
                    StructuredArguments.keyValue("idempotenceId", idempotenceId),
                    StructuredArguments.keyValue("originProcessId", originProcessId));
        } else {
            ProcessUpdate processUpdate = ProcessUpdate.messageReceived()
                    .originProcessId(originProcessId)
                    .messageReference(message.getId())
                    .messageName(messageName)
                    .idempotenceId(idempotenceId)
                    .build();
            processUpdateRepository.save(processUpdate);
        }
    }

    private void createAndSaveCreateProcessIfNeeded(String originProcessId, Message message, String template) {
        String messageName = message.getMessageName();
        String idempotenceId = message.getIdempotenceId();
        if (processUpdateRepository.findByOriginProcessIdAndMessageNameAndIdempotenceId(originProcessId, messageName, idempotenceId).isPresent()) {
            log.info("CreateProcess message {} {} has already been received for process {}.",
                    StructuredArguments.keyValue("messageName", messageName),
                    StructuredArguments.keyValue("idempotenceId", idempotenceId),
                    StructuredArguments.keyValue("originProcessId", originProcessId));
        } else {
            ProcessUpdate processUpdate = ProcessUpdate.createProcessReceived()
                    .originProcessId(originProcessId)
                    .template(template)
                    .messageReference(message.getId())
                    .messageName(messageName)
                    .idempotenceId(idempotenceId)
                    .build();
            processUpdateRepository.save(processUpdate);
        }
    }

    private void withinTransaction(Runnable callback) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> callback.run());
    }
}
