package ch.admin.bit.jeap.processcontext.plugin.api.condition;

import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext;

/**
 * Decide if a snapshot should be created or not given a certain process context.
 * Return <code>ProcessSnapshotConditionResult.NOT_TRIGGERED</code> to indicate that a snapshot should not be created.
 * Use <code>ProcessSnapshotConditionResult.triggeredFor(String snapshotName)</code> to indicate that a snapshot should
 * be triggered and that the snapshot should be named <code>snapshotName</code>. The snaphshot name usually corresponds
 * to the snapshot condition's name. The process context service will only create one snapshot per triggered snapshot name,
 * i.e. a process snapshot condition triggering again and again for the same snapshot name will still only result in one
 * snaphshot (created at the moment of the first triggering), meaning a snapshot condition is allowed to stay fulfilled
 * without creating additional snapshots. The snapshot name must not be null and must not contain a space character.
 */
public interface ProcessSnapshotCondition {

    ProcessSnapshotConditionResult triggerSnapshot(ProcessContext processContext);

}
