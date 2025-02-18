package ch.admin.bit.jeap.processcontext.testevent;

import ch.admin.bit.jeap.processcontext.event.test4.Test4Event;
import ch.admin.bit.jeap.processcontext.event.test4.Test4EventPayload;
import ch.admin.bit.jeap.processcontext.event.test4.Test4EventReferences;
import com.fasterxml.uuid.Generators;

import java.util.List;

public class Test4EventBuilder extends AbstractTestEventBuilder<Test4EventBuilder, Test4Event> {

    private List<String> taskIds = List.of();
    private String objectId;

    private Test4EventBuilder(String originProcessId) {
        super(Test4Event::new, Test4EventReferences::new, "Test4Event", originProcessId);
    }

    public static Test4EventBuilder createForProcessId(String originProcessId) {
        return new Test4EventBuilder(originProcessId);
    }

    public Test4EventBuilder taskIds(String... taskIds) {
        this.taskIds = List.of(taskIds);
        return this;
    }

    @Override
    public Test4Event build() {
        idempotenceId(Generators.timeBasedEpochGenerator().generate().toString());
        Test4Event event = super.build();
        Test4EventPayload payload = new Test4EventPayload();
        payload.setTaskIds(taskIds);
        payload.setObjectId(objectId);
        event.setPayload(payload);
        return event;
    }

    public Test4EventBuilder objectId(String objectId) {
        this.objectId = objectId;
        return this;
    }
}

