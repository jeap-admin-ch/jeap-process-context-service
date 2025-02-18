package ch.admin.bit.jeap.processcontext.domain.processupdate;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum ProcessUpdateType {
    DOMAIN_EVENT(1), PROCESS_CREATED(0), CREATE_PROCESS(0);

    @Getter
    int priority;

}
