package ch.admin.bit.jeap.processcontext.domain.maintenance;

import ch.admin.bit.jeap.processcontext.domain.tx.Transactions;
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
public class BackfillJobService {
    private final MaintenanceJobRepository maintenanceJobRepository;
    private final MaintenanceCommandPublisher maintenanceCommandPublisher;
    private final MaintenanceProperties maintenanceProperties;
    private final Transactions transactions;

    public boolean submit(BackfillJobSubmission submission) {
        if (submission == null) {
            throw MaintenanceJobException.invalidRequest("Backfill job request must not be null");
        }
        NormalizedBackfillJobSubmission normalized = submission.normalized(maintenanceProperties.getLimits());
        try {
            return transactions.withinNewTransactionWithResult(() -> submitInTransaction(normalized));
        } catch (MaintenanceJobAlreadyExistsException e) {
            return transactions.withinNewTransactionWithResult(() -> handleConcurrentSubmission(normalized));
        }
    }

    private boolean submitInTransaction(NormalizedBackfillJobSubmission normalized) {
        Optional<MaintenanceJob> existingJob = maintenanceJobRepository.findById(normalized.jobId());
        if (existingJob.isPresent()) {
            ensureSameRequest(existingJob.get(), normalized);
            return false;
        }

        MaintenanceJob job = MaintenanceJob.createBackfill(normalized);
        maintenanceJobRepository.create(job);
        job.tasks().forEach(task -> maintenanceCommandPublisher.publish(job, task));
        return true;
    }

    private boolean handleConcurrentSubmission(NormalizedBackfillJobSubmission normalized) {
        MaintenanceJob concurrentlyCreatedJob = maintenanceJobRepository.findById(normalized.jobId())
                .orElseThrow(() -> MaintenanceJobException.conflict(
                        "Maintenance job already exists but could not be loaded: " + normalized.jobId()));
        ensureSameRequest(concurrentlyCreatedJob, normalized);
        return false;
    }

    public Optional<MaintenanceJob> get(UUID jobId) {
        return maintenanceJobRepository.findById(jobId)
                .filter(job -> job.jobType() == MaintenanceJobType.PROCESS_DATA_BACKFILL);
    }

    private void ensureSameRequest(MaintenanceJob existingJob, NormalizedBackfillJobSubmission submission) {
        if (!existingJob.hasSameRequest(submission)) {
            throw MaintenanceJobException.conflict(
                    "Maintenance job '%s' already exists with different content".formatted(submission.jobId()));
        }
        log.info("Maintenance job '{}' already exists with same content. Treating request as idempotent.",
                submission.jobId());
    }
}
