package ch.admin.bit.jeap.processcontext.domain.processinstance.relation;

import java.util.UUID;

public record RelationCandidateCursor(UUID objectProcessDataId, UUID subjectProcessDataId) {
}
