package ch.admin.bit.jeap.processcontext.domain.processtemplate;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
public class RelationNodeSelector {
    String type;
    String processDataKey;
    String processDataRole;

    @Builder
    private RelationNodeSelector(@NonNull String type, @NonNull String processDataKey, String processDataRole) {
        this.type = type;
        this.processDataKey = processDataKey;
        this.processDataRole = processDataRole;
    }
}
