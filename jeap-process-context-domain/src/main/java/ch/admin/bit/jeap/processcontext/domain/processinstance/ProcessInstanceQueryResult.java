package ch.admin.bit.jeap.processcontext.domain.processinstance;

import java.util.UUID;

public interface ProcessInstanceQueryResult {
    UUID getId();
    String getOriginProcessId();
}
