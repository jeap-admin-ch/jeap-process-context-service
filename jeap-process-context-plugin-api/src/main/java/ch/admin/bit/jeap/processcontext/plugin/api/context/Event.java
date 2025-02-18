package ch.admin.bit.jeap.processcontext.plugin.api.context;

import ch.admin.bit.jeap.processcontext.plugin.api.event.EventData;
import ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData;
import lombok.Builder;
import lombok.Value;

import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * @deprecated Replaced by {@link Message}
 */
@Value
@Builder
@Deprecated(since = "7.0.0", forRemoval = true)
public class Event {

    String name;

    Set<String> relatedOriginTaskIds;

    Set<MessageData> messageData;

    @SuppressWarnings("removal")
    public Set<EventData> getEventData() {
        return messageData.stream()
                .map(data -> EventData.builder()
                        .key(data.getKey())
                        .value(data.getValue())
                        .role(data.getRole())
                        .build())
                .collect(toSet());
    }
}
