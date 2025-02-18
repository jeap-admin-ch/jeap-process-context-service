package ch.admin.bit.jeap.processcontext.testevent;

import ch.admin.bit.jeap.processcontext.event.processids.ProcessIdsEvent;
import ch.admin.bit.jeap.processcontext.event.processids.ProcessIdsEventPayload;
import ch.admin.bit.jeap.processcontext.event.processids.ProcessIdsEventReferences;

import java.util.List;

public class ProcessIdsEventBuilder extends AbstractTestEventBuilder<ProcessIdsEventBuilder, ProcessIdsEvent> {

    private List<String> processIds = List.of();

    private ProcessIdsEventBuilder() {
        super(ProcessIdsEvent::new, ProcessIdsEventReferences::new, "ProcessIdsEvent", null);
    }

    public static ProcessIdsEventBuilder create() {
        return new ProcessIdsEventBuilder();
    }

    public ProcessIdsEventBuilder processIds(String... processIds) {
        this.processIds = List.of(processIds);
        return this;
    }

    @Override
    public ProcessIdsEvent build() {
        ProcessIdsEvent event = super.build();
        ProcessIdsEventPayload payload = new ProcessIdsEventPayload();
        payload.setProcessIds(processIds);
        event.setPayload(payload);
        return event;
    }
}
