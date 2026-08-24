package ch.admin.bit.jeap.processcontext.domain.maintenance;

import ch.admin.bit.jeap.processcontext.domain.processinstance.RelationRepository;
import ch.admin.bit.jeap.processcontext.domain.tx.Transactions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@ConditionalOnProperty(value = "jeap.processcontext.maintenance.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class RelationPublicationJobService {
    private final MaintenanceJobRepository maintenanceJobRepository;
    private final MaintenanceEventPublisher maintenanceEventPublisher;
    private final RelationRepository relationRepository;
    private final MaintenanceProperties maintenanceProperties;
    private final Transactions transactions;

    public boolean submit(RelationPublicationJobSubmission submission) {
        if (submission == null) {
            throw MaintenanceJobException.invalidRequest("Relation republication job request must not be null");
        }
        NormalizedRelationPublicationJobSubmission normalized =
                submission.normalized(maintenanceProperties.getLimits());
        try {
            return transactions.withinNewTransactionWithResult(() -> submitInTransaction(normalized));
        } catch (MaintenanceJobAlreadyExistsException e) {
            return transactions.withinNewTransactionWithResult(() -> handleConcurrentSubmission(normalized));
        }
    }

    private boolean submitInTransaction(NormalizedRelationPublicationJobSubmission submission) {
        Optional<MaintenanceJob> existingJob = maintenanceJobRepository.findById(submission.jobId());
        if (existingJob.isPresent()) {
            ensureSameRequest(existingJob.get(), submission);
            return false;
        }

        Map<UUID, String> relationOwners =
                relationRepository.findOriginProcessIdsByIds(submission.relationIds());
        MaintenanceJob job = MaintenanceJob.createRepublication(submission, relationOwners);
        maintenanceJobRepository.create(job);
        job.tasks().forEach(task -> maintenanceEventPublisher.publish(job, task));
        return true;
    }

    private boolean handleConcurrentSubmission(NormalizedRelationPublicationJobSubmission submission) {
        MaintenanceJob concurrentlyCreatedJob = maintenanceJobRepository.findById(submission.jobId())
                .orElseThrow(() -> MaintenanceJobException.conflict(
                        "Maintenance job already exists but could not be loaded: " + submission.jobId()));
        ensureSameRequest(concurrentlyCreatedJob, submission);
        return false;
    }

    public Optional<MaintenanceJob> get(UUID jobId) {
        return maintenanceJobRepository.findById(jobId)
                .filter(job -> job.jobType() == MaintenanceJobType.RELATION_REPUBLICATION);
    }

    private void ensureSameRequest(MaintenanceJob existingJob,
                                   NormalizedRelationPublicationJobSubmission submission) {
        if (!existingJob.hasSameRequest(submission)) {
            throw MaintenanceJobException.conflict(
                    "Maintenance job '%s' already exists with different content".formatted(submission.jobId()));
        }
        log.info("Maintenance job '{}' already exists with same content. Treating request as idempotent.",
                submission.jobId());
    }
}
