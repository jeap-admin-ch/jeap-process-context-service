package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProcessRelationJpaRepository extends JpaRepository<ProcessRelation, UUID> {

    List<ProcessRelation> findAllByRelatedProcessId(String processId);

}
