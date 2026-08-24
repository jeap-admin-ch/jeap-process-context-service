package ch.admin.bit.jeap.processcontext.domain.processinstance;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public record ProcessDataValue(String key, String value, String role) {

    private static final Comparator<ProcessDataValue> CANONICAL_ORDER = Comparator
            .comparing(ProcessDataValue::key)
            .thenComparing(ProcessDataValue::value)
            .thenComparing(ProcessDataValue::role, Comparator.nullsFirst(Comparator.naturalOrder()));

    public static List<ProcessDataValue> canonicalize(Collection<ProcessDataValue> values) {
        return values.stream().sorted(CANONICAL_ORDER).toList();
    }
}
