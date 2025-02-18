package ch.admin.bit.jeap.processcontext.domain.processevent;

import ch.admin.bit.jeap.processcontext.domain.MutableDomainEntity;
import com.fasterxml.uuid.Generators;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.*;

import java.util.UUID;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@NoArgsConstructor(access = PROTECTED) // for JPA
@AllArgsConstructor(access = PRIVATE)
@ToString
@Entity
public class ProcessEvent extends MutableDomainEntity {

    @Id
    @NonNull
    private UUID id;

    @NonNull
    private String originProcessId;

    @NonNull
    @Enumerated(EnumType.STRING)
    @Getter
    private EventType eventType;

    /**
     * e.g. Milestone name
     */
    @Getter
    private String name;

    static ProcessEvent createProcessStarted(String originProcessId) {
        return new ProcessEvent(Generators.timeBasedEpochGenerator().generate(), originProcessId, EventType.PROCESS_STARTED, null);
    }

    static ProcessEvent createProcessCompleted(String originProcessId) {
        return new ProcessEvent(Generators.timeBasedEpochGenerator().generate(), originProcessId, EventType.PROCESS_COMPLETED, null);
    }

    static ProcessEvent createMilestoneReached(String originProcessId, String milestoneName) {
        return new ProcessEvent(Generators.timeBasedEpochGenerator().generate(), originProcessId, EventType.MILESTONE_REACHED, milestoneName);
    }

    public static ProcessEvent createRelationAdded(String originProcessId, UUID idempotenceId) {
        return new ProcessEvent(Generators.timeBasedEpochGenerator().generate(), originProcessId, EventType.RELATION_ADDED, idempotenceId.toString());
    }

    public static ProcessEvent createSnapshotCreated(String originProcessId, String snapshotVersion) {
        return new ProcessEvent(Generators.timeBasedEpochGenerator().generate(), originProcessId, EventType.SNAPSHOT_CREATED, snapshotVersion);
    }
}
