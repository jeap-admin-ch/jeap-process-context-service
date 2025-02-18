package ch.admin.bit.jeap.processcontext.domain.processtemplate;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
public class ProcessRelationSource {
    String messageName;
    String messageDataKey;

    @Builder
    private ProcessRelationSource(@NonNull String messageName, @NonNull String messageDataKey) {
        this.messageName = messageName;
        this.messageDataKey = messageDataKey;
    }
}
