package ch.admin.bit.jeap.processcontext.testevent;

import ch.admin.bit.jeap.processcontext.event.test6.ProcessReference;
import ch.admin.bit.jeap.processcontext.event.test6.Test6Event;
import ch.admin.bit.jeap.processcontext.event.test6.Test6EventPayload;
import ch.admin.bit.jeap.processcontext.event.test6.Test6EventReferences;
import com.fasterxml.uuid.Generators;

import java.util.List;

public class Test6EventBuilder extends AbstractTestEventBuilder<Test6EventBuilder, Test6Event> {

    private List<String> taskIds = List.of();
    private String objectId;

    private ProcessReference processReference;

    private Test6EventBuilder(String originProcessId) {
        super(Test6Event::new, Test6EventReferences::new, "Test6Event", originProcessId);
    }

    public static Test6EventBuilder createForProcessId(String originProcessId) {
        return new Test6EventBuilder(originProcessId);
    }

    public Test6EventBuilder taskIds(String... taskIds) {
        this.taskIds = List.of(taskIds);
        return this;
    }

    public Test6EventBuilder processReference(String processReference) {
        this.processReference = new ProcessReference("processReference", processReference);
        return this;
    }

    @Override
    public Test6Event build() {
        idempotenceId(Generators.timeBasedEpochGenerator().generate().toString());
        Test6Event event = super.build();
        Test6EventPayload payload = new Test6EventPayload();
        payload.setTaskIds(taskIds);
        payload.setObjectId(objectId);
        event.setPayload(payload);
        Test6EventReferences references = new Test6EventReferences();
        references.setProcessReference(processReference);
        event.setReferences(references);
        return event;
    }

    public Test6EventBuilder objectId(String objectId) {
        this.objectId = objectId;
        return this;
    }
}
