package ch.admin.bit.jeap.processcontext.repository.template.json.stubs;

import ch.admin.bit.jeap.processcontext.plugin.api.condition.ProcessSnapshotCondition;
import ch.admin.bit.jeap.processcontext.plugin.api.condition.ProcessSnapshotConditionResult;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext;


public class TestProcessSnapshotCondition implements ProcessSnapshotCondition {

    @Override
    public ProcessSnapshotConditionResult triggerSnapshot(ProcessContext processContext) {
        return ProcessSnapshotConditionResult.NOT_TRIGGERED;
    }

}
