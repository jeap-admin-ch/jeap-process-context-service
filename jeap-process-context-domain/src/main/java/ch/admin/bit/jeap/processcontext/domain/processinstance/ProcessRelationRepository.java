package ch.admin.bit.jeap.processcontext.domain.processinstance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ProcessRelationRepository {
    List<ProcessRelation> findAllByRelatedProcessId(String processId);

    List<ProcessRelation> findAllByProcessInstanceId(UUID processInstanceId);

    /**
     * Finds process relations that are visible for the given process instance, either because they are owned by the
     * process instance (excluding TARGET visibility) or because they are from other processes pointing to this one
     * (excluding ORIGIN visibility).
     */
    Page<ProcessRelation> findAllVisibleForProcess(ProcessInstance processInstance, Pageable pageable);

    boolean exists(UUID processInstanceId, ProcessRelation processRelation);

    List<ProcessRelation> saveAll(List<ProcessRelation> processRelations);
}
