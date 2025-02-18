package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;

import java.util.Set;
import java.util.UUID;

public interface ProcessInstanceRepository extends ProcessInstanceQueryRepository {

    ProcessInstance save(ProcessInstance processInstance);

    void deleteAllById(Set<UUID> ids);

    void setHashForTemplateIfNull(ProcessTemplate template);

    /**
     * Ensure that all process instance properties managed by the repository layer are up-to-date.
     */
    void flush();
}
