package ch.admin.bit.jeap.processcontext.domain.processevent;

import ch.admin.bit.jeap.processcontext.plugin.api.relation.Relation;
import lombok.experimental.UtilityClass;

@UtilityClass
class RelationMapper {
    Relation toApiObject(String originProcessId,
                         ch.admin.bit.jeap.processcontext.domain.processinstance.Relation domainRelation) {
        return Relation.builder()
                .systemId(domainRelation.getSystemId())
                .originProcessId(originProcessId)
                .objectType(domainRelation.getObjectType())
                .objectId(domainRelation.getObjectId())
                .subjectType(domainRelation.getSubjectType())
                .subjectId(domainRelation.getSubjectId())
                .predicateType(domainRelation.getPredicateType())
                .idempotenceId(domainRelation.getIdempotenceId())
                .createdAt(domainRelation.getCreatedAt())
                .build();
    }
}
