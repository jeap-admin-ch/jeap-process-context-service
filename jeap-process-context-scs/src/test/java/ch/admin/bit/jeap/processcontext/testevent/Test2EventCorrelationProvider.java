package ch.admin.bit.jeap.processcontext.testevent;

import ch.admin.bit.jeap.processcontext.event.test2.Test2Event;
import ch.admin.bit.jeap.processcontext.plugin.api.event.MessageCorrelationProvider;

import java.util.Set;

public class Test2EventCorrelationProvider implements MessageCorrelationProvider<Test2Event> {

    @Override
    public Set<String> getRelatedOriginTaskIds(Test2Event event) {
        return Set.copyOf(event.getPayload().getTaskIds());
    }
}
