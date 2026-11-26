package ch.admin.bit.jeap.processcontext.plugin.api.relation;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * A type of relation (subject type -- predicate type -- object type) declared by a relation pattern in a process
 * template, together with the system ID the relations are attributed to. Relation types are reported independently of
 * the state of their feature flag, i.e. relation types whose feature flag is currently inactive are reported as well.
 * <p>
 * Two relation types are equal only if all their attributes are equal, including the feature flag: the same
 * subject -- predicate -- object combination is reported once per feature flag it is declared with (and once without,
 * if a pattern declares it without feature flag).
 */
@Value
@Builder
public class RelationType {
    /**
     * System ID of the template declaring the relation pattern ({@code relationSystemId}). Every {@link Relation}
     * created from the pattern carries this value as {@link Relation#getSystemId()}.
     */
    @NonNull
    String systemId;
    @NonNull
    String predicateType;
    @NonNull
    String subjectType;
    @NonNull
    String objectType;
    /**
     * Name of the feature flag the relation pattern is bound to, <code>null</code> if none
     */
    String featureFlag;
}
