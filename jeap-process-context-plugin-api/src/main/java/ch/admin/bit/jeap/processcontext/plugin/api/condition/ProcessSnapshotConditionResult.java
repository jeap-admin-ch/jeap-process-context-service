package ch.admin.bit.jeap.processcontext.plugin.api.condition;


import io.micrometer.common.util.StringUtils;
import lombok.*;


@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ProcessSnapshotConditionResult {

    private final boolean snapShotTriggered;
    private final String snapshotName;

    /**
     * A result that indicates no snapshot has been triggered.
     */
    public static final ProcessSnapshotConditionResult NOT_TRIGGERED =
            new ProcessSnapshotConditionResult(false, null);

    /**
     * Create a result that indicates a snapshot with the given name has been triggered.
     * The snapshot name must not be null and must not contain a space character.
     * @param snapshotName The snapshot name
     * @return The snapshot condition result
     */
    public static ProcessSnapshotConditionResult triggeredFor(String snapshotName) {
        if (StringUtils.isEmpty(snapshotName)) {
            throw new IllegalArgumentException("snapshotName must not be empty.");
        }
        if (snapshotName.contains(" ")) {
            throw new IllegalArgumentException("snapshotName must not contain a space character.");
        }
        return new ProcessSnapshotConditionResult(true, snapshotName);
    }

}
