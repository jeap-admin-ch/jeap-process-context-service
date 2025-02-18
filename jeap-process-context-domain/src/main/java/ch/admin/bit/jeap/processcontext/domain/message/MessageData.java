package ch.admin.bit.jeap.processcontext.domain.message;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString
@EqualsAndHashCode
public class MessageData {

    @NotNull
    private String templateName;

    @NotNull
    @Column(name = "key_")
    private String key;

    @NotNull
    @Column(name = "value_")
    private String value;

    private String role;

    @Builder
    private static MessageData createMessageData(
            @NonNull String templateName,
            @NonNull String key,
            @NonNull String value,
            String role) {
        return new MessageData(templateName, key, value, role);
    }

    public MessageData(String templateName, String key, String value, String role) {
        Objects.requireNonNull(templateName, "Template name is mandatory.");
        Objects.requireNonNull(key, "Key is mandatory.");
        Objects.requireNonNull(value, "Value is mandatory.");
        this.templateName = templateName;
        this.key = key;
        this.value = value;
        this.role = role;
    }

    public MessageData(String templateName, String key, String value) {
        this(templateName, key, value, null);
    }

    public static MessageData from(String templateName, ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData messageData) {
        return new MessageData(templateName, messageData.getKey(), messageData.getValue(), messageData.getRole());
    }

    public static Set<MessageData> from(String templateName, Set<ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData> messageData) {
        return messageData.stream().map(data -> from(templateName, data)).collect(Collectors.toSet());
    }

}
