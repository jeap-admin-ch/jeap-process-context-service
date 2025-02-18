package ch.admin.bit.jeap.processcontext.domain.processinstance;

import lombok.AllArgsConstructor;

import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;
import static lombok.AccessLevel.PRIVATE;

/**
 * A wrapper around a collection of {@link ProcessData} objects, allowing for fast indexed access.
 */
@AllArgsConstructor(access = PRIVATE)
class ProcessDataWrapper {
    private final Map<String, Set<ProcessData>> processDataByKey;

    static ProcessDataWrapper of(Set<ProcessData> processData) {
        Map<String, Set<ProcessData>> processDataByKey = processData.stream()
                .collect(groupingBy(ProcessData::getKey, toSet()));
        return new ProcessDataWrapper(processDataByKey);
    }

    /**
     * @param processDataKey  Process data key
     * @param processDataRole Optional: Process data role to match, or null if the match should only be made by key
     * @return Collection of process data with matching key and role (if given)
     */
    public Set<ProcessData> findByKeyAndOptionalRole(String processDataKey, String processDataRole) {
        Set<ProcessData> matchingByKey = processDataByKey.getOrDefault(processDataKey, Set.of());
        if (processDataRole == null) {
            return matchingByKey;
        }

        return matchingByKey.stream()
                .filter(pd -> processDataRole.equals(pd.getRole()))
                .collect(toSet());
    }
}
