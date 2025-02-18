package ch.admin.bit.jeap.processcontext.plugin.api.condition;

import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessCompletionConclusion;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext;
import lombok.Getter;

import static ch.admin.bit.jeap.processcontext.plugin.api.condition.ProcessSnapshotConditionResult.NOT_TRIGGERED;
import static ch.admin.bit.jeap.processcontext.plugin.api.condition.ProcessSnapshotConditionResult.triggeredFor;

@Getter
public class ProcessCompletionProcessSnapshotCondition implements ProcessSnapshotCondition {

    private static final String SNAPSHOT_CONDITION_NAME = "JeapProcessCompletionSnapshotCondition";

    public ProcessCompletionProcessSnapshotCondition() {
        this.triggeringConclusion = null; // trigger on any conclusion
    }

    public ProcessCompletionProcessSnapshotCondition(ProcessCompletionConclusion triggeringConclusion) {
        this.triggeringConclusion = triggeringConclusion; // trigger only on given conclusion
    }

    private final ProcessCompletionConclusion triggeringConclusion;

    @Override
    public ProcessSnapshotConditionResult triggerSnapshot(ProcessContext processContext) {
        if (processContext.getProcessCompletion() == null) {
            // process not yet completed
            return NOT_TRIGGERED;
        }

        // trigger on any completion conclusion?
        if (triggeringConclusion == null) {
            return triggeredFor(SNAPSHOT_CONDITION_NAME);
        }

        // trigger on given completion conclusion
        if (processContext.getProcessCompletion().getConclusion() == triggeringConclusion) {
            return triggeredFor(SNAPSHOT_CONDITION_NAME + ":" + triggeringConclusion.name());
        } else {
            return NOT_TRIGGERED;
        }
    }

}
