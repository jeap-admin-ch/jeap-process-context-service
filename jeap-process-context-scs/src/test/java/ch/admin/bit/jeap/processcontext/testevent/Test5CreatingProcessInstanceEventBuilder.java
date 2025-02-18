package ch.admin.bit.jeap.processcontext.testevent;

import ch.admin.bit.jeap.processcontext.event.test5.Test5CreatingProcessInstanceEvent;
import ch.admin.bit.jeap.processcontext.event.test5.Test5CreatingProcessInstanceEventPayload;
import ch.admin.bit.jeap.processcontext.event.test5.Test5CreatingProcessInstanceEventReferences;
import com.fasterxml.uuid.Generators;

public class Test5CreatingProcessInstanceEventBuilder extends AbstractTestEventBuilder<Test5CreatingProcessInstanceEventBuilder, Test5CreatingProcessInstanceEvent> {

    private String trigger;

    private Test5CreatingProcessInstanceEventBuilder(String originProcessId) {
        super(Test5CreatingProcessInstanceEvent::new, Test5CreatingProcessInstanceEventReferences::new, "Test5CreatingProcessInstanceEvent", originProcessId);
    }

    public static Test5CreatingProcessInstanceEventBuilder createForProcessId(String originProcessId) {
        return new Test5CreatingProcessInstanceEventBuilder(originProcessId);
    }

    public Test5CreatingProcessInstanceEventBuilder trigger(String trigger) {
        this.trigger = trigger;
        return this;
    }

    @Override
    public Test5CreatingProcessInstanceEvent build() {
        idempotenceId(Generators.timeBasedEpochGenerator().generate().toString());
        Test5CreatingProcessInstanceEvent event = super.build();
        Test5CreatingProcessInstanceEventPayload payload = new Test5CreatingProcessInstanceEventPayload();
        payload.setTrigger(trigger);
        event.setPayload(payload);
        return event;
    }

}

