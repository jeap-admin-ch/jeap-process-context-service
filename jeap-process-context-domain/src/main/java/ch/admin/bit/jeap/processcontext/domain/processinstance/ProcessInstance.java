package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.MutableDomainEntity;
import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextFactory;
import ch.admin.bit.jeap.processcontext.domain.processinstance.snapshot.ProcessSnapshotConditionResult;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.plugin.api.condition.ProcessCompletionCondition;
import ch.admin.bit.jeap.processcontext.plugin.api.condition.ProcessCompletionConditionResult;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext;
import com.fasterxml.uuid.Generators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;
import java.util.*;

import static ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessCompletionConclusion.SUCCEEDED;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static lombok.AccessLevel.PROTECTED;
import static org.springframework.util.StringUtils.hasText;

@SuppressWarnings({"FieldMayBeFinal", "JpaDataSourceORMInspection"}) // JPA spec mandates non-final fields
@NoArgsConstructor(access = PROTECTED) // for JPA
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Slf4j
public class ProcessInstance extends MutableDomainEntity {

    @ToString.Include
    @Id
    @NotNull
    @Getter
    private UUID id = Generators.timeBasedEpochGenerator().generate();

    @ToString.Include
    @Column(unique = true)
    @NotNull
    @Getter
    private String originProcessId;

    @ToString.Include
    @NotNull
    @Enumerated(EnumType.STRING)
    @Getter
    private ProcessState state;

    @Embedded
    ProcessCompletion processCompletion;

    @ToString.Include
    @NotNull
    @Column(name = "template_name")
    @Getter
    private String processTemplateName;

    @Column(name = "template_hash")
    @Getter
    private String processTemplateHash;

    @Transient
    @Getter
    private ProcessTemplate processTemplate;

    @Transient
    private ProcessContextFactory processContextFactory;

    @Getter
    private int latestSnapshotVersion = 0;

    @Getter
    @Convert(converter = SnapshotNameSetConverter.class)
    private SequencedSet<String> snapshotNames = new LinkedHashSet<>();

    private ProcessInstance(String originProcessId, ProcessTemplate processTemplate, ProcessContextFactory processContextFactory) {
        Objects.requireNonNull(originProcessId, "Origin process ID is mandatory");
        Objects.requireNonNull(processTemplate, "Process template is mandatory");
        Objects.requireNonNull(processContextFactory, "Process context factory is mandatory");
        this.originProcessId = originProcessId;
        this.processTemplate = processTemplate;
        this.processTemplateName = processTemplate.getName();
        this.processTemplateHash = processTemplate.getTemplateHash();
        this.processContextFactory = processContextFactory;
        this.state = ProcessState.STARTED;
        this.createdAt = ZonedDateTime.now();
    }

    /**
     * Creates a new ProcessInstance based on the given processTemplate.
     *
     * @param originProcessId       origin process id
     * @param processTemplate       the process template to base the process instance on
     * @param processContextFactory the process context factory to create process contexts for PCS API calls (conditions etc.)
     * @return A new ProcessInstance
     */
    public static ProcessInstance createProcessInstance(String originProcessId, ProcessTemplate processTemplate, ProcessContextFactory processContextFactory) {
        return new ProcessInstance(originProcessId, processTemplate, processContextFactory);
    }

    public int nextSnapshotVersion() {
        latestSnapshotVersion++;
        return latestSnapshotVersion;
    }

    public void registerSnapshot(String snapshotName) {
        snapshotNames.add(snapshotName);
    }

    /**
     * Evaluate the snapshot conditions defined for this process and for every condition that
     * triggers a new snapshot return the name of the snapshot triggered.
     *
     * @return The names of newly triggered snapshots as determined by the snapshot conditions.
     */
    Set<String> evaluateSnapshotConditions() {
        if ((getProcessTemplate() == null) || getProcessTemplate().getProcessSnapshotConditions().isEmpty()) {
            return emptySet();
        }
        return getProcessTemplate().getProcessSnapshotConditions().stream().
                map(snapshotCondition -> snapshotCondition.triggerSnapshot(this)).
                filter(ProcessSnapshotConditionResult::isSnapShotTriggered).
                map(ProcessSnapshotConditionResult::getSnapshotName).
                filter(snapshotName -> !snapshotNames.contains(snapshotName)).
                collect(toSet());
    }

    public Optional<ProcessCompletion> getProcessCompletion() {
        ProcessCompletion reportedCompletion = processCompletion;
        if ((state == ProcessState.COMPLETED) && (reportedCompletion == null)) {
            // this is an 'old' process instance that completed without setting completion data -> create derived completion data
            ZonedDateTime completedAt = getModifiedAt() == null ? ZonedDateTime.now() : getModifiedAt();
            reportedCompletion = new ProcessCompletion(SUCCEEDED, "All tasks completed.", completedAt);
        }
        return Optional.ofNullable(reportedCompletion);
    }

    /**
     * Set process template and process context factory after re-loading the domain object from persistent state
     */
    public void onAfterLoadFromPersistentState(ProcessTemplate processTemplate, ProcessContextFactory processContextFactory) {
        this.processContextFactory = processContextFactory;
        this.processTemplate = processTemplate;
    }

    void updateState() {
        if (state == ProcessState.COMPLETED) {
            // final state
            return;
        }
        ProcessContext processContext = processContextFactory.createProcessContext(this);
        for (ProcessCompletionCondition condition : processTemplate.getProcessCompletionConditions()) {
            ProcessCompletionConditionResult processCompleted = condition.isProcessCompleted(processContext);
            if (processCompleted.isCompleted()) {
                this.state = ProcessState.COMPLETED;
                this.processCompletion = new ProcessCompletion(
                        ProcessCompletionConclusion.valueOf(processCompleted.getConclusion().orElseThrow().name()),
                        processCompleted.getName().orElse(null),
                        ZonedDateTime.now());
                return;
            }
        }
    }

    @Override
    public ZonedDateTime getCreatedAt() {
        return super.getCreatedAt();
    }

    @Override
    public ZonedDateTime getModifiedAt() {
        return super.getModifiedAt();
    }

    void updateTemplateHash() {
        this.processTemplateHash = processTemplate.getTemplateHash();
    }

    @Converter
    public static class SnapshotNameSetConverter implements AttributeConverter<LinkedHashSet<String>, String> {
        private static final String SEPARATOR_CHAR = " ";

        @Override
        public String convertToDatabaseColumn(LinkedHashSet<String> snapshotNames) {
            return snapshotNames != null ? String.join(SEPARATOR_CHAR, snapshotNames) : "";
        }

        @Override
        public LinkedHashSet<String> convertToEntityAttribute(String snapshotNamesString) {
            return hasText(snapshotNamesString) ? new LinkedHashSet<>(Arrays.asList(snapshotNamesString.split(SEPARATOR_CHAR))) : new LinkedHashSet<>();
        }
    }
}
