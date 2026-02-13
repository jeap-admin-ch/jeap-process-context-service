package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.Relation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
interface RelationJpaRepository extends JpaRepository<Relation, UUID>, JpaSpecificationExecutor<Relation> {

    Page<Relation> findByProcessInstanceId(UUID processInstanceId, Pageable pageable);
}
