package ch.admin.bit.jeap.processcontext.plugin.api.event;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

/**
 * @deprecated Replaced by {@link MessageData}
 */
@Getter
@EqualsAndHashCode
@ToString
@Builder
@Deprecated(since = "7.0.0", forRemoval = true)
public class EventData {
    private final String key;
    private final String value;

    private String role;

    public EventData(String key, String value) {
        Objects.requireNonNull(key, "Key is mandatory.");
        Objects.requireNonNull(value, "Value is mandatory.");
        this.key = key;
        this.value = value;
        this.role = null;
    }

    public EventData(String key, String value, String role) {
        this(key, value);
        this.role = role;
    }

    public MessageData toMessageData() {
        return MessageData.builder()
                .key(key)
                .value(value)
                .role(role)
                .build();
    }
}
