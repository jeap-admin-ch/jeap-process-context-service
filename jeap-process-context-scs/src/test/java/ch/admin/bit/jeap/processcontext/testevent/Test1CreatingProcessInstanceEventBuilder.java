package ch.admin.bit.jeap.processcontext.testevent;

import ch.admin.bit.jeap.processcontext.event.test1.Test1CreatingProcessInstanceEvent;
import ch.admin.bit.jeap.processcontext.event.test1.Test1CreatingProcessInstanceEventPayload;
import ch.admin.bit.jeap.processcontext.event.test1.Test1CreatingProcessInstanceEventReferences;
import com.fasterxml.uuid.Generators;

import java.util.List;

public class Test1CreatingProcessInstanceEventBuilder extends AbstractTestEventBuilder<Test1CreatingProcessInstanceEventBuilder, Test1CreatingProcessInstanceEvent> {

    private List<String> taskIds = List.of();

    private Test1CreatingProcessInstanceEventBuilder(String originProcessId) {
        super(Test1CreatingProcessInstanceEvent::new, Test1CreatingProcessInstanceEventReferences::new, "Test1CreatingProcessInstanceEvent", originProcessId);
    }

    public static Test1CreatingProcessInstanceEventBuilder createForProcessId(String originProcessId) {
        return new Test1CreatingProcessInstanceEventBuilder(originProcessId);
    }

    public Test1CreatingProcessInstanceEventBuilder taskIds(String... taskIds) {
        this.taskIds = List.of(taskIds);
        return this;
    }

    @Override
    public Test1CreatingProcessInstanceEvent build() {
        idempotenceId(Generators.timeBasedEpochGenerator().generate().toString());
        Test1CreatingProcessInstanceEvent event = super.build();
        Test1CreatingProcessInstanceEventPayload payload = new Test1CreatingProcessInstanceEventPayload();
        payload.setTaskIds(taskIds);
        event.setPayload(payload);
        return event;
    }
}
