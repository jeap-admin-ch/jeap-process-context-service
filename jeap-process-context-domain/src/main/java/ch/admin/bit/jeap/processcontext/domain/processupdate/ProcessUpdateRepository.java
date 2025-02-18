package ch.admin.bit.jeap.processcontext.domain.processupdate;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;

public interface ProcessUpdateRepository extends ProcessUpdateQueryRepository {

    ProcessUpdate save(ProcessUpdate processUpdate);

    Slice<UUID> findProcessUpdateIdWithHandledFalse(ZonedDateTime createdBefore, Pageable pageable);

    void markHandled(UUID processUpdateId);

    void markHandlingFailed(UUID processUpdateId);

    int countAllByOriginProcessIdIn(Set<String> originProcessIds);

    void deleteAllByOriginProcessIdIn(Set<String> originProcessIds);

    void deleteAllById(Set<UUID> ids);

}
