package ch.admin.bit.jeap.processcontext.domain.processupdate;

import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.port.InternalMessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.stereotype.Component;


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
    private static final String MESSAGE_NAME = "messageName";
    private static final String ORIGIN_PROCESS_ID = "originProcessId";

    private final InternalMessageProducer internalMessageProducer;

    public void messageReceived(String originProcessId, Message message) {
        internalMessageProducer.produceProcessContextOutdatedEventSynchronously(originProcessId,
                message.getId(), message.getMessageName(), message.getIdempotenceId());
        log.info("Message {} for {} received",
                StructuredArguments.keyValue(MESSAGE_NAME, message.getMessageName()),
                StructuredArguments.keyValue(ORIGIN_PROCESS_ID, originProcessId));
    }

    public void processCreatingMessageReceived(String originProcessId, Message message, String templateName) {
        internalMessageProducer.produceProcessContextOutdatedCreateProcessEventSynchronously(originProcessId,
                message.getId(), message.getMessageName(), message.getIdempotenceId(), templateName);
        log.info("Message {} for {} received, marking for process creation using template {}",
                StructuredArguments.keyValue(MESSAGE_NAME, message.getMessageName()),
                StructuredArguments.keyValue(ORIGIN_PROCESS_ID, originProcessId),
                templateName);
    }
}
