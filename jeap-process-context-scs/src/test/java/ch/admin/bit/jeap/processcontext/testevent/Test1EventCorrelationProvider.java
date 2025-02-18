package ch.admin.bit.jeap.processcontext.testevent;

import ch.admin.bit.jeap.processcontext.event.test1.Test1Event;
import ch.admin.bit.jeap.processcontext.plugin.api.event.MessageCorrelationProvider;

import java.util.Set;

public class Test1EventCorrelationProvider implements MessageCorrelationProvider<Test1Event> {

    @Override
    public Set<String> getRelatedOriginTaskIds(Test1Event event) {
        return Set.copyOf(event.getPayload().getTaskIds());
    }
}
