package ch.admin.bit.jeap.processcontext.domain.maintenance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@ConditionalOnProperty(value = "jeap.processcontext.maintenance.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class ReevaluationJobService {
    private final MaintenanceJobRepository maintenanceJobRepository;
    private final MaintenanceProperties maintenanceProperties;

    public void submit(ReevaluationJobSubmission submission) {
        if (submission == null) {
            throw MaintenanceJobException.invalidRequest("Reevaluation job request must not be null");
        }
        NormalizedReevaluationJobSubmission normalized = submission.normalized(maintenanceProperties.getLimits());
        Optional<MaintenanceJob> existingJob = maintenanceJobRepository.findById(normalized.jobId());
        if (existingJob.isPresent()) {
            ensureSameRequest(existingJob.get(), normalized);
            return;
        }

        try {
            maintenanceJobRepository.create(MaintenanceJob.createReevaluation(normalized));
        } catch (MaintenanceJobAlreadyExistsException e) {
            MaintenanceJob concurrentlyCreatedJob = maintenanceJobRepository.findById(normalized.jobId())
                    .orElseThrow(() -> MaintenanceJobException.conflict(
                            "Maintenance job already exists but could not be loaded: " + normalized.jobId()));
            ensureSameRequest(concurrentlyCreatedJob, normalized);
        }
    }

    public Optional<MaintenanceJob> get(UUID jobId) {
        return maintenanceJobRepository.findById(jobId);
    }

    private void ensureSameRequest(MaintenanceJob existingJob, NormalizedReevaluationJobSubmission submission) {
        if (!existingJob.hasSameRequest(submission)) {
            throw MaintenanceJobException.conflict(
                    "Maintenance job '%s' already exists with different content".formatted(submission.jobId()));
        }
        log.info("Maintenance job '{}' already exists with same content. Treating request as idempotent.", submission.jobId());
    }
}
