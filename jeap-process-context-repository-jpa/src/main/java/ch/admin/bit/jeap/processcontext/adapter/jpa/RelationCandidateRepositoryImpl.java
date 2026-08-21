package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.relation.RelationCandidate;
import ch.admin.bit.jeap.processcontext.domain.processinstance.relation.RelationCandidateCursor;
import ch.admin.bit.jeap.processcontext.domain.processinstance.relation.RelationCandidateRepository;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.RelationPattern;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class RelationCandidateRepositoryImpl implements RelationCandidateRepository {
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public List<RelationCandidate> findCandidates(UUID processInstanceId, RelationPattern pattern,
                                                  RelationCandidateCursor after, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT o.id, o.value_, s.id, s.value_
                  FROM process_instance_process_data o
                  JOIN process_instance_process_data s
                    ON s.process_instance_id = o.process_instance_id
                   AND s.key_ = :subjectKey
                 WHERE o.process_instance_id = :processInstanceId
                   AND o.key_ = :objectKey
                """);
        appendSelectorRoles(sql, pattern);
        appendJoin(sql, pattern);
        if (after != null) {
            sql.append("""
                     AND (o.id > :afterObjectId
                          OR (o.id = :afterObjectId AND s.id > :afterSubjectId))
                    """);
        }
        sql.append(" ORDER BY o.id, s.id");

        Query query = entityManager.createNativeQuery(sql.toString())
                .setParameter("processInstanceId", processInstanceId)
                .setParameter("objectKey", pattern.getObjectSelector().getProcessDataKey())
                .setParameter("subjectKey", pattern.getSubjectSelector().getProcessDataKey())
                .setMaxResults(limit);
        setSelectorRoleParameters(query, pattern);
        if (after != null) {
            query.setParameter("afterObjectId", after.objectProcessDataId());
            query.setParameter("afterSubjectId", after.subjectProcessDataId());
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(row -> new RelationCandidate(
                        uuid(row[0]), (String) row[1], uuid(row[2]), (String) row[3]))
                .toList();
    }

    private static UUID uuid(Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof byte[] bytes) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            return new UUID(buffer.getLong(), buffer.getLong());
        }
        return UUID.fromString(value.toString());
    }

    private static void appendSelectorRoles(StringBuilder sql, RelationPattern pattern) {
        if (pattern.getObjectSelector().getProcessDataRole() != null) {
            sql.append(" AND o.role = :objectRole");
        }
        if (pattern.getSubjectSelector().getProcessDataRole() != null) {
            sql.append(" AND s.role = :subjectRole");
        }
    }

    private static void appendJoin(StringBuilder sql, RelationPattern pattern) {
        if (pattern.getJoinType() == RelationPattern.JoinType.BY_VALUE) {
            sql.append(" AND s.value_ = o.value_");
        } else if (pattern.getJoinType() == RelationPattern.JoinType.BY_ROLE) {
            sql.append(" AND o.role IS NOT NULL AND s.role = o.role");
        }
    }

    private static void setSelectorRoleParameters(Query query, RelationPattern pattern) {
        if (pattern.getObjectSelector().getProcessDataRole() != null) {
            query.setParameter("objectRole", pattern.getObjectSelector().getProcessDataRole());
        }
        if (pattern.getSubjectSelector().getProcessDataRole() != null) {
            query.setParameter("subjectRole", pattern.getSubjectSelector().getProcessDataRole());
        }
    }
}
