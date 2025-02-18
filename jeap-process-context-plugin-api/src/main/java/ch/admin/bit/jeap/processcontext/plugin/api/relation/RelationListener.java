package ch.admin.bit.jeap.processcontext.plugin.api.relation;

import java.util.Collection;

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
}
