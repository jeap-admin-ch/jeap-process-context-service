package ch.admin.bit.jeap.processcontext.testevent;

import ch.admin.bit.jeap.processcontext.event.test3.Test3Event;
import ch.admin.bit.jeap.processcontext.plugin.api.event.MessageCorrelationProvider;

import java.util.Set;

public class Test3EventCorrelationProvider implements MessageCorrelationProvider<Test3Event> {

    @Override
    public Set<String> getRelatedOriginTaskIds(Test3Event event) {
        return Set.copyOf(event.getPayload().getTaskIds());
    }
}
