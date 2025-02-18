package ch.admin.bit.jeap.processcontext.testevent;

import ch.admin.bit.jeap.processcontext.event.test4.Test4CreatingProcessInstanceEvent;
import ch.admin.bit.jeap.processcontext.event.test4.Test4CreatingProcessInstanceEventPayload;
import ch.admin.bit.jeap.processcontext.event.test4.Test4CreatingProcessInstanceEventReferences;
import com.fasterxml.uuid.Generators;

import java.util.List;

public class Test4CreatingProcessInstanceEventBuilder extends AbstractTestEventBuilder<Test4CreatingProcessInstanceEventBuilder, Test4CreatingProcessInstanceEvent> {

    private List<String> taskIds = List.of();
    private String objectId;

    private Test4CreatingProcessInstanceEventBuilder(String originProcessId) {
        super(Test4CreatingProcessInstanceEvent::new, Test4CreatingProcessInstanceEventReferences::new, "Test4CreatingProcessInstanceEvent", originProcessId);
    }

    public static Test4CreatingProcessInstanceEventBuilder createForProcessId(String originProcessId) {
        return new Test4CreatingProcessInstanceEventBuilder(originProcessId);
    }

    public Test4CreatingProcessInstanceEventBuilder taskIds(String... taskIds) {
        this.taskIds = List.of(taskIds);
        return this;
    }

    @Override
    public Test4CreatingProcessInstanceEvent build() {
        idempotenceId(Generators.timeBasedEpochGenerator().generate().toString());
        Test4CreatingProcessInstanceEvent event = super.build();
        Test4CreatingProcessInstanceEventPayload payload = new Test4CreatingProcessInstanceEventPayload();
        payload.setTaskIds(taskIds);
        payload.setObjectId(objectId);
        event.setPayload(payload);
        return event;
    }

    public Test4CreatingProcessInstanceEventBuilder objectId(String objectId) {
        this.objectId = objectId;
        return this;
    }
}

