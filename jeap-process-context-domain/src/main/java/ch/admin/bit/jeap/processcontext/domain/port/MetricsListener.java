package ch.admin.bit.jeap.processcontext.domain.port;

import ch.admin.bit.jeap.messaging.model.MessageType;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;

import java.util.Map;
import java.util.function.Supplier;

public interface MetricsListener {

    void processUpdateFailed();

    void processInstanceCreated(String processTemplateName);

    /**
     * Counts the number of messages received
     *
     * @param messageType     the message type
     * @param firstProcessing whether the message is processed for the first time or has been received before (idempotence)
     */
    void messageReceived(MessageType messageType, boolean firstProcessing);

    void processUpdateProcessed(ProcessTemplate template);

    void processCompleted(ProcessTemplate template);

    void snapshotCreated(ProcessTemplate template);

    void timed(String name, Map<String, String> tags, Runnable runnable);

    <T> T timedWithReturnValue(String name, Supplier<T> supplier);

}
