package ch.admin.bit.jeap.processcontext.plugin.api.relation;

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;

/**
 * Default implementation of {@link RelationListener}, will simply log new relations
 */
@Slf4j
public class LoggingRelationListener implements RelationListener {

    @Override
    public void relationsAdded(Collection<Relation> relations) {
        log.info("Relations added to process {}: {}", relations.iterator().next().getOriginProcessId(), relations);
    }
}
