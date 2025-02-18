package ch.admin.bit.jeap.processcontext.domain.processtemplate;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.Set;

@Value
public class TaskData {

    String sourceMessage;
    Set<String> messageDataKeys;

    @Builder
    private TaskData(@NonNull String sourceMessage, @NonNull Set<String> messageDataKeys) {
        this.sourceMessage = sourceMessage;
        this.messageDataKeys = Set.copyOf(messageDataKeys);
    }

}
