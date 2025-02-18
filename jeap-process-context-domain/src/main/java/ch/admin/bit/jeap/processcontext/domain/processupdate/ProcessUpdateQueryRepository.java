package ch.admin.bit.jeap.processcontext.domain.processupdate;

import java.util.List;
import java.util.Optional;

public interface ProcessUpdateQueryRepository {

    List<ProcessUpdate> findByOriginProcessIdAndHandledFalse(String originProcessId);

    Optional<ProcessUpdate> findByOriginProcessIdAndMessageNameAndIdempotenceId(String originProcessId, String messageName, String idempotenceId);

}
