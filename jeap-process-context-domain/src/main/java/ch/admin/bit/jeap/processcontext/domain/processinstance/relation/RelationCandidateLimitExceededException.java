package ch.admin.bit.jeap.processcontext.domain.processinstance.relation;

import java.util.UUID;

public class RelationCandidateLimitExceededException extends RuntimeException {
    public RelationCandidateLimitExceededException(UUID processInstanceId, long maxCandidates) {
        super("Relation reevaluation for process instance '%s' exceeds the limit of %d candidate pairs"
                .formatted(processInstanceId, maxCandidates));
    }
}
