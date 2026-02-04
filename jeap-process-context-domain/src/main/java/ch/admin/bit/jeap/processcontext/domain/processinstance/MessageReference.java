package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.ImmutableDomainEntity;
import ch.admin.bit.jeap.processcontext.domain.message.Message;
import com.fasterxml.uuid.Generators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import static lombok.AccessLevel.PROTECTED;

@SuppressWarnings({"FieldMayBeFinal", "JpaDataSourceORMInspection"}) // JPA spec mandates non-final fields
@NoArgsConstructor(access = PROTECTED) // for JPA
@ToString
@Entity
@Table(name = "event_reference")
public class MessageReference extends ImmutableDomainEntity {

    @Id
    @NotNull
    @Getter
    private UUID id = Generators.timeBasedEpochGenerator().generate();

    @NotNull
    @Getter
    @Column(name = "events_id")
    private UUID messageId;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_instance_id")
    @Getter
    private ProcessInstance processInstance;

    public static MessageReference from(Message message, ProcessInstance processInstance) {
        MessageReference messageReference = new MessageReference(message);
        messageReference.setOwner(processInstance);
        return messageReference;
    }

    private MessageReference(Message message) {
        Objects.requireNonNull(message, "Message must not be null.");
        this.messageId = message.getId();
    }

    private void setOwner(ProcessInstance owner) {
        this.processInstance = owner;
    }

    @Override
    public ZonedDateTime getCreatedAt() {
        return super.getCreatedAt();
    }
}
