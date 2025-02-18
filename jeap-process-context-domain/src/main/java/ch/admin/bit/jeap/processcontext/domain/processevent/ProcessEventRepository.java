package ch.admin.bit.jeap.processcontext.domain.processevent;

import java.util.Set;

public interface ProcessEventRepository extends ProcessEventQueryRepository {

    void saveAll(Iterable<ProcessEvent> processEvent);

    long deleteAllByOriginProcessIdIn(Set<String> originProcessIds);
}
