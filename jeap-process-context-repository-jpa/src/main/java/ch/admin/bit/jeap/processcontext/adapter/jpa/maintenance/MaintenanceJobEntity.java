package ch.admin.bit.jeap.processcontext.adapter.jpa.maintenance;

import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJob;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobResult;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobState;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobType;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTask;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pcs_maintenance_job")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class MaintenanceJobEntity {

    @Id
    private UUID jobId;
    @Enumerated(EnumType.STRING)
    private MaintenanceJobType jobType;
    private String processTemplateName;
    private String requestHash;
    @Enumerated(EnumType.STRING)
    private MaintenanceJobState jobState;
    @Enumerated(EnumType.STRING)
    private MaintenanceJobResult jobResult;
    private Instant startedAt;
    private Instant completedAt;
    private String startedByName;
    private String startedByExtId;
    @Version
    private int version;

    static MaintenanceJobEntity fromDomain(MaintenanceJob job) {
        return new MaintenanceJobEntity(
                job.jobId(),
                job.jobType(),
                job.processTemplateName(),
                job.requestHash(),
                job.jobState(),
                job.jobResult(),
                job.startedAt(),
                job.completedAt(),
                job.startedByName(),
                job.startedByExtId(),
                0);
    }

    MaintenanceJob toDomain(List<MaintenanceTask> tasks) {
        return new MaintenanceJob(
                jobId,
                jobType,
                processTemplateName,
                requestHash,
                jobState,
                jobResult,
                startedAt,
                completedAt,
                startedByName,
                startedByExtId,
                tasks);
    }

    void apply(MaintenanceJob job) {
        this.jobState = job.jobState();
        this.jobResult = job.jobResult();
        this.completedAt = job.completedAt();
    }
}
