package ch.admin.bit.jeap.processcontext.adapter.jpa;

import java.util.UUID;

interface RelationOwnerProjection {
    UUID getRelationId();

    String getOriginProcessId();
}
