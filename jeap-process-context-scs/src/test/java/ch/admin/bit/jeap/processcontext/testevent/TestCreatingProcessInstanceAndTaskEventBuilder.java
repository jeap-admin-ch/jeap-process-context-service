package ch.admin.bit.jeap.processcontext.testevent;

import ch.admin.bit.jeap.processcontext.event.test1.TestCreatingProcessInstanceAndTaskEvent;
import ch.admin.bit.jeap.processcontext.event.test1.TestCreatingProcessInstanceAndTaskEventPayload;
import ch.admin.bit.jeap.processcontext.event.test1.TestCreatingProcessInstanceAndTaskEventReferences;
import com.fasterxml.uuid.Generators;

import java.util.List;

public class TestCreatingProcessInstanceAndTaskEventBuilder extends AbstractTestEventBuilder<TestCreatingProcessInstanceAndTaskEventBuilder, TestCreatingProcessInstanceAndTaskEvent> {

    private List<String> taskIds = List.of();

    private TestCreatingProcessInstanceAndTaskEventBuilder(String originProcessId) {
        super(TestCreatingProcessInstanceAndTaskEvent::new, TestCreatingProcessInstanceAndTaskEventReferences::new, "TestCreatingProcessInstanceAndTaskEvent", originProcessId);
    }

    public static TestCreatingProcessInstanceAndTaskEventBuilder createForProcessId(String originProcessId) {
        return new TestCreatingProcessInstanceAndTaskEventBuilder(originProcessId);
    }

    public TestCreatingProcessInstanceAndTaskEventBuilder taskIds(String... taskIds) {
        this.taskIds = List.of(taskIds);
        return this;
    }

    @Override
    public TestCreatingProcessInstanceAndTaskEvent build() {
        idempotenceId(Generators.timeBasedEpochGenerator().generate().toString());
        TestCreatingProcessInstanceAndTaskEvent event = super.build();
        TestCreatingProcessInstanceAndTaskEventPayload payload = new TestCreatingProcessInstanceAndTaskEventPayload();
        payload.setTaskIds(taskIds);
        event.setPayload(payload);
        return event;
    }
}
