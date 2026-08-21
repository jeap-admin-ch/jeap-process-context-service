package ch.admin.bit.jeap.processcontext.domain.processinstance.relation;

import java.util.UUID;

public record RelationCandidate(
        UUID objectProcessDataId,
        String objectValue,
        UUID subjectProcessDataId,
        String subjectValue) {

    RelationCandidateCursor cursor() {
        return new RelationCandidateCursor(objectProcessDataId, subjectProcessDataId);
    }
}
