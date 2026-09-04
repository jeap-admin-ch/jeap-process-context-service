package ch.admin.bit.jeap.processcontext.adapter.restapi.model;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RelationDTOTest {

    @Test
    void createWithoutPersistedId() {
        ZonedDateTime createdAt = ZonedDateTime.now();

        RelationDTO relation = new RelationDTO(
                "subject-type",
                "subject-id",
                "object-type",
                "object-id",
                "predicate-type",
                createdAt);

        assertThat(relation.getId()).isNull();
        assertThat(relation.getSubjectType()).isEqualTo("subject-type");
        assertThat(relation.getSubjectId()).isEqualTo("subject-id");
        assertThat(relation.getObjectType()).isEqualTo("object-type");
        assertThat(relation.getObjectId()).isEqualTo("object-id");
        assertThat(relation.getPredicateType()).isEqualTo("predicate-type");
        assertThat(relation.getCreatedAt()).isEqualTo(createdAt);
    }
}
