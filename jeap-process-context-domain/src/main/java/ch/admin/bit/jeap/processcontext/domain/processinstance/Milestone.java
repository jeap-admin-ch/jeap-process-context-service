package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.MutableDomainEntity;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.plugin.api.condition.MilestoneCondition;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext;
import com.fasterxml.uuid.Generators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;

import java.time.ZonedDateTime;
import java.util.UUID;

import static lombok.AccessLevel.PACKAGE;
import static lombok.AccessLevel.PROTECTED;

@SuppressWarnings({"FieldMayBeFinal", "JpaDataSourceORMInspection"}) // JPA spec mandates non-final fields
@NoArgsConstructor(access = PROTECTED) // for JPA
@ToString
@Entity
@Slf4j
public class Milestone extends MutableDomainEntity {
    @Id
    @NotNull
    private UUID id = Generators.timeBasedEpochGenerator().generate();

    @NotNull
    @Getter
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Getter
    private MilestoneState state;

    @Getter
    private ZonedDateTime reachedAt;

    @Transient
    private MilestoneCondition condition;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "process_instance_id")
    @Getter(value = PACKAGE)
    private ProcessInstance processInstance;

    Milestone(String name, MilestoneCondition milestoneCondition, ProcessInstance owner, MilestoneState milestoneState) {
        this.name = name;
        this.condition = milestoneCondition;
        this.processInstance = owner;
        this.state = milestoneState;
    }

    public static Milestone createNew(String name, MilestoneCondition milestoneCondition, ProcessInstance owner) {
        return new Milestone(name, milestoneCondition, owner, MilestoneState.NOT_REACHED);
    }

    public static Milestone createNewUnknown(String name, MilestoneCondition milestoneCondition, ProcessInstance owner) {
        return new Milestone(name, milestoneCondition, owner, MilestoneState.UNKNOWN);
    }

    public boolean isReached(){
        return MilestoneState.REACHED.equals(this.state);
    }

    void evaluateIfReached(ProcessContext processContext) {
        if (isReached()) {
            return; // A reached milestone is in a final state and does not need to be evaluated
        }

        if (condition!= null && condition.isMilestoneReached(processContext)){
            this.state = MilestoneState.REACHED;
        }

        if (isReached()) {
            reachedAt = ZonedDateTime.now();
            log.info("Milestone {} reached for process {}",
                    StructuredArguments.keyValue("milestone", name),
                    StructuredArguments.keyValue("originProcessId", processInstance.getOriginProcessId()));
        }
    }

    /**
     * Set milestone condition from template after re-loading the domain object from persistent state
     */
    void setMilestoneConditionFromTemplate(ProcessTemplate processTemplate) {
        if (this.condition != null) {
            throw new IllegalStateException("Cannot set milestone condition - already set for milestone " + name + " in process " + processInstance.getOriginProcessId());
        }
        this.condition = processTemplate.getMilestoneConditionByMilestoneName(name).orElse(null);
    }

    public void delete() {
        this.state = MilestoneState.DELETED;
    }
}
