package ch.admin.bit.jeap.processcontext.testevent;

import ch.admin.bit.jeap.processcontext.event.processids.ProcessIdsEvent;
import ch.admin.bit.jeap.processcontext.plugin.api.event.MessageCorrelationProvider;

import java.util.Set;

public class ProcessIdsEventCorrelationProvider implements MessageCorrelationProvider<ProcessIdsEvent> {

    @Override
    public Set<String> getOriginProcessIds(ProcessIdsEvent event) {
        return Set.copyOf(event.getPayload().getProcessIds());
    }

    @Override
    public Set<String> getRelatedOriginTaskIds(ProcessIdsEvent event) {
        return Set.of();
    }
}
