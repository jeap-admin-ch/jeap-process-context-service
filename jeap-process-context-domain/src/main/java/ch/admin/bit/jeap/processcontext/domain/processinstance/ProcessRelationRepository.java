package ch.admin.bit.jeap.processcontext.domain.processinstance;

import java.util.List;
import java.util.UUID;

public interface ProcessRelationRepository {
    List<ProcessRelation> findAllByRelatedProcessId(String processId);

    List<ProcessRelation> findAllByProcessInstanceId(UUID processInstanceId);

    boolean exists(UUID processInstanceId, ProcessRelation processRelation);

    List<ProcessRelation> saveAll(List<ProcessRelation> processRelations);
}
