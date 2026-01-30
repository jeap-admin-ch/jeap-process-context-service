package ch.admin.bit.jeap.processcontext.repository.template.json.stubs;

import ch.admin.bit.jeap.processcontext.domain.processinstance.snapshot.ProcessSnapshotCondition;
import ch.admin.bit.jeap.processcontext.domain.processinstance.snapshot.ProcessSnapshotConditionResult;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext;


public class TestProcessSnapshotCondition implements ProcessSnapshotCondition {

    @Override
    public ProcessSnapshotConditionResult triggerSnapshot(ProcessContext processContext) {
        return ProcessSnapshotConditionResult.NOT_TRIGGERED;
    }

}
