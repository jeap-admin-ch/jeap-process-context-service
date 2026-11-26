package ch.admin.bit.jeap.processcontext.plugin.api.relation;

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Set;

/**
 * Default implementation of {@link RelationListener}, will simply log new relations and the declared relation types
 */
@Slf4j
public class LoggingRelationListener implements RelationListener {

    @Override
    public void relationsAdded(Collection<Relation> relations) {
        if (!relations.isEmpty()) {
            log.info("Relations added to process {}: {}", relations.iterator().next().getOriginProcessId(), relations);
        }
    }

    @Override
    public void relationTypesDeclared(Set<RelationType> relationTypes) {
        log.info("Relation types declared in the process templates: {}", relationTypes);
    }
}
