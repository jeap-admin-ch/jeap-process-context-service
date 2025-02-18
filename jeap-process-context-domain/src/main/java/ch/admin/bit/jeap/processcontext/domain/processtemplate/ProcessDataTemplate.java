package ch.admin.bit.jeap.processcontext.domain.processtemplate;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Slf4j
public class ProcessDataTemplate {
    @EqualsAndHashCode.Include
    String key;
    String sourceMessageName;
    String sourceMessageDataKey;

    @Builder
    private ProcessDataTemplate(@NonNull String key, @NonNull String sourceMessageName, @NonNull String sourceMessageDataKey) {
        this.key = key;
        this.sourceMessageName = sourceMessageName;
        this.sourceMessageDataKey = sourceMessageDataKey;
    }
}
