package ch.admin.bit.jeap.processcontext.plugin.api.event;

import ch.admin.bit.jeap.messaging.model.Message;

import java.util.Set;

import static java.util.Collections.emptySet;

public interface MessageCorrelationProvider<M extends Message> {

    /**
     * Maps a message to process instances. If no ID is returned, the event will not be assigned to any process
     * instance and thus ignored.
     *
     * @param message Message
     * @return Origin Process IDs
     */
    default Set<String> getOriginProcessIds(M message) {
        return message.getOptionalProcessId()
                .map(Set::of)
                .orElse(emptySet());
    }

    /**
     * Maps a message to task(s) in a process. These IDs will be available on the message in the process context and
     * can be used by task completion conditions to correlate messages to dynamic multi-instance tasks.
     *
     * @param message Message
     * @return Origin Process ID
     */
    default Set<String> getRelatedOriginTaskIds(M message) {
        return Set.of();
    }
}
