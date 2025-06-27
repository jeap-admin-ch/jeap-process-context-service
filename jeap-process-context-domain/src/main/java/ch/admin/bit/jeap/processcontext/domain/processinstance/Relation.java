package ch.admin.bit.jeap.processcontext.domain.processinstance;

import com.fasterxml.uuid.Generators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

import static lombok.AccessLevel.PACKAGE;

@Entity
@Getter
@ToString
@EqualsAndHashCode
@Table(name="process_instance_relations")
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for JPA
public class Relation {

    @Id
    @NotNull
    @EqualsAndHashCode.Exclude
    private UUID id = Generators.timeBasedEpochGenerator().generate();

    @Setter(PACKAGE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_instance_id")
    private ProcessInstance processInstance;

    @NotNull
    private String systemId;

    @NotNull
    private String subjectType;

    @NotNull
    private String subjectId;

    @NotNull
    private String objectType;

    @NotNull
    private String objectId;

    @NotNull
    private String predicateType;

    @EqualsAndHashCode.Exclude
    private ZonedDateTime createdAt;
    /**
     * Persistent relation idempotence ID. Technical identifier used mainly to check whether a relation has alread been
     * notified externally.
     */
    @EqualsAndHashCode.Exclude
    private UUID idempotenceId;

    private String featureFlag;

    @Builder(access = PACKAGE)
    private Relation(@NonNull String systemId, @NonNull String subjectType, @NonNull String subjectId,
                     @NonNull String objectType, @NonNull String objectId,
                     @NonNull String predicateType, String featureFlag) {
        this.systemId = systemId;
        this.subjectType = subjectType;
        this.subjectId = subjectId;
        this.objectType = objectType;
        this.objectId = objectId;
        this.predicateType = predicateType;
        this.featureFlag = featureFlag;
    }

    void onPrePersist() {
        if (idempotenceId != null || createdAt != null) {
            throw new IllegalStateException("Modifying idempotenceId/createdAt not allowed for persistent Relation entity");
        }
        idempotenceId = Generators.timeBasedEpochGenerator().generate();
        createdAt = ZonedDateTime.now();
    }
}
