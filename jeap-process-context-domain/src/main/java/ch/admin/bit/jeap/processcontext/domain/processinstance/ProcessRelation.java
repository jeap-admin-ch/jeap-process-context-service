package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessRelationPattern;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessRelationRoleType;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessRelationRoleVisibility;
import com.fasterxml.uuid.Generators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

import static lombok.AccessLevel.PACKAGE;
import static lombok.AccessLevel.PROTECTED;

@SuppressWarnings({"FieldMayBeFinal", "JpaDataSourceORMInspection"}) // JPA spec mandates non-final fields
@Entity
@Getter
@ToString
@EqualsAndHashCode
@Table(name="process_instance_process_relations")
@NoArgsConstructor(access = PROTECTED, force = true) // for JPA
public class ProcessRelation {

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
    private String name;

    @NonNull
    @Enumerated(EnumType.STRING)
    private ProcessRelationRoleType roleType;

    @NonNull
    private String originRole;

    @NonNull
    private String targetRole;

    @NonNull
    @Enumerated(EnumType.STRING)
    private ProcessRelationRoleVisibility visibilityType;

    @NonNull
    private String relatedProcessId;

    @EqualsAndHashCode.Exclude
    private ZonedDateTime createdAt;

    @Builder(access = PACKAGE)
    private ProcessRelation(@NonNull String name,
                            @NonNull ProcessRelationRoleType roleType,
                            @NonNull String originRole,
                            @NonNull String targetRole,
                            @NonNull ProcessRelationRoleVisibility visibilityTyp,
                            @NonNull String relatedProcessId) {
        this.name = name;
        this.roleType = roleType;
        this.originRole = originRole;
        this.targetRole = targetRole;
        this.visibilityType = visibilityTyp;
        this.relatedProcessId = relatedProcessId;
    }

    void onPrePersist() {
        createdAt = ZonedDateTime.now();
    }

    protected static ProcessRelation createMatchingProcessRelation(ProcessRelationPattern processRelationPattern, String relatedProcessId) {
        return ProcessRelation.builder()
                .name(processRelationPattern.getName())
                .roleType(processRelationPattern.getRoleType())
                .originRole(processRelationPattern.getOriginRole())
                .targetRole(processRelationPattern.getTargetRole())
                .visibilityTyp(processRelationPattern.getVisibility())
                .relatedProcessId(relatedProcessId)
                .build();
    }
}
