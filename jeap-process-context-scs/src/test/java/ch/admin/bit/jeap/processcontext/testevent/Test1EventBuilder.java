package ch.admin.bit.jeap.processcontext.testevent;

import ch.admin.bit.jeap.processcontext.event.test1.Test1Event;
import ch.admin.bit.jeap.processcontext.event.test1.Test1EventPayload;
import ch.admin.bit.jeap.processcontext.event.test1.Test1EventReferences;
import com.fasterxml.uuid.Generators;

import java.util.List;

public class Test1EventBuilder extends AbstractTestEventBuilder<Test1EventBuilder, Test1Event> {

    private List<String> taskIds = List.of();
    private String someField;

    private Test1EventBuilder(String originProcessId) {
        super(Test1Event::new, Test1EventReferences::new, "Test1Event", originProcessId);
    }

    public static Test1EventBuilder createForProcessId(String originProcessId) {
        return new Test1EventBuilder(originProcessId);
    }

    public Test1EventBuilder taskIds(String... taskIds) {
        this.taskIds = List.of(taskIds);
        return this;
    }

    public Test1EventBuilder someField(String someField) {
        this.someField = someField;
        return this;
    }

    @Override
    public Test1Event build() {
        idempotenceId(Generators.timeBasedEpochGenerator().generate().toString());
        Test1Event event = super.build();
        Test1EventPayload payload = new Test1EventPayload();
        payload.setTaskIds(taskIds);
        payload.setSomeField(someField);
        event.setPayload(payload);
        return event;
    }
}
