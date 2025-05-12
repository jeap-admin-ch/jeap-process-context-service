package ch.admin.bit.jeap.processcontext.testevent;

import ch.admin.bit.jeap.processcontext.event.test1.Test1CreatingProcessInstanceEvent;
import ch.admin.bit.jeap.processcontext.plugin.api.message.MessageFilter;

public class Test1CreatingProcessInstanceEventMessageFilter implements MessageFilter<Test1CreatingProcessInstanceEvent> {

    @Override
    public boolean filter(Test1CreatingProcessInstanceEvent message) {
        return !message.getPayload().getTaskIds().contains("taskIdToFilter");
    }
}
