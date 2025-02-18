package ch.admin.bit.jeap.processcontext.testevent;

import ch.admin.bit.jeap.processcontext.event.test4.Test4Event;
import ch.admin.bit.jeap.processcontext.plugin.api.event.MessageCorrelationProvider;

import java.util.Set;

public class TaskTest4EventCorrelationProvider implements MessageCorrelationProvider<Test4Event> {

    @Override
    public Set<String> getRelatedOriginTaskIds(Test4Event event) {
        return Set.copyOf(event.getPayload().getTaskIds());
    }
}
