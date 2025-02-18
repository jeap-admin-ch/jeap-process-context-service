package ch.admin.bit.jeap.processcontext.domain.processtemplate;

/**
 * A task can have cardinality
 * <ul>
 *     <li>single instance: there can be ony one instance of such a task</li>
 *     <li>multi instance: there can be multiple instances of such a task</li>
 * </ul>
 */
public enum TaskCardinality {
    SINGLE_INSTANCE,
    MULTI_INSTANCE
}
