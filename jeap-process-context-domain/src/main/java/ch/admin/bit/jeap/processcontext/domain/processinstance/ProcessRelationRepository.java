package ch.admin.bit.jeap.processcontext.domain.processinstance;

import java.util.List;

public interface ProcessRelationRepository {
    List<ProcessRelation> findByRelatedProcessId(String processId);
}
