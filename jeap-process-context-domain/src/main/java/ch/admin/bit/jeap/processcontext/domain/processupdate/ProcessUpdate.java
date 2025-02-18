package ch.admin.bit.jeap.processcontext.domain.processupdate;

import ch.admin.bit.jeap.processcontext.domain.MutableDomainEntity;
import com.fasterxml.uuid.Generators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Optional;
import java.util.UUID;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@SuppressWarnings("JpaDataSourceORMInspection")
@NoArgsConstructor(access = PROTECTED) // for JPA
@AllArgsConstructor(access = PRIVATE)
@ToString
@Entity
public class ProcessUpdate extends MutableDomainEntity {

    @Id
    @NotNull
    @Getter
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Getter
    private ProcessUpdateType processUpdateType;

    @Getter
    private boolean handled;

    @Getter
    private boolean failed;

    @NotNull
    @Getter
    private String originProcessId;

    @Getter
    private String params;

    @Column(name = "event_reference")
    private UUID messageReference;

    @Getter
    private String name;

    // for idempotence
    @NotNull
    @Getter
    @Column(name = "event_name")
    private String messageName;
    @NotNull
    @Getter
    private String idempotenceId;

    public Optional<UUID> getMessageReference() {
        return Optional.ofNullable(messageReference);
    }

    @Builder(builderMethodName = "messageReceived", builderClassName = "ProcessUpdateMessageReceivedBuilder")
    private static ProcessUpdate createMessageReceived(@NonNull String originProcessId, @NonNull UUID messageReference, @NonNull String messageName, @NonNull String idempotenceId) {
        return new ProcessUpdate(Generators.timeBasedEpochGenerator().generate(), ProcessUpdateType.DOMAIN_EVENT, false, false, originProcessId, null, messageReference, null, messageName, idempotenceId);
    }

    @Builder(builderMethodName = "createProcessReceived", builderClassName = "ProcessUpdateCreateProcessReceivedBuilder")
    private static ProcessUpdate createCreateProcessReceived(@NonNull String originProcessId, @NonNull String template, @NonNull UUID messageReference, @NonNull String messageName, @NonNull String idempotenceId) {
        return new ProcessUpdate(Generators.timeBasedEpochGenerator().generate(), ProcessUpdateType.CREATE_PROCESS, false, false, originProcessId, template, messageReference, null, messageName, idempotenceId);
    }

    @Builder(builderMethodName = "processCreated", builderClassName = "ProcessUpdateProcessCreatedBuilder")
    private static ProcessUpdate createProcessCreated(@NonNull String originProcessId) {
        return new ProcessUpdate(Generators.timeBasedEpochGenerator().generate(), ProcessUpdateType.PROCESS_CREATED, false, false, originProcessId, null, null, null, "createProcessInstance", originProcessId);
    }

    /**
     * Mark this update as handled
     */
    public void setHandled() {
        this.handled = true;
    }
}
