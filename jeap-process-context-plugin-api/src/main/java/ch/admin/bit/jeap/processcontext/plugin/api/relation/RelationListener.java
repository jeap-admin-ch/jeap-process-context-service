package ch.admin.bit.jeap.processcontext.plugin.api.relation;

import java.util.Collection;
import java.util.Set;

/**
 * Can be implemented by process context service instances to be notified when new relations have been discovered
 * in a process instance.
 */
public interface RelationListener {

    /**
     * Invoked when new relations have been added to a process
     *
     * @param relations New relations discovered between business entities (subject -- predicate -- object),
     *                 as defined by relation patterns in the process template.
     */
    void relationsAdded(Collection<Relation> relations);

    /**
     * Invoked once at startup, after the process templates have been loaded and before any message is consumed.
     * Reports the distinct set of relation types declared by the relation patterns of all process templates,
     * regardless of the state of their feature flags. If the method throws an exception, the startup of the process
     * context service instance is aborted. The default implementation does nothing.
     *
     * @param relationTypes Distinct relation types declared in the process templates (may be empty)
     */
    default void relationTypesDeclared(Set<RelationType> relationTypes) {
        // no-op by default
    }
}
