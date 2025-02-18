package ch.admin.bit.jeap.processcontext.domain.message;

import ch.admin.bit.jeap.processcontext.domain.ImmutableDomainEntity;
import com.fasterxml.uuid.Generators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;
import static lombok.AccessLevel.PROTECTED;

@SuppressWarnings({"FieldMayBeFinal", "JpaDataSourceORMInspection"}) // JPA spec mandates non-final fields
@NoArgsConstructor(access = PROTECTED) // for JPA
@ToString
@Entity(name = "events")
@Table(name = "events")
public class Message extends ImmutableDomainEntity {

    @Id
    @NotNull
    @Getter
    private UUID id = Generators.timeBasedEpochGenerator().generate();

    @NotNull
    @Getter
    @Column(name = "event_id")
    private String messageId;

    @NotNull
    @Getter
    private String idempotenceId;

    @NotNull
    @Getter
    @Column(name = "event_name")
    private String messageName;

    @NotNull
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "events_event_data")
    private Set<MessageData> messageData;

    @NotNull
    @Getter
    // Only required in frontend: LAZY loading
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "events_user_data")
    private Set<MessageUserData> userData;

    @NotNull
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<OriginTaskId> originTaskIds;

    @Getter
    @Column(name = "event_created_at")
    private ZonedDateTime messageCreatedAt;

    @Getter
    @Column(name = "trace_id")
    private String traceId;

    @SuppressWarnings("java:S107")
    @Builder(builderMethodName = "messageBuilder", builderClassName = "MessageBuilder")
    private static Message createMessage(@NonNull String messageId,
                                         @NonNull String idempotenceId,
                                         @NonNull String messageName,
                                         Set<MessageData> messageData,
                                         Set<MessageUserData> userData,
                                         Set<OriginTaskId> originTaskIds,
                                         ZonedDateTime createdAt,
                                         ZonedDateTime messageCreatedAt,
                                         String traceId) {
        return new Message(messageId, idempotenceId, messageName, messageData, userData, originTaskIds, createdAt, messageCreatedAt, traceId);
    }

    public Set<MessageData> getMessageData(String templateName) {
        return getMessageData().stream()
                .filter(templateMessageData -> templateMessageData.getTemplateName().equals(templateName))
                .collect(Collectors.toSet());
    }

    public Set<OriginTaskId> getOriginTaskIds(String templateName) {
        return getOriginTaskIds().stream()
                .filter(templateOriginTaskId -> templateOriginTaskId.getTemplateName().equals(templateName))
                .collect(Collectors.toSet());
    }

    public Set<MessageData> getMessageData() {
        return Collections.unmodifiableSet(messageData);
    }

    public Set<OriginTaskId> getOriginTaskIds() {
        return Collections.unmodifiableSet(originTaskIds);
    }

    public ZonedDateTime getReceivedAt() {
        return getCreatedAt();
    }

    @SuppressWarnings("java:S107")
    private Message(String messageId, String idempotenceId, String messageName, Set<MessageData> messageData, Set<MessageUserData> userData, Set<OriginTaskId> originTaskIds, ZonedDateTime createdAt, ZonedDateTime messageCreatedAt, String traceId) {
        super();
        this.messageId = messageId;
        this.idempotenceId = idempotenceId;
        this.messageName = messageName;
        this.messageData = messageData != null ? new HashSet<>(messageData) : emptySet();
        this.userData = userData != null ? new HashSet<>(userData) : emptySet();
        this.originTaskIds = originTaskIds != null ? new HashSet<>(originTaskIds) : emptySet();
        this.createdAt = createdAt;
        this.messageCreatedAt = messageCreatedAt;
        this.traceId = traceId;
    }

}
