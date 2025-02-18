package ch.admin.bit.jeap.processcontext.domain.processtemplate;

import java.util.List;

import static ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskCardinality.MULTI_INSTANCE;
import static ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskCardinality.SINGLE_INSTANCE;

/**
 * A task can be
 * <ul>
 *     <li>static: planned at design time, non-optional</li>
 *     <li>dynamic: planned at run-time, optional</li>
 *     <li>observed: not planned, just observed, optional</li>
 * </ul>
 */
public enum TaskLifecycle {

    STATIC(SINGLE_INSTANCE),
    DYNAMIC(MULTI_INSTANCE, SINGLE_INSTANCE),
    OBSERVED(MULTI_INSTANCE);

    private final List<TaskCardinality> supportedCardinalites;

    /**
     * The first supported cardinality listed is used as default cardinality for this lifecycle.
     *
     * @param supportedCardinalities The list of cardinalities supported by this lifecycle.
     */
    TaskLifecycle(TaskCardinality... supportedCardinalities) {
        this.supportedCardinalites = List.of(supportedCardinalities);
    }

    public boolean supportsCardinality(TaskCardinality cardinality) {
        return supportedCardinalites.contains(cardinality);
    }

    public TaskCardinality getDefaultCardinality() {
        return supportedCardinalites.get(0);
    }

}
