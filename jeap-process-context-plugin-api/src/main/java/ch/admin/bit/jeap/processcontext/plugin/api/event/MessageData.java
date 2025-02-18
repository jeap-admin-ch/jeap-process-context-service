package ch.admin.bit.jeap.processcontext.plugin.api.event;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

@Getter
@EqualsAndHashCode
@ToString
@Builder
public class MessageData {

    private final String key;
    private final String value;

    private String role;

    public MessageData(String key, String value) {
        Objects.requireNonNull(key, "Key is mandatory.");
        Objects.requireNonNull(value, "Value is mandatory.");
        this.key = key;
        this.value = value;
        this.role = null;
    }

    public MessageData(String key, String value, String role) {
        this(key, value);
        this.role = role;
    }
}
