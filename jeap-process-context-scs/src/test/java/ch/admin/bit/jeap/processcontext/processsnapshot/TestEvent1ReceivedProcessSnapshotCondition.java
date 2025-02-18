package ch.admin.bit.jeap.processcontext.processsnapshot;

import ch.admin.bit.jeap.processcontext.plugin.api.condition.ProcessSnapshotCondition;
import ch.admin.bit.jeap.processcontext.plugin.api.condition.ProcessSnapshotConditionResult;
import ch.admin.bit.jeap.processcontext.plugin.api.context.Message;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext;

@SuppressWarnings("unused")
public class TestEvent1ReceivedProcessSnapshotCondition implements ProcessSnapshotCondition {
    @Override
    public ProcessSnapshotConditionResult triggerSnapshot(ProcessContext processContext) {
        if (processContext.getMessages().stream().
                map(Message::getName).
                anyMatch("Test1Event"::equals)) {
            return ProcessSnapshotConditionResult.triggeredFor("TestEvent1ReceivedCondition");
        } else {
            return ProcessSnapshotConditionResult.NOT_TRIGGERED;
        }
    }
}
