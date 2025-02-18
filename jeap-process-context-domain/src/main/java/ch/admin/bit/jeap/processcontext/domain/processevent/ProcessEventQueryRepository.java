package ch.admin.bit.jeap.processcontext.domain.processevent;

import java.util.List;

public interface ProcessEventQueryRepository {

    List<ProcessEvent> findByOriginProcessId(String originProcessId);
}
