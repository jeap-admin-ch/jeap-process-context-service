package ch.admin.bit.jeap.processcontext.testevent;

import ch.admin.bit.jeap.processcontext.event.test3.Test3Event;
import ch.admin.bit.jeap.processcontext.event.test3.Test3EventPayload;
import ch.admin.bit.jeap.processcontext.event.test3.Test3EventReferences;
import com.fasterxml.uuid.Generators;

import java.util.List;

public class Test3EventBuilder extends AbstractTestEventBuilder<Test3EventBuilder, Test3Event> {

    private List<String> taskIds = List.of();

    private Test3EventBuilder(String originProcessId) {
        super(Test3Event::new, Test3EventReferences::new, "Test3Event", originProcessId);
    }

    public static Test3EventBuilder createForProcessId(String originProcessId) {
        return new Test3EventBuilder(originProcessId);
    }

    public Test3EventBuilder taskIds(String... taskIds) {
        this.taskIds = List.of(taskIds);
        return this;
    }

    @Override
    public Test3Event build() {
        idempotenceId(Generators.timeBasedEpochGenerator().generate().toString());
        Test3Event event = super.build();
        Test3EventPayload payload = new Test3EventPayload();
        payload.setTaskIds(taskIds);
        event.setPayload(payload);
        return event;
    }
}
