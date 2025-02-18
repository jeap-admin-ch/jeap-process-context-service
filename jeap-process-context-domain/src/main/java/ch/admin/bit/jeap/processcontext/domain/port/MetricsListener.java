package ch.admin.bit.jeap.processcontext.domain.port;

import ch.admin.bit.jeap.messaging.avro.AvroMessageType;
import ch.admin.bit.jeap.messaging.model.MessageType;
import ch.admin.bit.jeap.processcontext.domain.processevent.EventType;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processupdate.ProcessUpdate;

import java.util.Map;

public interface MetricsListener {

    void processUpdateFailed(ProcessUpdate update, Exception ex);

    void processInstanceCreated(String processTemplateName);

    /**
     * Counts the number of messages received
     *
     * @param messageType     the message type
     * @param firstProcessing whether the message is processed for the first time or has been received before (idempotence)
     */
    void messageReceived(MessageType messageType, boolean firstProcessing);

    /**
     * Counts the number of commands received
     *
     * @param messageType the command type
     */
    void commandReceived(AvroMessageType messageType);

    void processUpdateProcessed(ProcessTemplate template, boolean successful, int count);

    void processEventCreated(ProcessTemplate template, EventType eventType);

    void milestoneReached(ProcessTemplate template, String milestoneName);

    void processCompleted(ProcessTemplate template);

    void snapshotCreated(ProcessTemplate template);

    void timed(String name, Map<String, String> tags, Runnable runnable);

}
