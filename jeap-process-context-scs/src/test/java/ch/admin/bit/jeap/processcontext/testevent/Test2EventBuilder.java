package ch.admin.bit.jeap.processcontext.testevent;

import ch.admin.bit.jeap.processcontext.event.test2.Test2Event;
import ch.admin.bit.jeap.processcontext.event.test2.Test2EventPayload;
import ch.admin.bit.jeap.processcontext.event.test2.Test2EventReferences;
import com.fasterxml.uuid.Generators;

import java.util.List;

public class Test2EventBuilder extends AbstractTestEventBuilder<Test2EventBuilder, Test2Event> {

    private List<String> taskIds = List.of();
    private String objectId;
    private String version;

    private Test2EventBuilder(String originProcessId) {
        super(Test2Event::new, Test2EventReferences::new, "Test2Event", originProcessId);
    }

    public static Test2EventBuilder createForProcessId(String originProcessId) {
        return new Test2EventBuilder(originProcessId);
    }

    public Test2EventBuilder taskIds(String... taskIds) {
        this.taskIds = List.of(taskIds);
        return this;
    }

    public Test2EventBuilder version(String version) {
        this.version = version;
        return this;
    }

    @Override
    public Test2Event build() {
        idempotenceId(Generators.timeBasedEpochGenerator().generate().toString());
        Test2Event event = super.build();
        Test2EventPayload payload = new Test2EventPayload();
        payload.setTaskIds(taskIds);
        payload.setObjectId(objectId);
        payload.setVersion(version);
        event.setPayload(payload);
        return event;
    }

    public Test2EventBuilder objectId(String objectId) {
        this.objectId = objectId;
        return this;
    }
}
