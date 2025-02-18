package ch.admin.bit.jeap.processcontext.adapter.restapi.model;

import ch.admin.bit.jeap.processcontext.domain.processinstance.Relation;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@AllArgsConstructor
@Builder(access = AccessLevel.PRIVATE)
public class RelationDTO {

    String subjectType;
    String subjectId;
    String objectType;
    String objectId;
    String predicateType;
    ZonedDateTime createdAt;

    static RelationDTO create(Relation relation) {
        return RelationDTO.builder()
                .subjectType(relation.getSubjectType())
                .subjectId(relation.getSubjectId())
                .objectType(relation.getObjectType())
                .objectId(relation.getObjectId())
                .predicateType(relation.getPredicateType())
                .createdAt(relation.getCreatedAt())
                .build();
    }
}
