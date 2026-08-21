package ch.admin.bit.jeap.processcontext.domain.processinstance.relation;

import ch.admin.bit.jeap.processcontext.domain.processtemplate.RelationPattern;

import java.util.List;
import java.util.UUID;

public interface RelationCandidateRepository {
    List<RelationCandidate> findCandidates(UUID processInstanceId, RelationPattern pattern,
                                           RelationCandidateCursor after, int limit);
}
